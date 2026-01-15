package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users

import kotlinx.serialization.Serializable

@Serializable
data class CompanyResponse(
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val taxId: String? = null
)
data class BusinessInfo(
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val taxId: String? = null,
    val city: String? = null
)
