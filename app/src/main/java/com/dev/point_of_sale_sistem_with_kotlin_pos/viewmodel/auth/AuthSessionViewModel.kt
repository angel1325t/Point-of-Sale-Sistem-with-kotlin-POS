package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.SessionState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AuthSessionViewModel(
    private val supabase: SupabaseClient,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    private val repository = AuthRepository(supabase)

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

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
                    is AuthIntent.UserDisabled -> handleUserDisabled(intent)
                    is AuthIntent.ChangeBranch -> handleChangeBranch(intent.branchId)
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
            _state.value = _state.value.copy(isLoading = true)

            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                // ✅ Consultar en la DB si el usuario sigue deshabilitado
                val isDisabledInDB = repository.isUserDisabled(UUID.fromString(user.id))

                // Actualizar SharedPreferences para mantenerlo sincronizado
                sessionPreferences.setUserDisabled(isDisabledInDB)

                if (!isDisabledInDB) {
                    val savedBranchId = sessionPreferences.getBranchId()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userId = user.id,
                        email = user.email,
                        branchId = savedBranchId,
                        isUserDisabled = false,
                        error = null,
                        successMessage = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        isUserDisabled = true,
                        error = AuthError.Other("USER_DISABLED")
                    )
                }
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    isUserDisabled = false
                )
            }
        }
    }

    private fun handleUserDisabled(intent: AuthIntent.UserDisabled) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = false,
                isUserDisabled = true,
                isAuthenticated = false
            )
            sessionPreferences.setUserDisabled(true)
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(
            isLoading = false,
            isAuthenticated = false,
            isUserDisabled = false,
            error = null,
            successMessage = null
        )
    }

    private fun handleChangeBranch(branchId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Changing branch to: $branchId")

            _state.value = _state.value.copy(isLoading = true)

            try {
                kotlinx.coroutines.delay(1500)

                sessionPreferences.saveBranchId(branchId)
                Log.d(TAG, "BranchId saved in preferences")

                _state.value = _state.value.copy(
                    isLoading = false,
                    branchId = branchId,
                    successMessage = "BRANCH_CHANGED",
                    error = null
                )

                Log.d(TAG, "Branch changed successfully to: $branchId")

                kotlinx.coroutines.delay(2000)
                _state.value = _state.value.copy(successMessage = null)

            } catch (e: Exception) {
                Log.e(TAG, "Error changing branch: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other("Error al cambiar de sucursal: ${e.message}")
                )
            }
        }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            Log.d(TAG, "Logging out")
            _state.value = _state.value.copy(isLoading = true)

            val result = repository.logout()
            result.onSuccess {
                try {
                    sessionPreferences.clearBranchId()
                    Log.d(TAG, "BranchId cleared from preferences")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing branchId: ${e.message}")
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    userId = null,
                    email = null,
                    branchId = null,
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