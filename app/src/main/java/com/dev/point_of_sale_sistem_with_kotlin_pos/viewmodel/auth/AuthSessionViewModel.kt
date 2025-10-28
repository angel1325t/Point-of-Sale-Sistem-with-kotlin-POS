package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthSessionState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AuthSessionViewModel(private val supabase: SupabaseClient) : ViewModel() {

    private val repository = AuthRepository(supabase)

    private val _state = MutableStateFlow(AuthSessionState())
    val state: StateFlow<AuthSessionState> = _state.asStateFlow()

    private val _intents = Channel<AuthIntent>(Channel.UNLIMITED)

    companion object {
        private const val TAG = "AuthSessionViewModel"
    }

    init {
        Log.d(TAG, "Initializing AuthSessionViewModel")
        observeIntents()
        sendIntent(AuthIntent.CheckSession)
    }

    private fun observeIntents() {
        viewModelScope.launch {
            _intents.receiveAsFlow().collect { intent ->
                when (intent) {
                    is AuthIntent.CheckSession -> handleCheckSession()
                    is AuthIntent.Logout -> handleLogout()
                    else -> {}
                }
            }
        }
    }

    fun sendIntent(intent: AuthIntent) {
        viewModelScope.launch { _intents.send(intent) }
    }

    private fun handleCheckSession() {
        viewModelScope.launch {
            Log.d(TAG, "Checking session")
            _state.value = _state.value.copy(isLoading = true)

            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    userId = user.id,
                    email = user.email,
                    error = null,
                    successMessage = null
                )
                Log.d(TAG, "Session check: Authenticated userId=${user.id}, email=${user.email}")
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    userId = null,
                    email = null,
                    error = null,
                    successMessage = null
                )
                Log.d(TAG, "Session check: Not authenticated")
            }
        }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            Log.d(TAG, "Logging out")
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.logout()
            result.onSuccess {
                // ✅ Usar identificador en lugar de string
                _state.value = _state.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    userId = null,
                    email = null,
                    successMessage = "LOGOUT_SUCCESS",
                    error = null
                )
                Log.d(TAG, "Logout successful")
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "LOGOUT_FAILED")
                )
                Log.e(TAG, "Logout failed: ${e.message}")
            }
        }
    }
}
