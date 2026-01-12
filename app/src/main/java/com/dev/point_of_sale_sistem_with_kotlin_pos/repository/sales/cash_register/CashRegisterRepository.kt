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

class CashRegisterRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TAG = "CashRegisterRepository"
    }

    private suspend fun getCurrentUserId(): String? =
        supabase.auth.currentUserOrNull()?.id

    // ----------------------------------------------------
    // CRUD PARA CAJAS (cash_registers)
    // ----------------------------------------------------

    suspend fun createCashRegister(name: String): CashRegister {
        val branchId = getCurrentUserBranchId()
            ?: throw Exception("Usuario sin sucursal asignada")

        val data = buildJsonObject {
            put("name", name)
            put("branch_id", branchId.toString())
        }

        val response = supabase.postgrest.from("cash_registers")
            .insert(data) { select() }

        return response.decodeSingle()
    }

    suspend fun getAllCashRegisters(): List<CashRegister> {
        val branchId = getCurrentUserBranchId() ?: return emptyList()

        return supabase.postgrest.from("cash_registers")
            .select {
                filter {
                    eq("branch_id", branchId.toString())
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun updateCashRegister(cashRegisterId: String, newName: String): CashRegister {
        Log.d(TAG, "Updating cash register: cashRegisterId=$cashRegisterId")

        val data = buildJsonObject {
            put("name", newName)
        }

        supabase.postgrest.from("cash_registers")
            .update(data) {
                filter {
                    eq("cash_register_id", cashRegisterId)
                }
            }

        return supabase.postgrest.from("cash_registers")
            .select {
                filter {
                    eq("cash_register_id", cashRegisterId)
                }
            }
            .decodeSingle()
    }

    suspend fun deleteCashRegister(cashRegisterId: String): Boolean {
        return try {
            supabase.postgrest.from("cash_registers")
                .delete {
                    filter { eq("cash_register_id", cashRegisterId) }
                }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando caja", e)
            false
        }
    }

    // ----------------------------------------------------
    // APERTURA Y CIERRE DE CAJA
    // ----------------------------------------------------

    suspend fun openCashRegister(cashRegisterId: String, initialBalance: Double): CashRegisterHistory {
        val authId = getCurrentUserId()
            .also { Log.d(TAG, "Usuario actual authId: $it") }
            ?: throw Exception("Usuario no autenticado")

        val open = getOpenCashRegister()
        if (open != null) throw Exception("Ya tienes una caja abierta")

        Log.d(TAG, "Insertando cash_registers_history: cashRegisterId=$cashRegisterId, initialBalance=$initialBalance")

        val data = buildJsonObject {
            put("cash_register_id", cashRegisterId)
            put("auth_id", authId)
            put("initial_balance", initialBalance)
            put("is_open", true)
        }

        val response = supabase.postgrest.from("cash_registers_history")
            .insert(data) { select() }

        return response.decodeList<CashRegisterHistory>().first()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun closeCashRegister(realFinalBalance: Double): CashRegisterHistory {
        val authId = getCurrentUserId() ?: throw Exception("Usuario no autenticado")

        val openRegister = getOpenCashRegister()
            ?: throw Exception("No hay caja abierta para cerrar")

        val historyId = openRegister.history_id

        // 📊 CALCULAR VENTAS EN EFECTIVO
        val totalCashSales = getTotalCashSales(historyId)

        // 💰 CÁLCULOS
        val expectedBalance = openRegister.initial_balance + totalCashSales
        val difference = realFinalBalance - expectedBalance

        Log.d(TAG, """
            Cierre de caja:
            - ID: $historyId
            - Saldo inicial: ${openRegister.initial_balance}
            - Ventas en efectivo: $totalCashSales
            - Saldo esperado: $expectedBalance
            - Saldo real: $realFinalBalance
            - Diferencia: $difference
        """.trimIndent())

        val nowUtc = Clock.System.now()

        val data = buildJsonObject {
            put("final_balance", realFinalBalance)
            put("expected_balance", expectedBalance)
            put("difference", difference)
            put("is_open", false)
            put("closing_date", nowUtc.toString())
        }

        // Actualizar el registro
        supabase.postgrest.from("cash_registers_history")
            .update(data) {
                filter {
                    eq("history_id", historyId)
                }
            }

        // Recuperar el registro actualizado
        val closedRegister = supabase.postgrest.from("cash_registers_history")
            .select {
                filter {
                    eq("history_id", historyId)
                }
            }
            .decodeSingle<CashRegisterHistory>()

        Log.d(TAG, "Caja cerrada exitosamente: $historyId")

        return closedRegister
    }

    // ----------------------------------------------------
    // 💵 CÁLCULO DE VENTAS EN EFECTIVO
    // ----------------------------------------------------

    private suspend fun getTotalCashSales(historyId: String): Double {
        return try {
            // Consulta para sumar todas las ventas en efectivo de esta apertura
            val result = supabase.postgrest.from("sales")
                .select {
                    filter {
                        eq("cash_register_history_id", historyId)
                        eq("payment_method", "cash")
                        eq("status", "completed")
                    }
                }
                .decodeList<SaleSummary>()

            val total = result.sumOf { it.total }
            Log.d(TAG, "Total ventas en efectivo: $total (${result.size} ventas)")
            total
        } catch (e: Exception) {
            Log.e(TAG, "Error calculando ventas", e)
            0.0
        }
    }

    // Clase auxiliar para deserializar solo el campo 'total'
    @kotlinx.serialization.Serializable
    private data class SaleSummary(val total: Double)

    // ----------------------------------------------------
    // CONSULTAS DE ESTADO
    // ----------------------------------------------------

    suspend fun getOpenCashRegister(): CashRegisterHistory? {
        val authId = getCurrentUserId() ?: return null

        return supabase.postgrest.from("cash_registers_history")
            .select {
                filter {
                    eq("auth_id", authId)
                    eq("is_open", true)
                }
                order("opening_date", Order.DESCENDING)
                limit(1)
            }
            .decodeList<CashRegisterHistory>()
            .firstOrNull()
    }

    suspend fun getCashRegisterHistory(limit: Int, offset: Int): List<CashRegisterHistory> {
        val authId = getCurrentUserId() ?: return emptyList()

        return supabase.postgrest["cash_registers_history"]
            .select {
                filter {
                    eq("auth_id", authId)
                }
                order("opening_date", Order.DESCENDING)
                limit(limit.toLong())
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<CashRegisterHistory>()
    }

    // ----------------------------------------------------
    // UTILIDADES
    // ----------------------------------------------------

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
}