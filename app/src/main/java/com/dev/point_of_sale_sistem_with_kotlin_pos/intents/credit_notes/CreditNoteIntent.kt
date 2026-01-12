package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.credit_notes

import java.util.UUID


/**
 * Acciones de usuario para usar notas de crédito
 */
sealed class CreditNoteIntent {

    data class SearchByQRCode(val qrContent: String) : CreditNoteIntent()
    data class SearchByInvoiceNumber(val invoiceNumber: String) : CreditNoteIntent()

    data class AmountChanged(val amount: Double) : CreditNoteIntent()

    data class ApplyCreditToSale(
        val creditNoteId: String,
        val saleTotal: Double
    ) : CreditNoteIntent()
    data class SetSaleTotal(
        val saleTotal: Double
    ) : CreditNoteIntent()

    object ConfirmCreditApplication : CreditNoteIntent()
    object ShowQRScanner : CreditNoteIntent()
    object HideQRScanner : CreditNoteIntent()
    object ClearError : CreditNoteIntent()
    object NavigateBack : CreditNoteIntent()
}
