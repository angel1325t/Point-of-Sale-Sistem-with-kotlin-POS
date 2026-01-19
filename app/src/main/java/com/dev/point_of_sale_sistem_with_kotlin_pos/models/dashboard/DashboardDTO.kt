package com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO para leer datos de ventas desde Supabase
 * Este es un contrato genérico que asume la estructura de la tabla `sale`
 */
@Serializable
data class SaleDTO(
    @SerialName("sale_id")
    val saleId: Int,

    @SerialName("total_amount")
    val totalAmount: Double,

    @SerialName("sale_date")
    val saleDate: String,

    @SerialName("payment_method")
    val paymentMethod: String? = null,

    @SerialName("branch_id")
    val branchId: Int? = null,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("status")
    val status: String? = null
)

/**
 * DTO para los ítems de venta (sale_items)
 */
@Serializable
data class SaleItemDTO(
    @SerialName("sale_item_id")
    val saleItemId: Int,

    @SerialName("sale_id")
    val saleId: Int,

    @SerialName("product_id")
    val productId: Int,

    @SerialName("product_name")
    val productName: String,

    @SerialName("quantity")
    val quantity: Int,

    @SerialName("unit_price")
    val unitPrice: Double,

    @SerialName("subtotal")
    val subtotal: Double
)

/**
 * DTO para agregados de productos más vendidos
 */
@Serializable
data class TopProductDTO(
    @SerialName("product_id")
    val productId: Int,

    @SerialName("product_name")
    val productName: String,

    @SerialName("total_quantity")
    val totalQuantity: Int,

    @SerialName("total_revenue")
    val totalRevenue: Double
)

/**
 * DTO para agregados de ingresos por fecha
 */
@Serializable
data class RevenueDateDTO(
    @SerialName("sale_date")
    val saleDate: String,

    @SerialName("total_revenue")
    val totalRevenue: Double,

    @SerialName("sales_count")
    val salesCount: Int
)