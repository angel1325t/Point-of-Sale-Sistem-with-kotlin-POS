package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders

import androidx.activity.ComponentActivity
import java.io.File
import java.util.UUID

sealed class SalesIntent {
    // Búsqueda
    data class SearchProductByName(val query: String) : SalesIntent()
    data class SearchProductByBarcode(val barcode: String) : SalesIntent()
    object ClearSearchResults : SalesIntent()

    // Gestión de productos
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

    // Venta
    data class CreateSale(
        val paymentMethod: String,
        val globalDiscount: Double,
        val userId: UUID
    ) : SalesIntent()

    data class UpdatePaymentMethod(val method: String) : SalesIntent()

    object CompleteSale : SalesIntent()

    // Pagos
    data class ConfirmCashPayment(val amountReceived: Double) : SalesIntent()

    object InitiateCardPayment : SalesIntent()


    data class ProcessStripeResult(val paymentIntentId: String) : SalesIntent()

    object CaptureTransferEvidence : SalesIntent()

    data class SubmitTransferEvidence(
        val imageFile: File,
        val referenceNumber: String
    ) : SalesIntent()

    // Control
    object ClearSale : SalesIntent()
    object ClearError : SalesIntent()
}