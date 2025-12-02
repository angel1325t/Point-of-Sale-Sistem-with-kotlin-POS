package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers

/**
 * Clase sellada que define todos los posibles errores en operaciones con Suppliers
 * Hereda de Exception para poder usarse con Result<T>
 */
sealed class SupplierError(
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
    ) : SupplierError(message, cause)

    data class InvalidPhoneError(
        override val message: String = "El teléfono debe tener al menos 8 dígitos",
        override val cause: Throwable? = null
    ) : SupplierError(message, cause)

    data class InvalidEmailError(
        override val message: String = "El email no tiene un formato válido",
        override val cause: Throwable? = null
    ) : SupplierError(message, cause)

    data class DuplicateNameError(
        override val message: String = "Ya existe un proveedor con ese nombre",
        override val cause: Throwable? = null
    ) : SupplierError(message, cause)

    // ───────────────────────────────
    // Errores de Base de Datos
    // ───────────────────────────────

    data class RecordNotFound(
        override val message: String = "Proveedor no encontrado",
        override val cause: Throwable? = null
    ) : SupplierError(message, cause)

    data class DatabaseError(
        override val message: String = "Error en la base de datos",
        override val cause: Throwable? = null
    ) : SupplierError(message, cause)

    // ───────────────────────────────
    // Errores de Red
    // ───────────────────────────────

    data class NetworkError(
        override val message: String = "Error de conexión. Verifica tu internet",
        override val cause: Throwable? = null
    ) : SupplierError(message, cause)

    data class TimeoutError(
        override val message: String = "La operación tardó demasiado. Intenta de nuevo",
        override val cause: Throwable? = null
    ) : SupplierError(message, cause)

    // ───────────────────────────────
    // Errores de Autorización
    // ───────────────────────────────

    data class UnauthorizedError(
        override val message: String = "No tienes permisos para realizar esta acción",
        override val cause: Throwable? = null
    ) : SupplierError(message, cause)

    // ───────────────────────────────
    // Error Genérico
    // ───────────────────────────────

    data class UnknownError(
        override val message: String = "Ocurrió un error inesperado",
        override val cause: Throwable? = null
    ) : SupplierError(message, cause)

    // ───────────────────────────────
    // Helpers
    // ───────────────────────────────

    /**
     * Obtiene un mensaje amigable para el usuario
     */
    fun getUserMessage(): String = message

    /**
     * Verifica si es un error de validación
     */
    fun isValidationError(): Boolean = this is ValidationError ||
            this is InvalidPhoneError ||
            this is InvalidEmailError ||
            this is DuplicateNameError

    /**
     * Verifica si es un error de red/conectividad
     */
    fun isNetworkError(): Boolean = this is NetworkError || this is TimeoutError

    /**
     * Verifica si es un error recuperable (el usuario puede reintentar)
     */
    fun isRecoverable(): Boolean = isNetworkError() || this is TimeoutError
}