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

    suspend fun openCashRegister(
        cashRegisterId: String,
        initialBalance: Double
    ): Result<CashRegisterHistory> {
        val historyId = UUID.randomUUID().toString()
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        val authId = getCurrentUserId() ?: "unknown"

        val entity = OfflineCashRegisterHistoryEntity(
            localHistoryId = historyId,
            cashRegisterId = cashRegisterId,
            authId = authId,
            openingDate = now,
            initialBalance = initialBalance,
            isOpen = true,
            syncStatus = SyncStatus.PENDING
        )

        offlineDb.cashRegisterDao().insert(entity)

        return if (NetworkUtils.isOnline(context)) {
            try {
                val onlineHistory = onlineRepository.openCashRegister(cashRegisterId, initialBalance)
                val updated = entity.copy(
                    localHistoryId = onlineHistory.history_id,
                    syncStatus = SyncStatus.SYNCED
                )
                offlineDb.cashRegisterDao().update(updated)
                Result.success(updated.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Error opening cash register online", e)
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

    suspend fun closeCashRegister(
        realFinalBalance: Double
    ): Result<CashRegisterHistory> {
        val openRegister = offlineDb.cashRegisterDao().getOpenCashRegister()
            ?: return Result.failure(IllegalStateException("No hay caja abierta"))

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        val salesTotal = getLocalSalesTotal(openRegister.localHistoryId)
        val expectedBalance = openRegister.initialBalance + salesTotal
        val difference = realFinalBalance - expectedBalance

        val updated = openRegister.copy(
            closingDate = now,
            finalBalance = realFinalBalance,
            expectedBalance = expectedBalance,
            difference = difference,
            isOpen = false,
            syncStatus = SyncStatus.PENDING
        )

        offlineDb.cashRegisterDao().update(updated)

        return if (NetworkUtils.isOnline(context)) {
            try {
                val onlineHistory = onlineRepository.closeCashRegister(realFinalBalance)
                val synced = updated.copy(
                    localHistoryId = onlineHistory.history_id,
                    syncStatus = SyncStatus.SYNCED
                )
                offlineDb.cashRegisterDao().update(synced)
                Result.success(synced.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Error closing cash register online", e)
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

    suspend fun getOpenCashRegister(): CashRegisterHistory? {
        val local = offlineDb.cashRegisterDao().getOpenCashRegister()

        // If there's a local box, check if it needs to be synced
        if (local != null) {
            // If local box is pending sync and we're online, try to sync
            if (local.syncStatus == SyncStatus.PENDING && NetworkUtils.isOnline(context)) {
                try {
                    // Check if there's already an open box on the server
                    val existingOnlineBox = onlineRepository.getOpenCashRegister()
                    if (existingOnlineBox != null) {
                        // Use server box instead of local pending one
                        val synced = local.copy(
                            localHistoryId = existingOnlineBox.history_id,
                            syncStatus = SyncStatus.SYNCED
                        )
                        offlineDb.cashRegisterDao().update(synced)
                        Log.d(TAG, "Using existing server box: ${existingOnlineBox.history_id}")
                        return synced.toDomain()
                    }

                    // No existing box, create new one
                    val onlineHistory = onlineRepository.openCashRegister(
                        local.cashRegisterId,
                        local.initialBalance
                    )
                    val synced = local.copy(
                        localHistoryId = onlineHistory.history_id,
                        syncStatus = SyncStatus.SYNCED
                    )
                    offlineDb.cashRegisterDao().update(synced)
                    Log.d(TAG, "Pending box synced: ${onlineHistory.history_id}")
                    return synced.toDomain()
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing local box: ${e.message}")
                    // If error and server has an open box, use it
                    if (e.message?.contains("Ya tienes una caja abierta") == true) {
                        try {
                            val existingBox = onlineRepository.getOpenCashRegister()
                            if (existingBox != null) {
                                val synced = local.copy(
                                    localHistoryId = existingBox.history_id,
                                    syncStatus = SyncStatus.SYNCED
                                )
                                offlineDb.cashRegisterDao().update(synced)
                                Log.d(TAG, "Using server box after sync error: ${existingBox.history_id}")
                                return synced.toDomain()
                            }
                        } catch (e2: Exception) {
                            Log.e(TAG, "Error getting existing box: ${e2.message}")
                        }
                    }
                    // If we can't sync, use local box anyway for offline functionality
                    return local.toDomain()
                }
            }
            // Local box is already synced or we're offline - use it
            return local.toDomain()
        }

        // No local box, try server
        return if (NetworkUtils.isOnline(context)) {
            try {
                onlineRepository.getOpenCashRegister()?.also { onlineHistory ->
                    val entity = OfflineCashRegisterHistoryEntity(
                        localHistoryId = onlineHistory.history_id,
                        cashRegisterId = onlineHistory.cash_register_id,
                        authId = onlineHistory.auth_id,
                        openingDate = onlineHistory.opening_date,
                        closingDate = onlineHistory.closing_date,
                        initialBalance = onlineHistory.initial_balance,
                        finalBalance = onlineHistory.final_balance,
                        expectedBalance = onlineHistory.expected_balance,
                        difference = onlineHistory.difference,
                        isOpen = onlineHistory.is_open,
                        syncStatus = SyncStatus.SYNCED
                    )
                    offlineDb.cashRegisterDao().insert(entity)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting open cash register from online", e)
                null
            }
        } else {
            null
        }
    }

    suspend fun syncPendingBoxIfNeeded() {
        // This is now handled in getOpenCashRegister()
        // Kept for compatibility but does nothing
    }

    private suspend fun getLocalSalesTotal(historyId: String): Double {
        return try {
            offlineDb.salesDao().getSaleByHistoryId(historyId)
                ?.let { sale -> sale.total }
                ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}

private fun OfflineCashRegisterHistoryEntity.toDomain() = CashRegisterHistory(
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
