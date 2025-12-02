package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories

/**
 * Representa los diferentes tipos de errores que pueden ocurrir en el CRUD de categorías
 */
sealed class CategoryError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    abstract val code: String?

    // Errores de red/conexión
    data class NetworkError(
        override val message: String = "Error de conexión. Verifica tu internet.",
        override val code: String? = "NETWORK_ERROR"
    ) : CategoryError(message)

    data class TimeoutError(
        override val message: String = "La operación tardó demasiado tiempo.",
        override val code: String? = "TIMEOUT_ERROR"
    ) : CategoryError(message)

    // Errores de base de datos
    data class DatabaseError(
        override val message: String = "Error al acceder a la base de datos.",
        override val code: String? = "DB_ERROR",
        val details: String? = null
    ) : CategoryError(message)

    data class RecordNotFound(
        override val message: String = "La categoría no fue encontrada.",
        override val code: String? = "NOT_FOUND"
    ) : CategoryError(message)

    // Errores de validación
    data class ValidationError(
        override val message: String,
        override val code: String? = "VALIDATION_ERROR",
        val field: String? = null
    ) : CategoryError(message)

    data class DuplicateNameError(
        override val message: String = "Ya existe una categoría con este nombre.",
        override val code: String? = "DUPLICATE_NAME"
    ) : CategoryError(message)

    // Errores de reglas de negocio
    data class CannotDeleteParentCategory(
        override val message: String = "No se puede eliminar una categoría que tiene subcategorías.",
        override val code: String? = "HAS_CHILDREN",
        val childCount: Int = 0
    ) : CategoryError(message)

    data class CircularReferenceError(
        override val message: String = "No se puede asignar una categoría como su propia categoría padre.",
        override val code: String? = "CIRCULAR_REFERENCE"
    ) : CategoryError(message)

    data class MaxDepthExceeded(
        override val message: String = "Se ha excedido el nivel máximo de anidación de categorías.",
        override val code: String? = "MAX_DEPTH_EXCEEDED",
        val maxDepth: Int = 5
    ) : CategoryError(message)

    // Errores de autenticación/autorización
    data class UnauthorizedError(
        override val message: String = "No tienes permisos para realizar esta acción.",
        override val code: String? = "UNAUTHORIZED"
    ) : CategoryError(message)

    data class SessionExpiredError(
        override val message: String = "Tu sesión ha expirado. Por favor, inicia sesión nuevamente.",
        override val code: String? = "SESSION_EXPIRED"
    ) : CategoryError(message)

    // Errores generales
    data class UnknownError(
        override val message: String = "Ocurrió un error inesperado.",
        override val code: String? = "UNKNOWN_ERROR",
        val exception: Throwable? = null
    ) : CategoryError(message, exception)

    data class ServerError(
        override val message: String = "Error del servidor. Intenta más tarde.",
        override val code: String? = "SERVER_ERROR",
        val statusCode: Int? = null
    ) : CategoryError(message)
}

/**
 * Extensiones útiles para CategoryError
 */
fun CategoryError.isRetryable(): Boolean {
    return when (this) {
        is CategoryError.NetworkError,
        is CategoryError.TimeoutError,
        is CategoryError.ServerError -> true
        else -> false
    }
}

fun CategoryError.getUserFriendlyMessage(): String {
    return when (this) {
        is CategoryError.ValidationError -> {
            field?.let { "Error en $it: $message" } ?: message
        }
        is CategoryError.CannotDeleteParentCategory -> {
            "$message (${childCount} subcategoría${if (childCount > 1) "s" else ""})"
        }
        is CategoryError.MaxDepthExceeded -> {
            "$message (máximo: $maxDepth niveles)"
        }
        is CategoryError.ServerError -> {
            statusCode?.let { "$message (Código: $it)" } ?: message
        }
        is CategoryError.DatabaseError -> {
            details?.let { "$message - $it" } ?: message
        }
        is CategoryError.UnknownError -> {
            exception?.message?.let { "$message: $it" } ?: message
        }
        else -> message
    }
}