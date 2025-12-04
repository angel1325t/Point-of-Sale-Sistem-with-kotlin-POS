package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetail
import java.time.LocalDateTime
import java.util.UUID

data class Sale(
    val saleId: UUID,
    val userId: UUID,
    val saleDate: LocalDateTime,
    val total: Double,
    val paymentMethod: String,
    val status: String,
    val createdAt: LocalDateTime,
    val globalDiscount: Double,
    val saleDetails: List<SaleDetail> = emptyList()
)