package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders

/**
 * Errores específicos del módulo de pedidos
 */
sealed class PurchaseOrderError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    // ───────────────────────────────
    // Errores de Validación
    // ───────────────────────────────

    data class ValidationError(
        override val message: String,
        val field: String? = null,
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    data class InvalidQuantityError(
        override val message: String = "La cantidad debe ser mayor a 0",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    data class SupplierNotSelectedError(
        override val message: String = "Debe seleccionar un proveedor",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    data class ProductNotSelectedError(
        override val message: String = "Debe seleccionar un producto",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    // ───────────────────────────────
    // Errores de Base de Datos
    // ───────────────────────────────

    data class RecordNotFound(
        override val message: String = "Pedido no encontrado",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    data class DatabaseError(
        override val message: String = "Error en la base de datos",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    // ───────────────────────────────
    // Errores de Red
    // ───────────────────────────────

    data class NetworkError(
        override val message: String = "Error de conexión. Verifica tu internet",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    data class TimeoutError(
        override val message: String = "La operación tardó demasiado. Intenta de nuevo",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    // ───────────────────────────────
    // Errores de Reglas de Negocio
    // ───────────────────────────────

    data class CannotModifyReceivedOrder(
        override val message: String = "No se puede modificar un pedido ya recibido",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    data class StockUpdateError(
        override val message: String = "Error al actualizar el inventario",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    // ───────────────────────────────
    // Errores de Autorización
    // ───────────────────────────────

    data class UnauthorizedError(
        override val message: String = "No tienes permisos para realizar esta acción",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    // ───────────────────────────────
    // Error Genérico
    // ───────────────────────────────

    data class UnknownError(
        override val message: String = "Ocurrió un error inesperado",
        override val cause: Throwable? = null
    ) : PurchaseOrderError(message, cause)

    // ───────────────────────────────
    // Helpers
    // ───────────────────────────────

    fun getUserMessage(): String = message

    fun isValidationError(): Boolean = this is ValidationError ||
            this is InvalidQuantityError ||
            this is SupplierNotSelectedError ||
            this is ProductNotSelectedError

    fun isNetworkError(): Boolean = this is NetworkError ||
            this is TimeoutError

    fun isRecoverable(): Boolean = isNetworkError() ||
            this is TimeoutError
}