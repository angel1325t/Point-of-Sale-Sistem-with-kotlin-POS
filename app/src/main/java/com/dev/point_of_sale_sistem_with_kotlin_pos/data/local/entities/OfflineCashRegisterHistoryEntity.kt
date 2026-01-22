package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_cash_register_history")
data class OfflineCashRegisterHistoryEntity(
    @PrimaryKey
    val localHistoryId: String, // UUID local

    val cashRegisterId: String,
    val authId: String,

    val openingDate: String,
    val closingDate: String? = null,

    val initialBalance: Double,
    val finalBalance: Double? = null,

    val isOpen: Boolean,
    val branchId: String,

    val expectedBalance: Double? = null,
    val difference: Double? = null,

    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
