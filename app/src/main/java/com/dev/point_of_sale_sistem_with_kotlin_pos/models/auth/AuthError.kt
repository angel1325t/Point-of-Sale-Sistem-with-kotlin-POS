package com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth

sealed class AuthError(val message: String) {
    object InvalidCredentials : AuthError("Credenciales inválidas")
    data class UsernameTaken(val username: String) : AuthError("El nombre de usuario '$username' ya está en uso")
    data class CompanyNameTaken(val name: String) : AuthError("El nombre de la compañía '$name' ya está en uso")
    data class BranchNameTaken(val name: String) : AuthError("El nombre de la sucursal '$name' ya está en uso")
    data class Other(val customMessage: String) : AuthError(customMessage)
}