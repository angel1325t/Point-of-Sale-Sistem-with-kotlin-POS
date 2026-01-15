package com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds

import java.util.UUID

/**
 * Estado del módulo de devoluciones
 */

data class RefundState(
    val isLoading: Boolean = false,
    val error: RefundError? = null,

    // Búsqueda y venta seleccionada
    val searchQuery: String = "",
    val foundSale: RefundableSale? = null,

    // Items seleccionados para devolución
    val selectedItems: List<RefundItem> = emptyList(),

    // Control de UI
    val showQRScanner: Boolean = false,
    val showConfirmationDialog: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val navigateBack: Boolean = false,

    // Información calculada (con ITBIS)
    val refundSubtotal: Double = 0.0,
    val refundItbis: Double = 0.0,
    val totalRefundAmount: Double = 0.0,
    val createdCreditNoteId: UUID? = null
) {
    val hasSelectedItems: Boolean
        get() = selectedItems.isNotEmpty()

    val canProcessRefund: Boolean
        get() = hasSelectedItems &&
                foundSale != null &&
                totalRefundAmount > 0 &&
                !isLoading
}