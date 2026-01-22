package com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth

data class SessionState(
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val email: String? = null,
    val branchId: String? = null,
    val profilePhotoUrl: String? = null,
    val isLoading: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val isChangingEmail: Boolean = false,
    val pendingEmailChange: String? = null,
    val codeResendCooldown: Long = 0,
    val codeExpiryTime: Long = 0,
    val error: AuthError? = null,
    val successMessage: String? = null,
    val isUserDisabled: Boolean = false,
    val isBiometricEnabled: Boolean = true
)