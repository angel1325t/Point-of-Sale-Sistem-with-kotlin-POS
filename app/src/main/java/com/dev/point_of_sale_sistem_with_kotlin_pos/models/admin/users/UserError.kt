package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users

sealed class UserError {
    object ConnectionError : UserError()
    object UserNotFound : UserError()
    object EmailAlreadyExists : UserError()
    object AuthError : UserError()
    data class Other(val message: String?) : UserError()
}