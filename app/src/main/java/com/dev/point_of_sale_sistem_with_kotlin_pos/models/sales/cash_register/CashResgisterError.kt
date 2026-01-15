package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register

sealed class CashRegisterError {
    data class NetworkError(val message: String) : CashRegisterError()
    data class ValidationError(val message: String) : CashRegisterError()
    data class DatabaseError(val message: String) : CashRegisterError()
    object UnauthorizedError : CashRegisterError()
    object CashRegisterNotFound : CashRegisterError()
    object CashRegisterAlreadyOpen : CashRegisterError() // 🆕
    data class UnknownError(val message: String) : CashRegisterError()

    fun toUserMessage(): String = when (this) {
        is NetworkError -> "Error de conexión: $message"
        is ValidationError -> message
        is DatabaseError -> "Error de base de datos: $message"
        UnauthorizedError -> "No autorizado"
        CashRegisterNotFound -> "No se encontró la caja"
        CashRegisterAlreadyOpen -> "Ya tienes una caja abierta" // 🆕
        is UnknownError -> "Error: $message"
    }
}