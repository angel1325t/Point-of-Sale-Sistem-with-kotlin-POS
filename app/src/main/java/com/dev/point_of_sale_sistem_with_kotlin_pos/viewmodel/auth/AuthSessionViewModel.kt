package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthSessionState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthSessionViewModel(private val supabase: SupabaseClient) : ViewModel() {

    private val repository = AuthRepository(supabase)

    private val _state = MutableStateFlow(AuthSessionState())
    val state: StateFlow<AuthSessionState> = _state.asStateFlow()

    companion object {
        private const val TAG = "AuthSessionViewModel"
    }

    init {
        Log.d(TAG, "Initializing AuthSessionViewModel")
        checkSession()
    }

    fun checkSession() {
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
                    error = null
                )
                Log.d(TAG, "Session check: Authenticated userId=${user.id}, email=${user.email}")
            } else {
                _state.value = AuthSessionState(
                    successMessage = if (_state.value.isAuthenticated) "Sesión cerrada correctamente" else null
                )
                Log.d(TAG, "Session check: Not authenticated")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "Logging out")
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.logout()

            result.onSuccess {
                _state.value = AuthSessionState(
                    successMessage = "Sesión cerrada correctamente"
                )
                Log.d(TAG, "Logout successful")
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "Error al cerrar sesión")
                )
                Log.e(TAG, "Logout failed: ${e.message}")
            }
        }
    }
}
