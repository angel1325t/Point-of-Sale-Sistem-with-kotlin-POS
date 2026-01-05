package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders

import java.io.File
import java.util.UUID

sealed class SalesIntent {
    // Búsqueda de productos
    data class SearchProductByName(val query: String) : SalesIntent()
    data class SearchProductByBarcode(val barcode: String) : SalesIntent()
    data object ClearSearchResults : SalesIntent()

    // Gestión de productos en venta
    data class AddSaleDetail(
        val productId: Int,
        val quantity: Int,
        val unitPrice: Double,
        val discount: Double
    ) : SalesIntent()

    data class RemoveSaleDetail(val productId: Int) : SalesIntent()

    data class UpdateSaleDetail(
        val productId: Int,
        val quantity: Int,
        val unitPrice: Double,
        val discount: Double
    ) : SalesIntent()

    // Creación y gestión de venta
    data class CreateSale(
        val paymentMethod: String,
        val globalDiscount: Double,
        val userId: UUID
    ) : SalesIntent()

    data class UpdatePaymentMethod(val method: String) : SalesIntent()
    data object CompleteSale : SalesIntent()
    // Métodos de pago
    data class ConfirmCashPayment(val amountReceived: Double) : SalesIntent()
    data object InitiateCardPayment : SalesIntent()
    data class ProcessStripeResult(val paymentIntentId: String) : SalesIntent()
    data object CaptureTransferEvidence : SalesIntent()
    data class SubmitTransferEvidence(
        val imageFile: File,
        val referenceNumber: String
    ) : SalesIntent()

    // Control general
    data object ClearSale : SalesIntent()
    data object ClearError : SalesIntent()
}