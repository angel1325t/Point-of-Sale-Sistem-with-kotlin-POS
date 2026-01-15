package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_sales")
data class OfflineSaleEntity(
    @PrimaryKey
    val localSaleId: String,

    val userId: String,
    val cashRegisterHistoryId: String,
    val subtotal: Double,
    val itbis: Double,
    val total: Double,
    val paymentMethod: String,
    val status: String,
    val createdAt: Long,

    // Campo para sincronización
    val pendingSync: Boolean = true,

    // Campos opcionales para soporte completo
    val globalDiscount: Double = 0.0,
    val invoiceNumber: String? = null
)