package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register

data class CashRegisterState(
    val isLoading: Boolean = false,
    val error: CashRegisterError? = null,
    val success: Boolean = false,
    val currentCashRegister: CashRegisterHistory? = null,
    val allCashRegisters: List<CashRegister>? = null,
    val cashRegisterHistory: List<CashRegisterHistory> = emptyList()
)
