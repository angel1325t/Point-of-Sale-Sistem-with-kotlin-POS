package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val supabase: SupabaseClient) : ViewModel() {

    private val repository = AuthRepository(supabase)

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    // 🔹 Registrar usuario (CORREGIDO)
    fun register(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.register(email, password, username)
            result.onSuccess { userId ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    userId = userId,
                    email = email,
                    successMessage = "Registro exitoso"
                )
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error en el registro"
                )
            }
        }
    }

    // 🔹 Iniciar sesión (CORREGIDO)
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.login(email, password)
            result.onSuccess { userId ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    userId = userId,
                    email = email,
                    successMessage = "Inicio de sesión exitoso"
                )
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error al iniciar sesión"
                )
            }
        }
    }

    // 🔹 Cerrar sesión
    fun logout() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            val result = repository.logout()
            result.onSuccess {
                _authState.value = AuthState(successMessage = "Sesión cerrada")
            }.onFailure { e ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error al cerrar sesión"
                )
            }
        }
    }

    // 🔹 Verificar sesión actual
    fun checkSession() {
        viewModelScope.launch {
            val result = repository.getCurrentUser()
            result.onSuccess { userId ->
                if (userId != null) {
                    _authState.value = _authState.value.copy(
                        isAuthenticated = true,
                        userId = userId
                    )
                }
            }
        }
    }
}