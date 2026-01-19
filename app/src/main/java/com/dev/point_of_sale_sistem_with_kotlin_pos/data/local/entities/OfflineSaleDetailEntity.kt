package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "offline_sale_details",
    foreignKeys = [
        ForeignKey(
            entity = OfflineSaleEntity::class,
            parentColumns = ["localSaleId"],
            childColumns = ["localSaleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["localSaleId"])]
)
data class OfflineSaleDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "localSaleId")
    val localSaleId: String,

    val productId: Int,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double,
    val finalPrice: Double
)