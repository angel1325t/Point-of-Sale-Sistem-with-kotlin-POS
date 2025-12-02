package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CashRegister(
    val cash_register_id: String,
    val name: String,
    val branch_id: String,
    val created_at: String? = null
)

@Serializable
data class CashRegisterHistory(
    val history_id: String,
    val cash_register_id: String,
    val auth_id: String,
    val opening_date: String,
    val closing_date: String? = null,
    val initial_balance: Double,
    val final_balance: Double? = null,
    val created_at: String? = null
)
