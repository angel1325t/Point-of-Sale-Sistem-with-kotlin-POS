package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.LoginState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LoginViewModel(private val supabase: SupabaseClient,private val authSessionViewModel: AuthSessionViewModel) : ViewModel() {

    private val repository = AuthRepository(supabase)

    private val _intents = Channel<AuthIntent>(Channel.UNLIMITED)
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    init {
        observeIntents()
    }

    fun sendIntent(intent: AuthIntent) {
        viewModelScope.launch { _intents.send(intent) }
    }

    private fun observeIntents() {
        viewModelScope.launch {
            _intents.receiveAsFlow().collect { intent ->
                when (intent) {
                    is AuthIntent.Login -> handleLogin(intent)
                    else -> {}
                }
            }
        }
    }

    private suspend fun handleLogin(intent: AuthIntent.Login) {
        _state.value = _state.value.copy(isLoading = true, error = null)

        val result = repository.login(intent.email, intent.password)

        result.onSuccess {
            _state.value = _state.value.copy(
                isLoading = false,
                successMessage = "Inicio de sesión exitoso ✅"
            )
            authSessionViewModel.checkSession() // Actualiza el estado de la sesión
        }.onFailure { e ->
            _state.value = _state.value.copy(
                isLoading = false,
                error = if (e.message?.contains("invalid") == true)
                    AuthError.InvalidCredentials
                else
                    AuthError.Other(e.message ?: "Error desconocido")
            )
        }
    }

    // ─── On Change ───
    fun onEmailChange(value: String) {
        _state.value = _state.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value)
    }
}
