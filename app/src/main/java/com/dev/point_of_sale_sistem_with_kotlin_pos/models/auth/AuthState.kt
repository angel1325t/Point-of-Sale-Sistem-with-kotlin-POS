package com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth

sealed class AuthError {
    object InvalidCredentials : AuthError()
    data class Other(val message: String) : AuthError()
}
data class AuthState(
    val isLoading: Boolean = false,            // indica si se está procesando algo (login, registro, etc.)
    val isAuthenticated: Boolean = false,      // true si el usuario ya inició sesión
    val userId: String? = null,                // ID del usuario logueado
    val email: String? = null,                 // email del usuario
    val error: AuthError? = null,           // errores (login fallido, usuario existente, etc.)
    val successMessage: String? = null         // mensajes informativos (registro exitoso, logout, etc.)
)
