package com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth

sealed class AuthError {
    object InvalidCredentials : AuthError()
    data class Other(val message: String) : AuthError()
}

data class LoginState(
    val isLoading: Boolean = false,     // indica si se está procesando el login
    val email: String = "",             // email del usuario
    val password: String = "",          // contraseña del usuario
    val error: AuthError? = null,       // errores (login fallido, etc.)
    val successMessage: String? = null  // mensajes informativos
)
