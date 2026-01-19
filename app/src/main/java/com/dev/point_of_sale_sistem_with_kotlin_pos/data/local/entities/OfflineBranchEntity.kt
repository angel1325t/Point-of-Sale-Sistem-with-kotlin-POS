package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_branches_cache")
data class OfflineBranchEntity(
    @PrimaryKey val branchId: String,
    val companyId: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String
)
