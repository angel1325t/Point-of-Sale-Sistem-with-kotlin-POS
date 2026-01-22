package com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory

import kotlinx.serialization.Serializable

@Serializable
data class InventoryItem(
    val id: Int,
    val name: String,
    val description: String? = null,
    val price: Double,
    val barcode: String? = null,
    val categoryId: Int,
    val categoryName: String? = null,
    val stock: Int,
    val minStock: Int,
    val image: String? = null,
    // Optionally add discount fields here too if needed in the UI
    val discountType: String = "none",
    val discountValue: Double = 0.0,
    val finalPrice: Double? = null
)