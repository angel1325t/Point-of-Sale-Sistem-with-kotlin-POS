package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth

sealed class AuthIntent {

    // 🔹 Login
    data class Login(val email: String, val password: String) : AuthIntent()

    // 🔹 Logout
    object Logout : AuthIntent()

    // 🔹 Verificar sesión
    object CheckSession : AuthIntent()

    data class Register(
        val userEmail: String,
        val userPassword: String,
        val userName: String,
        val businessName: String,
        val businessEmail: String,
        val businessPhone: String?,
        val businessAddress: String?
    ) : AuthIntent()
}