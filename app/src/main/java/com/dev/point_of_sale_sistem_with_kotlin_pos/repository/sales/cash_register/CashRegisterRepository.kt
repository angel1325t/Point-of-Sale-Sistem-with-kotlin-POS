package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegister
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterHistory
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CashRegisterRepository(
    private val supabase: SupabaseClient
) {

    companion object {
        private const val TAG = "CashRegisterRepository"
    }

    private suspend fun getCurrentUserId(): String? =
        supabase.auth.currentUserOrNull()?.id

    private suspend fun getCurrentUserBranchId(): UUID? {
        val authId = getCurrentUserId() ?: return null
        return try {
            supabase.postgrest.from("users")
                .select { filter { eq("auth_id", authId) } }
                .decodeSingleOrNull<UserRepository.UserModel>()
                ?.branch_id
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo branch_id", e)
            null
        }
    }

    // ----------------------------------------------------
    // CRUD CASH REGISTERS
    // ----------------------------------------------------

    suspend fun createCashRegister(name: String): CashRegister {
        val branchId = getCurrentUserBranchId()
            ?: throw Exception("Usuario sin sucursal asignada")

        val data = buildJsonObject {
            put("name", name)
            put("branch_id", branchId.toString())
        }

        return supabase.postgrest.from("cash_registers")
            .insert(data) { select() }
            .decodeSingle()
    }

    suspend fun getAllCashRegisters(): List<CashRegister> {
        val branchId = getCurrentUserBranchId() ?: return emptyList()

        return supabase.postgrest.from("cash_registers")
            .select {
                filter { eq("branch_id", branchId.toString()) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun updateCashRegister(
        cashRegisterId: String,
        newName: String
    ): CashRegister {

        val data = buildJsonObject {
            put("name", newName)
        }

        supabase.postgrest.from("cash_registers")
            .update(data) {
                filter { eq("cash_register_id", cashRegisterId) }
            }

        return supabase.postgrest.from("cash_registers")
            .select {
                filter { eq("cash_register_id", cashRegisterId) }
            }
            .decodeSingle()
    }

    suspend fun deleteCashRegister(cashRegisterId: String): Boolean {
        return try {
            supabase.postgrest.from("cash_registers")
                .delete { filter { eq("cash_register_id", cashRegisterId) } }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando caja", e)
            false
        }
    }

    // ----------------------------------------------------
    // APERTURA / CIERRE
    // ----------------------------------------------------

    suspend fun openCashRegister(
        cashRegisterId: String,
        initialBalance: Double
    ): CashRegisterHistory {

        val authId = getCurrentUserId()
            ?: throw Exception("Usuario no autenticado")

        val branchId = getCurrentUserBranchId()
            ?: throw Exception("Usuario sin sucursal")

        if (getOpenCashRegister() != null) {
            throw Exception("Ya tienes una caja abierta")
        }

        val data = buildJsonObject {
            put("cash_register_id", cashRegisterId)
            put("auth_id", authId)
            put("branch_id", branchId.toString())
            put("initial_balance", initialBalance)
            put("is_open", true)
        }

        return supabase.postgrest.from("cash_registers_history")
            .insert(data) { select() }
            .decodeSingle()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun closeCashRegister(realFinalBalance: Double): CashRegisterHistory {
        val openRegister = getOpenCashRegister()
            ?: throw Exception("No hay caja abierta")

        val historyId = openRegister.history_id
        val totalCashSales = getTotalCashSales(historyId)

        val expectedBalance = openRegister.initial_balance + totalCashSales
        val difference = realFinalBalance - expectedBalance
        val nowUtc = Clock.System.now()

        val data = buildJsonObject {
            put("final_balance", realFinalBalance)
            put("expected_balance", expectedBalance)
            put("difference", difference)
            put("is_open", false)
            put("closing_date", nowUtc.toString())
        }

        supabase.postgrest.from("cash_registers_history")
            .update(data) {
                filter { eq("history_id", historyId) }
            }

        return supabase.postgrest.from("cash_registers_history")
            .select { filter { eq("history_id", historyId) } }
            .decodeSingle()
    }

    // ----------------------------------------------------
    // CONSULTAS
    // ----------------------------------------------------

    suspend fun getOpenCashRegister(): CashRegisterHistory? {
        val authId = getCurrentUserId() ?: return null
        val branchId = getCurrentUserBranchId() ?: return null

        return supabase.postgrest.from("cash_registers_history")
            .select {
                filter {
                    eq("auth_id", authId)
                    eq("branch_id", branchId.toString())
                    eq("is_open", true)
                }
                order("opening_date", Order.DESCENDING)
                limit(1)
            }
            .decodeList<CashRegisterHistory>()
            .firstOrNull()
    }

    suspend fun getCashRegisterHistory(
        limit: Int,
        offset: Int
    ): List<CashRegisterHistory> {

        val authId = getCurrentUserId() ?: return emptyList()
        val branchId = getCurrentUserBranchId() ?: return emptyList()

        return supabase.postgrest.from("cash_registers_history")
            .select {
                filter {
                    eq("auth_id", authId)
                    eq("branch_id", branchId.toString())
                }
                order("opening_date", Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList()
    }

    // ----------------------------------------------------
    // VENTAS EN EFECTIVO
    // ----------------------------------------------------

    private suspend fun getTotalCashSales(historyId: String): Double {
        return try {
            supabase.postgrest.from("sales")
                .select {
                    filter {
                        eq("cash_register_history_id", historyId)
                        eq("payment_method", "cash")
                        eq("status", "completed")
                    }
                }
                .decodeList<SaleSummary>()
                .sumOf { it.total }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculando ventas", e)
            0.0
        }
    }

    @kotlinx.serialization.Serializable
    private data class SaleSummary(val total: Double)
}
