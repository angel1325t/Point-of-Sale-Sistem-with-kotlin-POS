package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.credit_notes

import java.util.UUID


/**
 * Acciones de usuario para usar notas de crédito
 */
sealed class CreditNoteIntent {
    // Búsqueda de nota de crédito
    data class SearchByInvoiceNumber(val invoiceNumber: String) : CreditNoteIntent()
    data class SearchByQRCode(val qrContent: String) : CreditNoteIntent()
    data object ClearSearch : CreditNoteIntent()

    // Aplicación de crédito
    data class ApplyCreditToSale(
        val creditNoteId: UUID,
        val saleTotal: Double
    ) : CreditNoteIntent()

    data object ConfirmCreditApplication : CreditNoteIntent()

    // Control de UI
    data object ShowQRScanner : CreditNoteIntent()
    data object HideQRScanner : CreditNoteIntent()
    data object ClearError : CreditNoteIntent()
    data object NavigateBack : CreditNoteIntent()
}
