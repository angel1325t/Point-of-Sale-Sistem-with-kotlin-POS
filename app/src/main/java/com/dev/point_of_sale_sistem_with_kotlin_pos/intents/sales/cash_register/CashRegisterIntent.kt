package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.cash_register

sealed class CashRegisterIntent {
    object LoadCurrentCashRegister : CashRegisterIntent()
    object LoadAllCashRegisters : CashRegisterIntent()
    object LoadCashRegisterHistory : CashRegisterIntent()

    data class OpenCashRegister(val cashRegisterId: String, val initialBalance: Double) : CashRegisterIntent()

    data class CloseCashRegister(val finalBalance: Double) : CashRegisterIntent()

    data class CreateCashRegister(val name: String) : CashRegisterIntent()
    data class UpdateCashRegister(val cashRegisterId: String, val newName: String) : CashRegisterIntent()
    data class DeleteCashRegister(val cashRegisterId: String) : CashRegisterIntent()
    object RefreshAllData : CashRegisterIntent()

    object ClearSuccess : CashRegisterIntent()
    object ClearError : CashRegisterIntent()
}

