package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO

data class SaleState(
    val sale: Sale? = null,
    val searchResults: List<ProductDTO> = emptyList(),
    val productsCache: Map<Int, ProductDTO> = emptyMap(),
    val isLoading: Boolean = false,
    val error: SaleError? = null,
    val lastScannedBarcode: String? = null,
    val paymentFlowState: PaymentFlowState = PaymentFlowState.Idle,
    val transferEvidence: TransferEvidenceData? = null,
    val appliedCreditNotes: List<AppliedCreditNote> = emptyList(),

    // 🆕 ESTADO DEL LECTOR DE CÓDIGOS
    val isBarcodeReaderActive: Boolean = false,
    val barcodeReaderBuffer: String = ""
) {
    val totalCreditApplied: Double
        get() = appliedCreditNotes.sumOf { it.amountApplied }

    val remainingToPay: Double
        get() {
            val saleTotal = sale?.total ?: 0.0
            return maxOf(0.0, saleTotal - totalCreditApplied)
        }

    val isCoveredByCredit: Boolean
        get() = remainingToPay <= 0.0
}