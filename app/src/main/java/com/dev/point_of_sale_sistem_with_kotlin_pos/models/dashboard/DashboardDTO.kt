package com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SaleDTO(
    @SerialName("sale_id")
    val saleId: String,

    @SerialName("total")
    val totalAmount: Double,

    @SerialName("sale_date")
    val saleDate: String,

    @SerialName("payment_method")
    val paymentMethod: String? = null,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("status")
    val status: String? = null,

    @SerialName("subtotal")
    val subtotal: Double = 0.0,

    @SerialName("itbis")
    val itbis: Double = 0.0,

    @SerialName("invoice_number")
    val invoiceNumber: String? = null
)

@Serializable
data class SaleItemDTO(
    @SerialName("sale_detail_id")
    val saleDetailId: Int,

    @SerialName("sale_id")
    val saleId: String,

    @SerialName("product_id")
    val productId: Int,

    @SerialName("quantity")
    val quantity: Int,

    @SerialName("unit_price")
    val unitPrice: Double,

    @SerialName("final_price")
    val finalPrice: Double,

    @SerialName("products")
    val product: ProductNameDTO? = null
) {
    val productName: String
        get() = product?.name ?: "Producto $productId"
}

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

@Serializable
data class RevenueDateDTO(
    @SerialName("sale_date")
    val saleDate: String,

    @SerialName("total_revenue")
    val totalRevenue: Double,

    @SerialName("sales_count")
    val salesCount: Int
)

@Serializable
data class ProductNameDTO(
    val name: String
)