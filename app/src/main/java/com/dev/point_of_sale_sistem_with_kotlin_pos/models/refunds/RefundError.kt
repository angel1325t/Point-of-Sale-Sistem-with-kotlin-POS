package com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds

/**
 * Errores del módulo de devoluciones
 */
sealed class RefundError {
    object InvoiceNotFound : RefundError()
    object SaleAlreadyFullyRefunded : RefundError()
    object InvalidRefundAmount : RefundError()
    object ExceedsAvailableQuantity : RefundError()
    object CreditNoteNotFound : RefundError()
    object CreditNoteFullyUsed : RefundError()
    object Network : RefundError()
    data class Validation(val message: String) : RefundError()
    data class Server(val message: String) : RefundError()
    data class Unknown(val throwable: Throwable) : RefundError()
}

/**
 * Extensión para convertir errores a mensajes de usuario
 */
fun RefundError.toUserMessage(): String = when (this) {
    is RefundError.InvoiceNotFound -> "Factura no encontrada"
    is RefundError.SaleAlreadyFullyRefunded -> "Esta venta ya ha sido devuelta completamente"
    is RefundError.InvalidRefundAmount -> "Monto de devolución inválido"
    is RefundError.ExceedsAvailableQuantity -> "Cantidad excede lo disponible para devolución"
    is RefundError.CreditNoteNotFound -> "Nota de crédito no encontrada"
    is RefundError.CreditNoteFullyUsed -> "Esta nota de crédito ya fue utilizada"
    is RefundError.Network -> "Error de conexión. Verifica tu internet"
    is RefundError.Validation -> message
    is RefundError.Server -> "Error del servidor: $message"
    is RefundError.Unknown -> "Error inesperado: ${throwable.message}"
}