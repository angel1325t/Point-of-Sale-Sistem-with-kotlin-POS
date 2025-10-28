package com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth

sealed class AuthError {
    object InvalidCredentials : AuthError()
    data class UsernameTaken(val username: String) : AuthError()

    data class EmailTaken(val email: String) : AuthError()
    data class CompanyEmailTaken(val email: String) : AuthError()
    data class CompanyNameTaken(val name: String) : AuthError()
    data class BranchNameTaken(val name: String) : AuthError()
    data class Other(val customMessage: String) : AuthError()
}
