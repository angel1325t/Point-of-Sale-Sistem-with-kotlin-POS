package com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth

data class SessionState(
    val isAuthenticated: Boolean = false,

    // 🔐 Supabase Auth
    val authId: String? = null,
    val email: String? = null,

    // 🧠 Usuario interno (public.users)
    val userId: String? = null,
    val branchId: String? = null,

    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val successMessage: String? = null,
    val isUserDisabled: Boolean = false,
    val isBiometricEnabled: Boolean = true
)
