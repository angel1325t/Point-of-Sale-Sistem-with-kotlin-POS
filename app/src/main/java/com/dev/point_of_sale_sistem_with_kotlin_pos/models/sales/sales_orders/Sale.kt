package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
@Serializable
data class SaleInsertDTO(
    @SerialName("sale_id")
    val saleId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("sale_date")
    val saleDate: String,
    @SerialName("total")
    val total: Double,
    @SerialName("payment_method")
    val paymentMethod: String,
    @SerialName("status")
    val status: String,
    @SerialName("global_discount")
    val globalDiscount: Double
)

