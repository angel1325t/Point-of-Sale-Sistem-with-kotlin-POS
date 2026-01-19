package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_products_cache")
data class OfflineProductCacheEntity(
    @PrimaryKey val productId: Int,
    val name: String,
    val barcode: String?,
    val price: Double,
    val currentStock: Int,
    val categoryId: Int,
    val branchId: String
)
