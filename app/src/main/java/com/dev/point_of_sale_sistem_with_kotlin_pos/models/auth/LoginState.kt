package com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError

data class LoginState(
    val isLoading: Boolean = false,
    val email: String = "",
    val password: String = "",
    val error: AuthError? = null,
    val successMessage: String? = null
)