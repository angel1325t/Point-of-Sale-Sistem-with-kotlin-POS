package com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryItemDTO(
    @SerialName("product_id") val id: Int,
    val name: String,
    val description: String? = null,
    val price: Double,
    val barcode: String? = null,
    @SerialName("category_id") val categoryId: Int?,
    @SerialName("current_stock") val stock: Int,
    @SerialName("minimum_stock") val minStock: Int,
    val image: String? = null
)