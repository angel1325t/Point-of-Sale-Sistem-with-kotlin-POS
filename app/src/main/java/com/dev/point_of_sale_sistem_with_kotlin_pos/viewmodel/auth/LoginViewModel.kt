package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.LoginState
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.SessionState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class LoginViewModel(
    private val supabase: SupabaseClient,
    private val authSessionViewModel: AuthSessionViewModel
) : ViewModel() {

    private val repository = AuthRepository(supabase)

    private val _intents = Channel<AuthIntent>(Channel.UNLIMITED)
    private val _state = MutableStateFlow(LoginState())
    private val _sessionState = MutableStateFlow(SessionState())
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

        result.onSuccess { userIdString ->

            val userId = UUID.fromString(userIdString)

            // Verificar si está desactivado
            val isDisabled = repository.isUserDisabled(userId)
            Log.d("loginvm","$isDisabled")

            if (isDisabled) {
                _state.value = _state.value.copy(isLoading = false)
                authSessionViewModel.handleIntent(AuthIntent.UserDisabled())
                return@onSuccess
            }

            _state.value = _state.value.copy(
                isLoading = false,
                successMessage = "SIGN_IN_SUCCESS"
            )
            authSessionViewModel.handleIntent(AuthIntent.CheckSession)

        }.onFailure { e ->
            _state.value = _state.value.copy(
                isLoading = false,
                error = if (e.message?.contains("invalid") == true)
                    AuthError.InvalidCredentials
                else
                    AuthError.Other(e.message ?: "SIGN_IN_FAILED")
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