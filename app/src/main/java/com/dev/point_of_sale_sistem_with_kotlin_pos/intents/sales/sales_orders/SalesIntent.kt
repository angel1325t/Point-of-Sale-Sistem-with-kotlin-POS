package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders

import java.io.File
import java.util.UUID

sealed class SalesIntent {
    // ═══════════════════════════════════════════════════
    // 🔍 BÚSQUEDA DE PRODUCTOS
    // ═══════════════════════════════════════════════════
    data class SearchProductByName(val query: String) : SalesIntent()
    data class SearchProductByBarcode(val barcode: String) : SalesIntent()
    object ClearSearchResults : SalesIntent()

    // ═══════════════════════════════════════════════════
    // 🛍️ GESTIÓN DE PRODUCTOS EN VENTA
    // ═══════════════════════════════════════════════════
    data class AddSaleDetail(
        val productId: Int,
        val quantity: Int,
        val unitPrice: Double,
        val discount: Double
    ) : SalesIntent()

    data class UpdateSaleDetail(
        val productId: Int,
        val quantity: Int,
        val unitPrice: Double,
        val discount: Double
    ) : SalesIntent()

    data class RemoveSaleDetail(val productId: Int) : SalesIntent()

    // ═══════════════════════════════════════════════════
    // 💳 GESTIÓN DE VENTA Y MÉTODOS DE PAGO
    // ═══════════════════════════════════════════════════
    data class CreateSale(
        val paymentMethod: String,
        val globalDiscount: Double,
        val userId: UUID
    ) : SalesIntent()

    data class UpdatePaymentMethod(val method: String) : SalesIntent()

    object CompleteSale : SalesIntent()

    // ═══════════════════════════════════════════════════
    // 💵 PAGO EN EFECTIVO
    // ═══════════════════════════════════════════════════
    data class ConfirmCashPayment(val amountReceived: Double) : SalesIntent()

    // ═══════════════════════════════════════════════════
    // 🏦 PAGO POR TRANSFERENCIA CON EVIDENCIA
    // ═══════════════════════════════════════════════════
    object CaptureTransferEvidence : SalesIntent()

    data class SubmitTransferEvidence(
        val imageFile: File,
        val referenceNumber: String
    ) : SalesIntent()

    // ═══════════════════════════════════════════════════
    // 🔄 CONTROL DE FLUJO
    // ═══════════════════════════════════════════════════
    object ClearSale : SalesIntent()
    object ClearError : SalesIntent()
}