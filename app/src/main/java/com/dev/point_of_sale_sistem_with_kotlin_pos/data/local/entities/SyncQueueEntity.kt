package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class  SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val entityType: String,   // SALE, CASH_REGISTER, etc
    val entityId: String,     // ID local
    val createdAt: Long = System.currentTimeMillis()
)
