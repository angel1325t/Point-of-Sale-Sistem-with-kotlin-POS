package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(private val supabase: SupabaseClient) : ViewModel() {

    private val repository = AuthRepository(supabase)

    // 🔹 CANAL DE INTENTS (MVI)
    private val _intents = Channel<AuthIntent>(Channel.UNLIMITED)

    // 🔹 STATE FLOW (MVI)
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        // 🔹 ESCUCHAR INTENTS AL INICIAR
        observeIntents()
        // 🔹 VERIFICAR SESIÓN AUTOMÁTICO
        sendIntent(AuthIntent.CheckSession)
    }

    // 🔹 RECIBIR INTENTS DESDE UI
    fun sendIntent(intent: AuthIntent) {
        viewModelScope.launch {
            _intents.send(intent)
        }
    }

    // 🔹 PROCESAR INTENTS → STATE (FLUXO UNIDIRECCIONAL MVI)
    private fun observeIntents() {
        viewModelScope.launch {
            _intents.receiveAsFlow().collect { intent ->
                when (intent) {
                    is AuthIntent.Login -> handleLogin(intent)
                    is AuthIntent.Logout -> handleLogout()
                    is AuthIntent.CheckSession -> handleCheckSession()
                }
            }
        }
    }

    private suspend fun handleLogin(intent: AuthIntent.Login) {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        val result = repository.login(intent.email, intent.password)

        result.onSuccess { userId ->
            _state.value = _state.value.copy(
                isLoading = false,
                isAuthenticated = true,
                userId = userId,
                email = intent.email,
                successMessage = "¡Bienvenido! 👋"
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = e.message ?: "Email o contraseña incorrectos"
            )
        }
    }

    private suspend fun handleLogout() {
        _state.value = _state.value.copy(isLoading = true)
        val result = repository.logout()

        result.onSuccess {
            _state.value = AuthState(successMessage = "Sesión cerrada correctamente")
        }.onFailure { e ->
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = e.message ?: "Error al cerrar sesión"
            )
        }
    }

    private suspend fun handleCheckSession() {
        val result = repository.getCurrentUser()
        result.onSuccess { userId ->
            if (userId != null) {
                _state.value = _state.value.copy(
                    isAuthenticated = true,
                    userId = userId,
                    email = supabase.auth.currentUserOrNull()?.email
                )
            }
        }
    }
}