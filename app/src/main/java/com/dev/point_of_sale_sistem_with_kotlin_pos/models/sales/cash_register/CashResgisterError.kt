package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register


sealed class CashRegisterError {
    data class NetworkError(val message: String) : CashRegisterError()
    data class ValidationError(val message: String) : CashRegisterError()
    data class DatabaseError(val message: String) : CashRegisterError()
    object UnauthorizedError : CashRegisterError()
    object CashRegisterNotFound : CashRegisterError()
    data class UnknownError(val message: String) : CashRegisterError()
}
