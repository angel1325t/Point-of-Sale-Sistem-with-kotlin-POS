package com.dev.point_of_sale_sistem_with_kotlin_pos.models.dashboard

/**
 * Representa los diferentes tipos de errores que pueden ocurrir en el Dashboard
 */
sealed class DashboardError(
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
    ) : DashboardError(message)

    data class TimeoutError(
        override val message: String = "La operación tardó demasiado tiempo.",
        override val code: String? = "TIMEOUT_ERROR"
    ) : DashboardError(message)

    // ───────────────────────────────
    // Errores de base de datos
    // ───────────────────────────────
    data class DatabaseError(
        override val message: String = "Error al acceder a la base de datos.",
        override val code: String? = "DB_ERROR",
        val details: String? = null
    ) : DashboardError(message)

    data class NoDataFound(
        override val message: String = "No hay datos disponibles para el período seleccionado.",
        override val code: String? = "NO_DATA"
    ) : DashboardError(message)

    // ───────────────────────────────
    // Errores de validación
    // ───────────────────────────────
    data class InvalidDateRange(
        override val message: String = "El rango de fechas no es válido.",
        override val code: String? = "INVALID_DATE_RANGE"
    ) : DashboardError(message)

    // ───────────────────────────────
    // Errores de autorización
    // ───────────────────────────────
    data class UnauthorizedError(
        override val message: String = "No tienes permisos para ver esta información.",
        override val code: String? = "UNAUTHORIZED"
    ) : DashboardError(message)

    // ───────────────────────────────
    // Errores generales
    // ───────────────────────────────
    data class UnknownError(
        override val message: String = "Ocurrió un error inesperado.",
        override val code: String? = "UNKNOWN_ERROR",
        val exception: Throwable? = null
    ) : DashboardError(message, exception)

    data class ServerError(
        override val message: String = "Error del servidor. Intenta más tarde.",
        override val code: String? = "SERVER_ERROR",
        val statusCode: Int? = null
    ) : DashboardError(message)
}

/**
 * Extensiones útiles para DashboardError
 */
fun DashboardError.isRetryable(): Boolean {
    return when (this) {
        is DashboardError.NetworkError,
        is DashboardError.TimeoutError,
        is DashboardError.ServerError -> true
        else -> false
    }
}

fun DashboardError.getUserFriendlyMessage(): String {
    return when (this) {
        is DashboardError.ServerError -> {
            statusCode?.let { "$message (Código: $it)" } ?: message
        }
        is DashboardError.DatabaseError -> {
            details?.let { "$message - $it" } ?: message
        }
        is DashboardError.UnknownError -> {
            exception?.message?.let { "$message: $it" } ?: message
        }
        else -> message
    }
}