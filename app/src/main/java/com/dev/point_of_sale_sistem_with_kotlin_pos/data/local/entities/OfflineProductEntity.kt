package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "offline_product_stock")
data class OfflineProductStockEntity(

    @PrimaryKey val productId: Int,

    val currentStock: Int
)
