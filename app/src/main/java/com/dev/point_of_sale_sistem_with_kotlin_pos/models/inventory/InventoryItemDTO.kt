package com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class InventoryItemDTO(
    @SerialName("product_id")
    val id: Int,
    val name: String,
    val description: String? = null,
    val price: Double,
    val barcode: String? = null,
    @SerialName("category_id")
    val categoryId: Int,
    val image: String? = null,
    @SerialName("current_stock")
    val stock: Int,
    @SerialName("minimum_stock")
    val minStock: Int,

    // Add these missing fields that exist in the database
    @SerialName("discount_type")
    val discountType: String = "none",

    @SerialName("discount_value")
    val discountValue: Double = 0.0,

    @SerialName("final_price")
    val finalPrice: Double? = null,

    @SerialName("branch_id")
    val branchId: String = "",

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)