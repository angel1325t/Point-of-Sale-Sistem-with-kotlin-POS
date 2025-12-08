package com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory

data class InventoryItem(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    val barcode: String?,
    val categoryId: Int?,
    val categoryName: String?,
    val stock: Int,
    val minStock: Int,
    val image: String?
)