package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineSaleDetailEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineSaleEntity

data class OfflineSaleWithDetails(

    @Embedded
    val sale: OfflineSaleEntity,

    @Relation(
        parentColumn = "localSaleId",
        entityColumn = "localSaleId"
    )
    val details: List<OfflineSaleDetailEntity>
)