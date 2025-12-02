package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products

/**
 * Representa los diferentes tipos de errores que pueden ocurrir en el CRUD de productos
 */
sealed class ProductsError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    abstract val code: String?

    // ───────────────────────────────
    // Errores de red/conexión
    // ───────────────────────────────
    data class NetworkError(
        override val message: String = "Error de conexión. Verifica tu internet.",
        override val code: String? = "NETWORK_ERROR"
    ) : ProductsError(message)

    data class TimeoutError(
        override val message: String = "La operación tardó demasiado tiempo.",
        override val code: String? = "TIMEOUT_ERROR"
    ) : ProductsError(message)


    // ───────────────────────────────
    // Errores de base de datos
    // ───────────────────────────────
    data class DatabaseError(
        override val message: String = "Error al acceder a la base de datos.",
        override val code: String? = "DB_ERROR",
        val details: String? = null
    ) : ProductsError(message)

    data class RecordNotFound(
        override val message: String = "El producto no fue encontrado.",
        override val code: String? = "NOT_FOUND"
    ) : ProductsError(message)


    // ───────────────────────────────
    // Errores de validación
    // ───────────────────────────────
    data class ValidationError(
        override val message: String,
        override val code: String? = "VALIDATION_ERROR",
        val field: String? = null
    ) : ProductsError(message)

    data class DuplicateNameError(
        override val message: String = "Ya existe un producto con este nombre.",
        override val code: String? = "DUPLICATE_NAME"
    ) : ProductsError(message)

    data class DuplicateBarcodeError(
        override val message: String = "Este código de barras ya está registrado.",
        override val code: String? = "DUPLICATE_BARCODE"
    ) : ProductsError(message)

    data class InvalidPriceError(
        override val message: String = "El precio debe ser mayor que 0.",
        override val code: String? = "INVALID_PRICE"
    ) : ProductsError(message)

    data class InvalidStockError(
        override val message: String = "Los valores de stock no son válidos.",
        override val code: String? = "INVALID_STOCK"
    ) : ProductsError(message)


    // ───────────────────────────────
    // Errores de reglas de negocio
    // ───────────────────────────────
    data class CategoryNotFoundError(
        override val message: String = "La categoría asignada no existe.",
        override val code: String? = "CATEGORY_NOT_FOUND"
    ) : ProductsError(message)

    data class CannotDeleteProductWithStock(
        override val message: String = "No se puede eliminar un producto con stock disponible.",
        override val code: String? = "STOCK_REMAINING",
        val currentStock: Int = 0
    ) : ProductsError(message)

    data class ImageUploadError(
        override val message: String = "Error al subir la imagen del producto.",
        override val code: String? = "IMAGE_UPLOAD_ERROR"
    ) : ProductsError(message)


    // ───────────────────────────────
    // Errores de autenticación/autorización
    // ───────────────────────────────
    data class UnauthorizedError(
        override val message: String = "No tienes permisos para realizar esta acción.",
        override val code: String? = "UNAUTHORIZED"
    ) : ProductsError(message)

    data class SessionExpiredError(
        override val message: String = "Tu sesión ha expirado. Por favor, inicia sesión nuevamente.",
        override val code: String? = "SESSION_EXPIRED"
    ) : ProductsError(message)


    // ───────────────────────────────
    // Errores generales
    // ───────────────────────────────
    data class UnknownError(
        override val message: String = "Ocurrió un error inesperado.",
        override val code: String? = "UNKNOWN_ERROR",
        val exception: Throwable? = null
    ) : ProductsError(message, exception)

    data class ServerError(
        override val message: String = "Error del servidor. Intenta más tarde.",
        override val code: String? = "SERVER_ERROR",
        val statusCode: Int? = null
    ) : ProductsError(message)
}


/**
 * Extensiones útiles para ProductsError
 */
fun ProductsError.isRetryable(): Boolean {
    return when (this) {
        is ProductsError.NetworkError,
        is ProductsError.TimeoutError,
        is ProductsError.ServerError -> true
        else -> false
    }
}

fun ProductsError.getUserFriendlyMessage(): String {
    return when (this) {

        is ProductsError.ValidationError -> {
            field?.let { "Error en $it: $message" } ?: message
        }

        is ProductsError.CannotDeleteProductWithStock -> {
            "$message (stock actual: $currentStock)"
        }

        is ProductsError.ServerError -> {
            statusCode?.let { "$message (Código: $it)" } ?: message
        }

        is ProductsError.DatabaseError -> {
            details?.let { "$message - $it" } ?: message
        }

        is ProductsError.UnknownError -> {
            exception?.message?.let { "$message: $it" } ?: message
        }

        else -> message
    }
}
