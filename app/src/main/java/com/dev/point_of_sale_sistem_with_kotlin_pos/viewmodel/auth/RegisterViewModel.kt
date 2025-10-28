package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.RegisterState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent

class RegisterViewModel(
    private val supabase: SupabaseClient,
    private val authSessionViewModel: AuthSessionViewModel,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repository = AuthRepository(supabase)
    private val _intents = Channel<AuthIntent>(Channel.UNLIMITED)
    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    init {
        val savedStep = savedStateHandle.get<Int>("currentStep") ?: 1
        _state.value = _state.value.copy(currentStep = savedStep)
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
        Log.d("RegisterViewModel", "Attempting to register user: ${intent.userEmail}")

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
            Log.d("RegisterViewModel", "Registration successful")
            _state.value = _state.value.copy(
                isLoading = false,
                successMessage = "REGISTER_SUCCESS"
            )
            authSessionViewModel.sendIntent(AuthIntent.CheckSession)
        }.onFailure { e ->
            Log.e("RegisterViewModel", "Registration failed: ${e.message}")

            val authError = when {
                e.message?.contains("Username") == true -> AuthError.UsernameTaken(intent.userName)
                e.message?.contains("User already registered") == true ||
                        e.message?.contains("user email") == true -> AuthError.EmailTaken(intent.userEmail)
                e.message?.contains("Business email") == true -> AuthError.CompanyEmailTaken(intent.businessEmail)
                e.message?.contains("Company name") == true -> AuthError.CompanyNameTaken(intent.businessName)
                e.message?.contains("Branch name") == true -> AuthError.BranchNameTaken("${intent.businessName} - Principal")
                else -> AuthError.Other("REGISTER_FAILED")
            }

            _state.value = _state.value.copy(
                isLoading = false,
                error = authError
            )
        }
    }

    fun goToPreviousStep() {
        val newStep = (_state.value.currentStep - 1).coerceAtLeast(1)
        _state.value = _state.value.copy(currentStep = newStep)
        savedStateHandle["currentStep"] = newStep
    }

    fun goToNextStep() {
        val newStep = _state.value.currentStep + 1
        _state.value = _state.value.copy(currentStep = newStep)
        savedStateHandle["currentStep"] = newStep
    }

    fun onUserNameChange(value: String) = _state.update { it.copy(userName = value, error = null) }
    fun onUserEmailChange(value: String) = _state.update { it.copy(userEmail = value, error = null) }
    fun onUserPasswordChange(value: String) = _state.update { it.copy(userPassword = value, error = null) }
    fun onUserConfirmPasswordChange(value: String) = _state.update { it.copy(userConfirmPassword = value, error = null) }
    fun onBusinessNameChange(value: String) = _state.update { it.copy(businessName = value, error = null) }
    fun onBusinessEmailChange(value: String) = _state.update { it.copy(businessEmail = value, error = null) }
    fun onBusinessAddressChange(value: String) = _state.update { it.copy(businessAddress = value, error = null) }
    fun onBusinessPhoneChange(value: String) = _state.update { it.copy(businessPhone = value, error = null) }

    fun toggleShowPassword() = _state.update { it.copy(showPassword = !it.showPassword) }
    fun toggleShowConfirmPassword() = _state.update { it.copy(showConfirmPassword = !it.showConfirmPassword) }
    fun clearError() = _state.update { it.copy(error = null) }
}
