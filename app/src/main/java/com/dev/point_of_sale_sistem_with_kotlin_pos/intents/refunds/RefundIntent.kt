package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.refunds

/**
 * Acciones de usuario en el módulo de devoluciones
 */
sealed class RefundIntent {
    // Búsqueda de venta
    data class SearchByInvoiceNumber(val invoiceNumber: String) : RefundIntent()
    data class SearchByQRCode(val qrContent: String) : RefundIntent()
    data object ClearSearch : RefundIntent()

    // Selección de items para devolución
    data class SelectItemForRefund(
        val saleDetailId: Int,
        val quantity: Int
    ) : RefundIntent()

    data class UpdateRefundQuantity(
        val saleDetailId: Int,
        val quantity: Int
    ) : RefundIntent()

    data class RemoveRefundItem(val saleDetailId: Int) : RefundIntent()

    // Procesamiento de devolución
    data object ProcessRefund : RefundIntent()
    data object ConfirmRefund : RefundIntent()

    // Control de UI
    data object ShowQRScanner : RefundIntent()
    data object HideQRScanner : RefundIntent()
    data object ClearError : RefundIntent()
    data object NavigateBack : RefundIntent()
}