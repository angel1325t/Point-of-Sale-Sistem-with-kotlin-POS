package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para leer datos de Supabase
 * Coincide con la estructura de la tabla "suppliers"
 */
@Serializable
data class SupplierDTO(

    @SerialName("supplier_id")
    val supplierId: Int,

    @SerialName("name")
    val name: String,

    @SerialName("contact")
    val contact: String? = null,

    @SerialName("phone")
    val phone: String? = null,

    @SerialName("email")
    val email: String? = null,

    @SerialName("address")
    val address: String? = null,

    @SerialName("branch_id")
    val branchId: String,

    @SerialName("company_id")
    val companyId: String,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)
/**
 * DTO para insertar nuevos proveedores
 * supplier_id se genera automáticamente
 */
@Serializable
data class SupplierInsertDTO(

    @SerialName("name")
    val name: String,

    @SerialName("contact")
    val contact: String? = null,

    @SerialName("phone")
    val phone: String? = null,

    @SerialName("email")
    val email: String? = null,

    @SerialName("address")
    val address: String? = null,

    @SerialName("branch_id")
    val branchId: String,

    @SerialName("company_id")
    val companyId: String
)
/**
 * DTO para actualizar proveedores existentes
 * branch_id y company_id NO se actualizan
 */
@Serializable
data class SupplierUpdateDTO(

    @SerialName("name")
    val name: String,

    @SerialName("contact")
    val contact: String? = null,

    @SerialName("phone")
    val phone: String? = null,

    @SerialName("email")
    val email: String? = null,

    @SerialName("address")
    val address: String? = null
)
