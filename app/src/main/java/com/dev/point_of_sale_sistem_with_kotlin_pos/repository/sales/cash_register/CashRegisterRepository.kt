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
    suspend fun closeCashRegister(finalBalance: Double? = null): CashRegisterHistory {
        val authId = getCurrentUserId() ?: throw Exception("Usuario no autenticado")

        val nowUtc = Clock.System.now()  // fecha y hora actual en UTC

        val data = buildJsonObject {
            if (finalBalance != null) put("final_balance", finalBalance)
            put("is_open", false)
            put("closing_date", nowUtc.toString()) // <-- agregamos closing_date
        }

        // Actualizamos la fila abierta
        supabase.postgrest.from("cash_registers_history")
            .update(data) {
                filter {
                    eq("auth_id", authId)
                    eq("is_open", true)
                }
            }

        // Recuperamos la última fila cerrada
        return supabase.postgrest.from("cash_registers_history")
            .select {
                filter {
                    eq("auth_id", authId)
                    eq("is_open", false)
                }
                order("closing_date", Order.DESCENDING)
                limit(1)
            }
            .decodeSingle()
    }

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

    suspend fun getCashRegisterHistory(): List<CashRegisterHistory> {
        val authId = getCurrentUserId() ?: return emptyList()

        return supabase.postgrest.from("cash_registers_history")
            .select {
                filter { eq("auth_id", authId) }
                order("opening_date", Order.DESCENDING)
            }
            .decodeList()
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
