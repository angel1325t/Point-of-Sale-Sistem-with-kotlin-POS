package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register

import android.content.Context
import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.utils.NetworkUtils
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineCashRegisterHistoryEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.SyncStatus
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterHistory
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HybridCashRegisterRepository(
    private val context: Context,
    private val onlineRepository: CashRegisterRepository,
    private val supabase: SupabaseClient
) {

    companion object {
        private const val TAG = "HybridCashRegisterRepo"
    }

    private val offlineDb = OfflineDatabase.getInstance(context)

    private suspend fun getCurrentUserId(): String? =
        supabase.auth.currentUserOrNull()?.id

    // ----------------------------------------------------
    // APERTURA - ALWAYS CREATES IN SUPABASE FIRST
    // ----------------------------------------------------

    suspend fun openCashRegister(
        cashRegisterId: String,
        initialBalance: Double
    ): Result<CashRegisterHistory> = runCatching {

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        val authId = getCurrentUserId() ?: "unknown"

        // Check if there's already an open register (locally)
        val existingOpen = offlineDb.cashRegisterDao().getOpenCashRegister()
        if (existingOpen != null) {
            throw Exception("Ya existe una caja abierta: ${existingOpen.localHistoryId}")
        }

        if (!NetworkUtils.isOnline(context)) {
            throw Exception("No se puede abrir caja sin conexión a internet. Las cajas deben abrirse en línea para evitar conflictos de sincronización.")
        }

        // ALWAYS CREATE IN SUPABASE FIRST
        Log.d(TAG, "Opening cash register in Supabase...")
        val onlineHistory = onlineRepository.openCashRegister(cashRegisterId, initialBalance)

        // Save to local DB with the SERVER-GENERATED ID
        val entity = OfflineCashRegisterHistoryEntity(
            localHistoryId = onlineHistory.history_id, // ✅ USE SERVER ID
            cashRegisterId = cashRegisterId,
            branchId = onlineHistory.branch_id,
            authId = authId,
            openingDate = onlineHistory.opening_date,
            initialBalance = initialBalance,
            isOpen = true,
            syncStatus = SyncStatus.SYNCED
        )

        offlineDb.cashRegisterDao().insert(entity)
        Log.d(TAG, "✅ Cash register opened: ${onlineHistory.history_id}")

        onlineHistory
    }

    // ----------------------------------------------------
    // CIERRE - UPDATES SUPABASE THEN LOCAL
    // ----------------------------------------------------

    suspend fun closeCashRegister(
        realFinalBalance: Double
    ): Result<CashRegisterHistory> = runCatching {

        val openRegister = offlineDb.cashRegisterDao().getOpenCashRegister()
            ?: throw Exception("No hay caja abierta")

        if (!NetworkUtils.isOnline(context)) {
            throw Exception("No se puede cerrar caja sin conexión a internet")
        }

        Log.d(TAG, "Closing cash register in Supabase...")
        val onlineHistory = onlineRepository.closeCashRegister(realFinalBalance)

        // Update local record
        val updated = openRegister.copy(
            closingDate = onlineHistory.closing_date,
            finalBalance = onlineHistory.final_balance,
            expectedBalance = onlineHistory.expected_balance,
            difference = onlineHistory.difference,
            isOpen = false,
            syncStatus = SyncStatus.SYNCED
        )

        offlineDb.cashRegisterDao().update(updated)
        Log.d(TAG, "✅ Cash register closed: ${onlineHistory.history_id}")

        onlineHistory
    }

    // ----------------------------------------------------
    // CONSULTA - CHECKS BOTH LOCAL AND ONLINE
    // ----------------------------------------------------

    suspend fun getOpenCashRegister(): CashRegisterHistory? {
        val isOnline = NetworkUtils.isOnline(context)

        if (isOnline) {
            // When online, check Supabase for the source of truth
            return try {
                val onlineRegister = onlineRepository.getOpenCashRegister()

                if (onlineRegister != null) {
                    // Sync to local DB if not already there
                    val localRegister = offlineDb.cashRegisterDao().getOpenCashRegister()

                    if (localRegister == null || localRegister.localHistoryId != onlineRegister.history_id) {
                        Log.d(TAG, "Syncing online register to local DB: ${onlineRegister.history_id}")

                        // Close any stale local registers
                        localRegister?.let {
                            offlineDb.cashRegisterDao().update(it.copy(isOpen = false))
                        }

                        // Insert the online register
                        val entity = OfflineCashRegisterHistoryEntity(
                            localHistoryId = onlineRegister.history_id,
                            cashRegisterId = onlineRegister.cash_register_id,
                            branchId = onlineRegister.branch_id,
                            authId = onlineRegister.auth_id,
                            openingDate = onlineRegister.opening_date,
                            initialBalance = onlineRegister.initial_balance,
                            isOpen = true,
                            syncStatus = SyncStatus.SYNCED
                        )
                        offlineDb.cashRegisterDao().insert(entity)
                    }
                }

                onlineRegister
            } catch (e: Exception) {
                Log.e(TAG, "Error checking online register, falling back to local", e)
                offlineDb.cashRegisterDao().getOpenCashRegister()?.toDomain()
            }
        } else {
            // Offline: use local DB
            return offlineDb.cashRegisterDao().getOpenCashRegister()?.toDomain()
        }
    }

    private suspend fun getLocalSalesTotal(historyId: String): Double {
        return offlineDb.salesDao()
            .getSaleByHistoryId(historyId)
            ?.total ?: 0.0
    }
}

// ----------------------------------------------------
// MAPPER
// ----------------------------------------------------

private fun OfflineCashRegisterHistoryEntity.toDomain() =
    CashRegisterHistory(
        history_id = localHistoryId,
        cash_register_id = cashRegisterId,
        auth_id = authId,
        opening_date = openingDate,
        closing_date = closingDate,
        initial_balance = initialBalance,
        final_balance = finalBalance,
        expected_balance = expectedBalance,
        difference = difference,
        is_open = isOpen,
        branch_id = branchId,
    )