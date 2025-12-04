package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

import java.time.LocalDateTime

data class SaleDetail(
    val saleDetailId: Int,
    val saleId: String,
    val productId: Int,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double,
    val finalPrice: Double,
    val createdAt: LocalDateTime
)