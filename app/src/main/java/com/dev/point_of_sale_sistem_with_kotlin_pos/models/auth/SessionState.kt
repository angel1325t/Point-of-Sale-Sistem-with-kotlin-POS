package com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth

data class SessionState(
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val email: String? = null,
    val branchId: String? = null,
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val successMessage: String? = null
)