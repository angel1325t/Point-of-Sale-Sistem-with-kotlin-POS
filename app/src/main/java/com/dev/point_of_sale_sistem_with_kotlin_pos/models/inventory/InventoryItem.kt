package com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryItem(
    @SerialName("product_id") val id: Int = 0,
    val name: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val barcode: String? = null,

    @SerialName("category_id")
    val categoryId: Int?,

    val categoryName: String? = null,

    @SerialName("current_stock")
    val stock: Int = 0,

    @SerialName("minimum_stock")
    val minStock: Int = 0,

    val image: String? = null
)
