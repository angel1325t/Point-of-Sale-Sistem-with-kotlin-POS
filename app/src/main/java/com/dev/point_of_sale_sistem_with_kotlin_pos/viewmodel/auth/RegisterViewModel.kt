package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.RegisterState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RegisterViewModel(private val supabase: SupabaseClient,private val authSessionViewModel: AuthSessionViewModel) : ViewModel() {

    private val repository = AuthRepository(supabase)

    private val _intents = Channel<AuthIntent>(Channel.UNLIMITED)
    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

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
                    is AuthIntent.Register -> handleRegister(intent)
                    else -> {}
                }
            }
        }
    }

    private suspend fun handleRegister(intent: AuthIntent.Register) {
        _state.value = _state.value.copy(isLoading = true, error = null)

        val result = repository.register(
            userEmail = intent.userEmail,
            userPassword = intent.userPassword,
            userName = intent.userName,
            businessName = intent.businessName,
            businessEmail = intent.businessEmail,
            businessPhone = intent.businessPhone,
            businessAddress = intent.businessAddress
        )

        result.onSuccess {
            _state.value = _state.value.copy(
                isLoading = false,
                successMessage = "Registro exitoso. Revisa tu correo 📧"
            )
            authSessionViewModel.checkSession()
        }.onFailure { e ->
            _state.value = _state.value.copy(
                isLoading = false,
                error = AuthError.Other(e.message ?: "Error al registrar")
            )
        }
    }

    // ─── On Change ───
    fun onUserNameChange(value: String) { _state.value = _state.value.copy(userName = value) }
    fun onUserEmailChange(value: String) { _state.value = _state.value.copy(userEmail = value) }
    fun onUserPasswordChange(value: String) { _state.value = _state.value.copy(userPassword = value) }
    fun onUserConfirmPasswordChange(value: String) { _state.value = _state.value.copy(userConfirmPassword = value) }
    fun onBusinessNameChange(value: String) { _state.value = _state.value.copy(businessName = value) }
    fun onBusinessEmailChange(value: String) { _state.value = _state.value.copy(businessEmail = value) }
    fun onBusinessAddressChange(value: String) { _state.value = _state.value.copy(businessAddress = value) }
    fun onBusinessPhoneChange(value: String) { _state.value = _state.value.copy(businessPhone = value) }

    fun goToPreviousStep() {
        _state.value = _state.value.copy(
            currentStep = (_state.value.currentStep - 1).coerceAtLeast(1)
        )
    }

    fun goToNextStep() {
        _state.value = _state.value.copy(currentStep = _state.value.currentStep + 1)
    }

    fun toggleShowPassword() {
        _state.value = _state.value.copy(showPassword = !_state.value.showPassword)
    }

    fun toggleShowConfirmPassword() {
        _state.value = _state.value.copy(showConfirmPassword = !_state.value.showConfirmPassword)
    }
}
