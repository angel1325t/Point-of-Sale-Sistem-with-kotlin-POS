package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders

import java.io.File
import java.util.UUID

sealed class SalesIntent {

    // 🔍 Product Search
    data class SearchProductByName(val query: String) : SalesIntent()
    data class SearchProductByBarcode(val barcode: String) : SalesIntent()
    object ClearSearchResults : SalesIntent()

    // 📟 Barcode Reader
    object ToggleBarcodeReader : SalesIntent()
    data class ProcessBarcodeFromReader(val barcode: String) : SalesIntent()

    // 🛒 Cart Management
    data class AddSaleDetail(
        val productId: Int,
        val quantity: Int,
        val unitPrice: Double,
        val discount: Double = 0.0
    ) : SalesIntent()

    data class RemoveSaleDetail(val productId: Int) : SalesIntent()

    data class UpdateSaleDetail(
        val productId: Int,
        val quantity: Int,
        val unitPrice: Double,
        val discount: Double = 0.0
    ) : SalesIntent()

    // 🧾 Sale Creation
    data class CreateSale(
        val paymentMethod: String,
        val globalDiscount: Double = 0.0,
        val userId: UUID
    ) : SalesIntent()

    // 💳 Payment Methods
    data class UpdatePaymentMethod(val method: String) : SalesIntent()

    object CompleteSale : SalesIntent()

    // 💵 Cash Payment
    data class ConfirmCashPayment(val amountReceived: Double) : SalesIntent()

    // 💳 Card Payment (Stripe)
    object InitiateCardPayment : SalesIntent()
    data class ProcessStripeResult(val paymentIntentId: String) : SalesIntent()

    // 🏦 Transfer Payment
    object CaptureTransferEvidence : SalesIntent()
    data class SubmitTransferEvidence(
        val imageFile: File,
        val referenceNumber: String
    ) : SalesIntent()

    // 🎫 Credit Notes
    data class ApplyCreditNote(
        val creditNoteId: String,
        val amountApplied: Double
    ) : SalesIntent()

    data class RemoveCreditNote(val creditNoteId: UUID) : SalesIntent()

    // 🔄 Lifecycle
    object ClearSale : SalesIntent()
    object ClearError : SalesIntent()
    object SyncProducts : SalesIntent()
}