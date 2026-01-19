package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

@Serializable
data class SaleDetailInsertDTO(
    @SerialName("sale_id")
    val saleId: String,
    @SerialName("product_id")
    val productId: Int,
    @SerialName("quantity")
    val quantity: Int,
    @SerialName("unit_price")
    val unitPrice: Double,
    @SerialName("discount")
    val discount: Double,
    @SerialName("final_price")
    val finalPrice: Double
)