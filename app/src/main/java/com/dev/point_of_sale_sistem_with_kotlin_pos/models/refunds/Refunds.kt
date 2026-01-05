package com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// ═══════════════════════════════════════════════════
// DTOs PARA DESERIALIZACIÓN DESDE SUPABASE
// ═══════════════════════════════════════════════════

@Serializable
data class RefundableSaleDTO(
    @SerialName("sale_id") val saleId: String,
    @SerialName("invoice_number") val invoiceNumber: String,
    @SerialName("sale_date") val saleDate: String,
    @SerialName("total") val total: Double,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("refunded_total") val refundedTotal: Double? = 0.0,
    @SerialName("is_credit_note") val isCreditNote: Boolean? = false,
    @SerialName("original_sale_id") val originalSaleId: String? = null,
    @SerialName("credit_remaining") val creditRemaining: Double? = null,
    @SerialName("sale_details") val saleDetails: List<RefundableSaleDetailDTO>
)

@Serializable
data class RefundableSaleDetailDTO(
    @SerialName("sale_detail_id") val saleDetailId: Int,
    @SerialName("product_id") val productId: Int,
    @SerialName("quantity") val quantity: Int,
    @SerialName("refunded_quantity") val refundedQuantity: Int? = 0,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("discount") val discount: Double,
    @SerialName("final_price") val finalPrice: Double,
    @SerialName("products") val products: ProductNameDTO
)

@Serializable
data class ProductNameDTO(
    @SerialName("name") val name: String
)

// ═══════════════════════════════════════════════════
// MODELOS DE DOMINIO
// ═══════════════════════════════════════════════════

/**
 * Venta con información de facturación y devoluciones
 */
data class RefundableSale(
    val saleId: UUID,
    val invoiceNumber: String,
    val saleDate: LocalDateTime,
    val total: Double,
    val paymentMethod: String,
    val refundedTotal: Double = 0.0,
    val isCreditNote: Boolean = false,
    val originalSaleId: UUID? = null,
    val creditRemaining: Double? = null,
    val details: List<RefundableSaleDetail>
) {
    val canBeRefunded: Boolean
        get() = !isCreditNote && (total - refundedTotal) > 0.01

    val availableForRefund: Double
        get() = total - refundedTotal
}

/**
 * Detalle de producto en venta con cantidades devueltas
 */
data class RefundableSaleDetail(
    val saleDetailId: Int,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val refundedQuantity: Int = 0,
    val unitPrice: Double,
    val discount: Double,
    val finalPrice: Double
) {
    val availableQuantity: Int
        get() = quantity - refundedQuantity

    val canBeRefunded: Boolean
        get() = availableQuantity > 0
}

/**
 * Item seleccionado para devolución
 */
data class RefundItem(
    val saleDetailId: Int,
    val productId: Int,
    val productName: String,
    val quantityToRefund: Int,
    val unitPrice: Double,
    val discount: Double
) {
    val refundAmount: Double
        get() = (unitPrice * quantityToRefund) - discount
}

// ═══════════════════════════════════════════════════
// CONVERSIÓN DTO -> DOMAIN
// ═══════════════════════════════════════════════════

fun RefundableSaleDTO.toDomain(): RefundableSale {
    return RefundableSale(
        saleId = UUID.fromString(saleId),
        invoiceNumber = invoiceNumber,
        saleDate = LocalDateTime.parse(saleDate, DateTimeFormatter.ISO_DATE_TIME),
        total = total,
        paymentMethod = paymentMethod,
        refundedTotal = refundedTotal ?: 0.0,
        isCreditNote = isCreditNote ?: false,
        originalSaleId = originalSaleId?.let { UUID.fromString(it) },
        creditRemaining = creditRemaining,
        details = saleDetails.map { it.toDomain() }
    )
}

fun RefundableSaleDetailDTO.toDomain(): RefundableSaleDetail {
    return RefundableSaleDetail(
        saleDetailId = saleDetailId,
        productId = productId,
        productName = products.name,
        quantity = quantity,
        refundedQuantity = refundedQuantity ?: 0,
        unitPrice = unitPrice,
        discount = discount,
        finalPrice = finalPrice
    )
}

