package com.dev.point_of_sale_sistem_with_kotlin_pos.core.sync

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.relations.OfflineSaleWithDetails
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.mappers.toDomainSale
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(
    private val offlineDb: OfflineDatabase,
    private val salesRepository: SalesRepository,
    private val cashRegisterRepository: CashRegisterRepository
) {

    companion object {
        private const val TAG = "SyncManager"
    }

    suspend fun syncAll() = withContext(Dispatchers.IO) {

        val queue = offlineDb.syncQueueDao().getAll()
        Log.d(TAG, "Sync queue size: ${queue.size}")

        queue.forEach { item ->
            try {
                when (item.entityType) {

                    "SALE" -> syncSale(item.entityId)

                    "CASH_REGISTER" -> syncCashRegister(item.entityId)
                }

                // ✅ solo se borra si todo salió bien
                offlineDb.syncQueueDao().deleteById(item.id)

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing ${item.entityType}", e)
            }
        }
    }

    // -------------------------
    // 🛒 VENTAS
    // -------------------------
    private suspend fun syncSale(localSaleId: String) {

        val sale = offlineDb.salesDao().getSaleById(localSaleId)
            ?: throw IllegalStateException("Offline sale not found")

        val details = offlineDb.salesDao().getDetailsBySaleId(localSaleId)

        val domainSale = OfflineSaleWithDetails(sale, details).toDomainSale()

        salesRepository.createSale(domainSale)

        Log.d(TAG, "Sale synced: $localSaleId")
    }


    // -------------------------
    // 🧾 CAJA (APERTURA / CIERRE)
    // -------------------------
    private suspend fun syncCashRegister(localHistoryId: String) {

        val history = offlineDb.cashRegisterDao()
            .getById(localHistoryId)
            ?: throw IllegalStateException("Offline cash register history not found")

        if (history.isOpen) {
            // 🔓 APERTURA
            cashRegisterRepository.openCashRegister(
                cashRegisterId = history.cashRegisterId,
                initialBalance = history.initialBalance
            )
        } else {
            // 🔒 CIERRE
            cashRegisterRepository.closeCashRegister(
                realFinalBalance = history.finalBalance
                    ?: throw IllegalStateException("Final balance missing")
            )
        }

        Log.d(TAG, "Cash register synced: $localHistoryId")
    }
}
