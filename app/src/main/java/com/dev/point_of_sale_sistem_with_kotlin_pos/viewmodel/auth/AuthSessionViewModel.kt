package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

import android.util.Log
import android.content.Context
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
    private val sessionPreferences: SessionPreferences,
    private val context: Context
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
is AuthIntent.ResetPassword -> handleResetPassword(intent)
is AuthIntent.ResetPasswordWithToken -> handleResetPasswordWithToken(intent)
is AuthIntent.VerifyPasswordResetCode -> handleVerifyPasswordResetCode(intent)
is AuthIntent.RequestEmailChange -> handleRequestEmailChange(intent)
is AuthIntent.VerifyEmailChange -> handleVerifyEmailChange(intent)
is AuthIntent.ResendOtp -> handleResendOtp(intent)
                    is AuthIntent.UploadProfilePhoto -> handleUploadProfilePhoto(context, intent)
                    is AuthIntent.UpdateProfilePhotoUrl -> handleUpdateProfilePhotoUrl(intent)
                    is AuthIntent.ClearPhotoUploadState -> handleClearPhotoUploadState()
                    is AuthIntent.ClearMessages -> handleClearMessages()
                    else -> {}
                }
            }
        }
    }

private fun handleResetPassword(intent: AuthIntent.ResetPassword) {
        viewModelScope.launch {
            Log.d(TAG, "Handling password reset for email: ${intent.email}")
            _state.value = _state.value.copy(isLoading = true)

            val result = repository.resetPassword(intent.email)
            result.onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "PASSWORD_RESET_EMAIL_SENT",
                    error = null
                )
                Log.d(TAG, "Password reset email sent successfully")
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "PASSWORD_RESET_FAILED")
                )
                Log.e(TAG, "Password reset failed: ${e.message}")
            }
        }
    }

    private fun handleResetPasswordWithToken(intent: AuthIntent.ResetPasswordWithToken) {
        viewModelScope.launch {
            Log.d(TAG, "Handling password reset with token")
            _state.value = _state.value.copy(isLoading = true)

            val result = repository.resetPasswordWithToken(intent.token, intent.newPassword)
            result.onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "PASSWORD_RESET_SUCCESS",
                    error = null
                )
                Log.d(TAG, "Password reset with token successful")
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "PASSWORD_RESET_WITH_TOKEN_FAILED")
                )
                Log.e(TAG, "Password reset with token failed: ${e.message}")
            }
        }
    }

    private fun handleValidatePasswordResetToken(intent: AuthIntent.ValidatePasswordResetToken) {
        viewModelScope.launch {
            Log.d(TAG, "Handling password reset token validation")
            _state.value = _state.value.copy(isLoading = true)

            try {
                // Simply try to retrieve user with the token to validate it
                supabase.auth.retrieveUser(intent.token)
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "PASSWORD_RESET_TOKEN_VALID",
                    error = null
                )
                Log.d(TAG, "Password reset token is valid")
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other("Invalid or expired reset token: ${e.message}")
                )
                Log.e(TAG, "Password reset token validation failed: ${e.message}")
            }
        }
    }

private fun handleVerifyPasswordResetCode(intent: AuthIntent.VerifyPasswordResetCode) {
        viewModelScope.launch {
            Log.d(TAG, "Handling password reset code verification")
            _state.value = _state.value.copy(isLoading = true)

            val result = repository.verifyPasswordResetCode(intent.email, intent.code)
            result.onSuccess { sessionToken ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "PASSWORD_RESET_CODE_VALID",
                    error = null
                )
                Log.d(TAG, "Password reset code verification successful")
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "PASSWORD_RESET_CODE_INVALID")
                )
                Log.e(TAG, "Password reset code verification failed: ${e.message}")
            }
        }
    }

    private fun handleRequestEmailChange(intent: AuthIntent.RequestEmailChange) {
        viewModelScope.launch {
            Log.d(TAG, "Handling email change request to: ${intent.newEmail}")
            _state.value = _state.value.copy(isLoading = true)

            val result = repository.requestEmailChange(intent.newEmail, intent.password)
            result.onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "EMAIL_CHANGE_OTP_SENT",
                    error = null
                )
                Log.d(TAG, "Email change request sent successfully")
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "EMAIL_CHANGE_REQUEST_FAILED")
                )
                Log.e(TAG, "Email change request failed: ${e.message}")
            }
        }
    }

    private fun handleVerifyEmailChange(intent: AuthIntent.VerifyEmailChange) {
        viewModelScope.launch {
            Log.d(TAG, "Handling email change verification")
            _state.value = _state.value.copy(isLoading = true)

            val result = repository.verifyEmailChange(intent.code)
            result.onSuccess {
                // Update the email in the state after successful verification
                repository.getCurrentUser().onSuccess { newEmail ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        email = newEmail,
                        successMessage = "EMAIL_CHANGE_SUCCESS",
                        error = null
                    )
                    Log.d(TAG, "Email change verification successful. New email: $newEmail")
                }.onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = AuthError.Other(e.message ?: "EMAIL_CHANGE_VERIFICATION_FAILED")
                    )
                    Log.e(TAG, "Failed to get updated email: ${e.message}")
                }
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "EMAIL_CHANGE_VERIFICATION_FAILED")
                    )
                Log.e(TAG, "Email change verification failed: ${e.message}")
            }
        }
    }

    private fun handleResendOtp(intent: AuthIntent.ResendOtp) {
        viewModelScope.launch {
            Log.d(TAG, "Handling resend OTP for email: ${intent.email}")
            _state.value = _state.value.copy(isLoading = true)

            val result = repository.resetPassword(intent.email)
            result.onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "PASSWORD_RESET_EMAIL_SENT",
                    codeResendCooldown = System.currentTimeMillis(),
                    error = null
                )
                Log.d(TAG, "Password reset resend OTP sent successfully")
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "OTP_RESEND_FAILED")
                )
                Log.e(TAG, "Failed to resend OTP: ${e.message}")
            }
        }
    }
        }
    }

    private fun handleRequestEmailChange(intent: AuthIntent.RequestEmailChange) {
        viewModelScope.launch {
            Log.d(TAG, "Handling email change request to: ${intent.newEmail}")
            _state.value = _state.value.copy(isLoading = true)

            val result = repository.requestEmailChange(intent.newEmail, intent.password)
            result.onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "EMAIL_CHANGE_REQUEST_SENT",
                    error = null
                )
                Log.d(TAG, "Email change request sent successfully")
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "EMAIL_CHANGE_REQUEST_FAILED")
                )
                Log.e(TAG, "Email change request failed: ${e.message}")
            }
        }
    }

    private fun handleVerifyEmailChange(intent: AuthIntent.VerifyEmailChange) {
        viewModelScope.launch {
            Log.d(TAG, "Handling email change verification")
            _state.value = _state.value.copy(isLoading = true)

            val result = repository.verifyEmailChange(intent.token)
            result.onSuccess {
                // Update the email in the state after successful verification
                val currentUserResult = repository.getCurrentUser()
                currentUserResult.onSuccess { newEmail ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        email = newEmail,
                        successMessage = "EMAIL_CHANGE_SUCCESS",
                        error = null
                    )
                    Log.d(TAG, "Email change verified successfully. New email: $newEmail")
                }.onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        successMessage = "EMAIL_CHANGE_SUCCESS",
                        error = null
                    )
                    Log.w(TAG, "Email changed but failed to get new email: ${e.message}")
                }
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other(e.message ?: "EMAIL_CHANGE_VERIFICATION_FAILED")
                )
                Log.e(TAG, "Email change verification failed: ${e.message}")
            }
        }
    }

    fun sendIntent(intent: AuthIntent) {
        viewModelScope.launch { _intents.send(intent) }
    }

    // ============================================
    //        MÉTODOS DE BIOMÉTRICO
    // ============================================

    /**
     * Guarda si el dispositivo tiene datos biométricos configurados
     */
    fun setHasBiometric(hasBiometric: Boolean) {
        viewModelScope.launch {
            sessionPreferences.setHasBiometric(hasBiometric)
            Log.d(TAG, "Biometric availability saved: $hasBiometric")
        }
    }

    /**
     * Obtiene si el dispositivo tiene datos biométricos configurados
     */
    suspend fun getHasBiometric(): Boolean {
        return sessionPreferences.getHasBiometric()
    }

    // ============================================
    //        MÉTODOS EXISTENTES
    // ============================================

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
                    
                    // Fetch user profile photo URL
                    val profilePhotoResult = repository.getUserProfilePhotoUrl(user.id)
                    val profilePhotoUrl = profilePhotoResult.getOrNull()
                    
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userId = user.id,
                        email = user.email,
                        branchId = savedBranchId,
                        profilePhotoUrl = profilePhotoUrl,
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
                    profilePhotoUrl = null,
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

    // ============================================
    //        PROFILE PHOTO METHODS
    // ============================================

    private fun handleUploadProfilePhoto(context: Context, intent: AuthIntent.UploadProfilePhoto) {
        viewModelScope.launch {
            Log.d(TAG, "Handling profile photo upload")
            val userId = _state.value.userId
            
            if (userId == null) {
                _state.value = _state.value.copy(
                    isUploadingPhoto = false,
                    error = AuthError.Other("User not authenticated")
                )
                return@launch
            }

            _state.value = _state.value.copy(isUploadingPhoto = true)

            val uploadResult = repository.uploadProfilePhoto(context, intent.imageUri, userId)
            uploadResult.onSuccess { photoUrl ->
                // Update profile photo URL in database
                val updateResult = repository.updateProfilePhotoUrl(userId, photoUrl)
                updateResult.onSuccess {
                    _state.value = _state.value.copy(
                        isUploadingPhoto = false,
                        profilePhotoUrl = photoUrl,
                        successMessage = "PROFILE_PHOTO_UPDATED",
                        error = null
                    )
                    Log.d(TAG, "Profile photo updated successfully")
                }.onFailure { e ->
                    _state.value = _state.value.copy(
                        isUploadingPhoto = false,
                        error = AuthError.Other("Failed to update profile photo URL: ${e.message}")
                    )
                    Log.e(TAG, "Failed to update profile photo URL: ${e.message}")
                }
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isUploadingPhoto = false,
                    error = AuthError.Other("Failed to upload profile photo: ${e.message}")
                )
                Log.e(TAG, "Profile photo upload failed: ${e.message}")
            }
        }
    }

    private fun handleUpdateProfilePhotoUrl(intent: AuthIntent.UpdateProfilePhotoUrl) {
        viewModelScope.launch {
            Log.d(TAG, "Handling profile photo URL update")
            val userId = _state.value.userId
            
            if (userId == null) {
                _state.value = _state.value.copy(
                    error = AuthError.Other("User not authenticated")
                )
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true)

            val result = repository.updateProfilePhotoUrl(userId, intent.photoUrl)
            result.onSuccess {
                _state.value = _state.value.copy(
                    isLoading = false,
                    profilePhotoUrl = intent.photoUrl,
                    successMessage = "PROFILE_PHOTO_UPDATED",
                    error = null
                )
                Log.d(TAG, "Profile photo URL updated successfully")
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AuthError.Other("Failed to update profile photo: ${e.message}")
                )
                Log.e(TAG, "Profile photo URL update failed: ${e.message}")
            }
        }
    }

    private fun handleClearPhotoUploadState() {
        _state.value = _state.value.copy(
            isUploadingPhoto = false,
            error = null,
            successMessage = null
        )
    }

    private fun handleClearMessages() {
        _state.value = _state.value.copy(
            error = null,
            successMessage = null
        )
    }
}