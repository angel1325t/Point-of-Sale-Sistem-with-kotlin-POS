package com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth

@kotlinx.serialization.Serializable
data class UserFullInfoDTO(
    val user_id: String,
    val auth_id: String,
    val email: String,
    val branch_id: String?,
    val active: Boolean
)
