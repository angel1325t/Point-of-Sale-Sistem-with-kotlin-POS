package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.roles

sealed class RoleError {
    data object ConnectionError : RoleError()
    data object RoleNotFound : RoleError()
    data class Other(val customMessage: String?) : RoleError()
}