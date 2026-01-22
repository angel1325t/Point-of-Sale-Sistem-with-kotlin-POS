package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class CashRegister(
    @SerialName("cash_register_id")
    val cash_register_id: String,

    @SerialName("name")
    val name: String,

    @SerialName("branch_id")
    val branch_id: String,

    @SerialName("created_at")
    val created_at: String? = null
)

@Serializable
data class CashRegisterHistory(
    @SerialName("history_id")
    val history_id: String,

    @SerialName("cash_register_id")
    val cash_register_id: String,

    @SerialName("auth_id")
    val auth_id: String,

    @SerialName("opening_date")
    val opening_date: String,

    @SerialName("closing_date")
    val closing_date: String? = null,

    @SerialName("initial_balance")
    val initial_balance: Double,

    @SerialName("final_balance")
    val final_balance: Double? = null,

    // 🆕 NUEVOS CAMPOS PARA CONTROL DE CAJA
    @SerialName("expected_balance")
    val expected_balance: Double? = null,

    @SerialName("difference")
    val difference: Double? = null,

    @SerialName("is_open")
    val is_open: Boolean = false,

    @SerialName("created_at")
    val created_at: String? = null,
    @SerialName("branch_id")
    val branch_id: String,
) {
    // ════════════════════════════════════════════════════
    // HELPERS PARA FORMATEAR FECHAS
    // ════════════════════════════════════════════════════

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedOpeningDate(): String {
        return try {
            val instant = java.time.Instant.parse(opening_date)
            val dateTime = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        } catch (e: Exception) {
            opening_date
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedClosingDate(): String? {
        return closing_date?.let {
            try {
                val instant = java.time.Instant.parse(it)
                val dateTime = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            } catch (e: Exception) {
                it
            }
        }
    }

    // ════════════════════════════════════════════════════
    // HELPERS PARA ANÁLISIS DE DIFERENCIAS
    // ════════════════════════════════════════════════════

    /**
     * Indica si hay diferencia entre el saldo esperado y el real
     */
    val hasDifference: Boolean
        get() = difference != null && difference != 0.0

    /**
     * Indica si la diferencia es positiva (sobrante)
     */
    val isDifferencePositive: Boolean
        get() = difference != null && difference > 0.0

    /**
     * Indica si la diferencia es negativa (faltante)
     */
    val isDifferenceNegative: Boolean
        get() = difference != null && difference < 0.0

    /**
     * Mensaje descriptivo de la diferencia
     */
    fun getDifferenceMessage(): String? {
        return when {
            difference == null -> null
            difference == 0.0 -> "Cierre correcto: sin diferencia"
            difference > 0.0 -> "Sobrante de RD$ %.2f".format(difference)
            else -> "Faltante de RD$ %.2f".format(kotlin.math.abs(difference))
        }
    }
}