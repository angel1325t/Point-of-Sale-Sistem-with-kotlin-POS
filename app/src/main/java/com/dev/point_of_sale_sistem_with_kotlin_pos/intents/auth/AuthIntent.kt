package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth

sealed class AuthIntent {
    data class Login(val email: String, val password: String) : AuthIntent()
    data class Register(
        val userEmail: String,
        val userPassword: String,
        val userName: String,
        val businessName: String,
        val businessEmail: String,
        val businessPhone: String?,
        val businessAddress: String?
    ) : AuthIntent()

    object Logout : AuthIntent()
    object CheckSession : AuthIntent()
    data class UserDisabled(val message: String? = null) : AuthIntent()
    data class ChangeBranch(val branchId: String) : AuthIntent()

// Password Reset Intents
    data class ResetPassword(val email: String) : AuthIntent()
    data class ResetPasswordWithToken(val token: String, val newPassword: String) : AuthIntent()
    data class ValidatePasswordResetToken(val token: String) : AuthIntent()
    data class VerifyPasswordResetCode(val email: String, val code: String) : AuthIntent()

// Email Reset Intents
    data class ResetPassword(val email: String) : AuthIntent()
    data class ResetPasswordWithToken(val token: String, val newPassword: String) : AuthIntent()
    data class ValidatePasswordResetToken(val token: String) : AuthIntent()
    data class VerifyPasswordResetCode(val email: String, val code: String) : AuthIntent()
    
    // Email Change Intents
    data class RequestEmailChange(val newEmail: String, val password: String) : AuthIntent()
    data class VerifyEmailChange(val code: String) : AuthIntent()
    
    // Profile Photo Intents
    data class UploadProfilePhoto(val imageUri: android.net.Uri) : AuthIntent()
    data class UpdateProfilePhotoUrl(val photoUrl: String) : AuthIntent()
    object ClearPhotoUploadState : AuthIntent()
    
    // Clear messages
    object ClearMessages : AuthIntent()
    
    // Resend OTP intent
    data class ResendOtp(val email: String) : AuthIntent()
}