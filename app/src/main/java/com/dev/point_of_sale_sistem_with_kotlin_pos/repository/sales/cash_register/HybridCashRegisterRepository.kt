package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register

import android.content.Context
import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.utils.NetworkUtils
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineCashRegisterHistoryEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.SyncQueueEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.SyncStatus
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterHistory
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

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
    // APERTURA
    // ----------------------------------------------------

    suspend fun openCashRegister(
        cashRegisterId: String,
        initialBalance: Double
    ): Result<CashRegisterHistory> {

        val branchId = onlineRepository
            .let { it }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }
            .let { onlineRepository }
            .run { onlineRepository }

        val historyId = UUID.randomUUID().toString()
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        val authId = getCurrentUserId() ?: "unknown"

        val entity = OfflineCashRegisterHistoryEntity(
            localHistoryId = historyId,
            cashRegisterId = cashRegisterId,
            branchId = branchId.toString(),
            authId = authId,
            openingDate = now,
            initialBalance = initialBalance,
            isOpen = true,
            syncStatus = SyncStatus.PENDING
        )

        offlineDb.cashRegisterDao().insert(entity)

        return if (NetworkUtils.isOnline(context)) {
            try {
                val online = onlineRepository.openCashRegister(cashRegisterId, initialBalance)
                val synced = entity.copy(
                    localHistoryId = online.history_id,
                    syncStatus = SyncStatus.SYNCED
                )
                offlineDb.cashRegisterDao().update(synced)
                Result.success(synced.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Error apertura online", e)
                Result.success(entity.toDomain())
            }
        } else {
            offlineDb.syncQueueDao().insert(
                SyncQueueEntity(
                    entityType = "CASH_REGISTER",
                    entityId = historyId
                )

            )
            Result.success(entity.toDomain())
        }
    }

    // ----------------------------------------------------
    // CIERRE
    // ----------------------------------------------------

    suspend fun closeCashRegister(
        realFinalBalance: Double
    ): Result<CashRegisterHistory> {

        val openRegister = offlineDb.cashRegisterDao()
            .getOpenCashRegister()
            ?: return Result.failure(Exception("No hay caja abierta"))

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        val salesTotal = getLocalSalesTotal(openRegister.localHistoryId)
        val expected = openRegister.initialBalance + salesTotal
        val difference = realFinalBalance - expected

        val updated = openRegister.copy(
            closingDate = now,
            finalBalance = realFinalBalance,
            expectedBalance = expected,
            difference = difference,
            isOpen = false,
            syncStatus = SyncStatus.PENDING
        )

        offlineDb.cashRegisterDao().update(updated)

        return if (NetworkUtils.isOnline(context)) {
            try {
                val online = onlineRepository.closeCashRegister(realFinalBalance)
                val synced = updated.copy(
                    localHistoryId = online.history_id,
                    syncStatus = SyncStatus.SYNCED
                )
                offlineDb.cashRegisterDao().update(synced)
                Result.success(synced.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Error cierre online", e)
                Result.success(updated.toDomain())
            }
        } else {
            offlineDb.syncQueueDao().insert(
                        SyncQueueEntity(
                        entityType = "CASH_REGISTER",
                entityId = updated.localHistoryId
            )

            )
            Result.success(updated.toDomain())
        }
    }

    // ----------------------------------------------------
    // CONSULTA
    // ----------------------------------------------------

    suspend fun getOpenCashRegister(): CashRegisterHistory? {
        return offlineDb.cashRegisterDao()
            .getOpenCashRegister()
            ?.toDomain()
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
        is_open = isOpen
    )
