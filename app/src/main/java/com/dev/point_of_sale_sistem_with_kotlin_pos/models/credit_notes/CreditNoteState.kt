package com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes

/**
 * Estado del módulo de uso de notas de crédito (MVI)
 */
data class CreditNoteUsageState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // Nota de crédito escaneada
    val scannedCreditNote: ScannedCreditNote? = null,

    // Control de UI
    val showQRScanner: Boolean = true,
    val showConfirmationDialog: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val navigateBack: Boolean = false,

    // Aplicación de crédito
    val amountToApply: Double = 0.0,
    val saleTotal: Double = 0.0,
    val appliedSuccessfully: Boolean = false
) {

    val maxApplicableAmount: Double
        get() = scannedCreditNote?.let {
            minOf(it.creditRemaining, saleTotal)
        } ?: 0.0

    val canApplyCredit: Boolean
        get() =
            scannedCreditNote != null &&
                    amountToApply > 0 &&
                    amountToApply <= maxApplicableAmount &&
                    !isLoading
}
