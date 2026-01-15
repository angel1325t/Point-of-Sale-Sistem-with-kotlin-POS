package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register

import android.content.Context
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.utils.NetworkUtils
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineCashRegisterHistoryEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.SyncQueueEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.SyncStatus
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterHistory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Repositorio híbrido para operaciones de caja
 */
class HybridCashRegisterRepository(
    private val context: Context,
    private val onlineRepository: CashRegisterRepository
) {

    private val offlineDb = OfflineDatabase.getInstance(context)

    /**
     * Abre una caja (online u offline)
     */
    suspend fun openCashRegister(
        cashRegisterId: String,
        initialBalance: Double
    ): Result<CashRegisterHistory> {
        return if (NetworkUtils.isOnline(context)) {
            // 🌐 ONLINE
            runCatching {
                onlineRepository.openCashRegister(cashRegisterId, initialBalance)
            }
        } else {
            // 📴 OFFLINE
            openCashRegisterOffline(cashRegisterId, initialBalance)
        }
    }

    /**
     * Cierra una caja (online u offline)
     */
    suspend fun closeCashRegister(
        realFinalBalance: Double
    ): Result<CashRegisterHistory> {
        return if (NetworkUtils.isOnline(context)) {
            // 🌐 ONLINE
            runCatching {
                onlineRepository.closeCashRegister(realFinalBalance)
            }
        } else {
            // 📴 OFFLINE
            closeCashRegisterOffline(realFinalBalance)
        }
    }

    /**
     * Obtiene la caja abierta actual
     */
    suspend fun getOpenCashRegister(): CashRegisterHistory? {
        return if (NetworkUtils.isOnline(context)) {
            // 🌐 ONLINE
            onlineRepository.getOpenCashRegister()
        } else {
            // 📴 OFFLINE
            offlineDb.cashRegisterDao().getOpenCashRegister()?.toDomain()
        }
    }

    // ════════════════════════════════════════════════════
    // MÉTODOS OFFLINE
    // ════════════════════════════════════════════════════

    private suspend fun openCashRegisterOffline(
        cashRegisterId: String,
        initialBalance: Double
    ): Result<CashRegisterHistory> = runCatching {

        val historyId = UUID.randomUUID().toString()
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)

        val entity = OfflineCashRegisterHistoryEntity(
            localHistoryId = historyId,
            cashRegisterId = cashRegisterId,
            authId = "offline-user", // TODO: obtener del SessionPreferences
            openingDate = now,
            initialBalance = initialBalance,
            isOpen = true,
            syncStatus = SyncStatus.PENDING
        )

        offlineDb.cashRegisterDao().insert(entity)

        // Encolar para sincronización
        offlineDb.syncQueueDao().insert(
            SyncQueueEntity(
                entityType = "CASH_REGISTER",
                entityId = historyId
            )
        )

        entity.toDomain()
    }

    private suspend fun closeCashRegisterOffline(
        realFinalBalance: Double
    ): Result<CashRegisterHistory> = runCatching {

        val openRegister = offlineDb.cashRegisterDao().getOpenCashRegister()
            ?: throw IllegalStateException("No hay caja abierta")

        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)

        // TODO: Calcular expected balance desde ventas locales
        val expectedBalance = realFinalBalance // Simplificado
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

        // Encolar para sincronización
        offlineDb.syncQueueDao().insert(
            SyncQueueEntity(
                entityType = "CASH_REGISTER",
                entityId = updated.localHistoryId
            )
        )

        updated.toDomain()
    }
}

// ════════════════════════════════════════════════════
// MAPPER
// ════════════════════════════════════════════════════

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