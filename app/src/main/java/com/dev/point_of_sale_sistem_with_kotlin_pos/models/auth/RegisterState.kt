package com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth

data class RegisterState(
    val currentStep: Int = 1,

    // Paso 1: Datos de usuario
    val userName: String = "",
    val userEmail: String = "",
    val userPassword: String = "",
    val userConfirmPassword: String = "",
    val showPassword: Boolean = false,
    val showConfirmPassword: Boolean = false,

    // Paso 2: Datos del negocio
    val businessName: String = "",
    val businessEmail: String = "",
    val businessAddress: String = "",
    val businessPhone: String = "",

    // Estado de la interfaz
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val successMessage: String? = null
)
