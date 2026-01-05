    package com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes

    import com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds.RefundError
    import java.util.UUID


    /**
     * Estado del módulo de notas de crédito
     */
    data class CreditNoteState(
        val isLoading: Boolean = false,
        val error: RefundError? = null,

        // Búsqueda y nota seleccionada
        val searchQuery: String = "",
        val foundCreditNote: CreditNote? = null,

        // Aplicación de crédito
        val creditApplication: CreditApplication? = null,

        // Control de UI
        val showQRScanner: Boolean = false,
        val showConfirmationDialog: Boolean = false,
        val showSuccessDialog: Boolean = false,
        val navigateBack: Boolean = false,

        // Resultado
        val appliedCreditNoteId: UUID? = null,
        val remainingPaymentRequired: Double = 0.0
    ) {
        val canApplyCredit: Boolean
            get() = foundCreditNote != null &&
                    !foundCreditNote.isFullyUsed &&
                    !isLoading
    }