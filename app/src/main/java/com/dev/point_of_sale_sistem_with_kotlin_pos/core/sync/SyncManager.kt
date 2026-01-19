package com.dev.point_of_sale_sistem_with_kotlin_pos.core.sync

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineProductCacheEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.SyncStatus
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.relations.OfflineSaleWithDetails
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.mappers.toDomainSale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterHistory
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.products.ProductRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(
    private val offlineDb: OfflineDatabase,
    private val salesRepository: SalesRepository,
    private val cashRegisterRepository: CashRegisterRepository,
    private val productRepository: ProductRepository,
    private val sessionPreferences: SessionPreferences
) {

    companion object {
        private const val TAG = "SyncManager"
    }

    suspend fun syncAll() = withContext(Dispatchers.IO) {
        syncProducts()
        syncPendingData()
    }

    private suspend fun syncProducts() {
        try {
            val branchId = sessionPreferences.getBranchId() ?: return

            val productsResult = productRepository.getProducts()
            productsResult.onSuccess { products ->
                val cacheEntities = products.map { product ->
                    OfflineProductCacheEntity(
                        productId = product.productId,
                        name = product.name,
                        barcode = product.barcode,
                        price = product.price,
                        currentStock = product.currentStock,
                        categoryId = product.categoryId,
                        branchId = branchId
                    )
                }
                offlineDb.productCacheDao().deleteByBranch(branchId)
                offlineDb.productCacheDao().insertAll(cacheEntities)
                Log.d(TAG, "Synced ${cacheEntities.size} products to offline cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing products", e)
        }
    }

    private suspend fun syncPendingData() {
        val queue = offlineDb.syncQueueDao().getAll()
        Log.d(TAG, "syncPendingData: Found ${queue.size} items in sync queue")

        if (queue.isEmpty()) {
            Log.d(TAG, "syncPendingData: No items to sync")
            return
        }

        queue.forEach { item ->
            Log.d(TAG, "syncPendingData: Processing ${item.entityType}: ${item.entityId}")
        }

        val sortedQueue = queue.sortedWith(
            compareBy({ it.entityType != "CASH_REGISTER" }, { it.createdAt })
        )

        val pendingCashRegisterIds = sortedQueue
            .filter { it.entityType == "CASH_REGISTER" }
            .map { it.entityId }
            .toMutableSet()

        sortedQueue.forEach { item ->
            try {
                when (item.entityType) {
                    "CASH_REGISTER" -> {
                        syncCashRegister(item.entityId)
                        pendingCashRegisterIds.remove(item.entityId)
                    }
                    "SALE" -> {
                        val sale = offlineDb.salesDao().getSaleById(item.entityId)
                        if (sale == null) {
                            Log.d(TAG, "syncPendingData: Sale ${item.entityId} not found, skipping")
                        } else if (pendingCashRegisterIds.contains(sale.cashRegisterHistoryId)) {
                            Log.d(TAG, "syncPendingData: Deferring sale ${item.entityId} - cash register ${sale.cashRegisterHistoryId} not yet synced")
                            return@forEach
                        } else {
                            syncSale(item.entityId)
                        }
                    }
                }

                offlineDb.syncQueueDao().deleteById(item.id)
                Log.d(TAG, "syncPendingData: Completed ${item.entityType}: ${item.entityId}")

            } catch (e: Exception) {
                Log.e(TAG, "syncPendingData: Error syncing ${item.entityType} ${item.entityId}: ${e.message}")
            }
        }
    }

    private suspend fun syncSale(localSaleId: String) {
        val sale = offlineDb.salesDao().getSaleById(localSaleId)
            ?: throw IllegalStateException("Offline sale not found")

        val details = offlineDb.salesDao().getDetailsBySaleId(localSaleId)

        val domainSale = OfflineSaleWithDetails(sale, details).toDomainSale()

        salesRepository.createSale(domainSale)
            .onSuccess {
                Log.d(TAG, "Sale synced successfully: $localSaleId")
            }
            .onFailure { e ->
                Log.e(TAG, "Error syncing sale $localSaleId: ${e.message}")
                throw e
            }
    }

    private suspend fun syncCashRegister(localHistoryId: String) {
        val history = offlineDb.cashRegisterDao()
            .getById(localHistoryId)
            ?: throw IllegalStateException("Offline cash register history not found")

        val serverHistory: CashRegisterHistory = if (history.isOpen) {
            cashRegisterRepository.openCashRegister(
                cashRegisterId = history.cashRegisterId,
                initialBalance = history.initialBalance
            )
        } else {
            cashRegisterRepository.closeCashRegister(
                realFinalBalance = history.finalBalance
                    ?: throw IllegalStateException("Final balance missing")
            )
        }

        val updatedHistory = history.copy(
            localHistoryId = serverHistory.history_id,
            syncStatus = SyncStatus.SYNCED
        )
        offlineDb.cashRegisterDao().update(updatedHistory)

        offlineDb.salesDao().getSalesByHistoryId(localHistoryId).forEach { sale ->
            offlineDb.salesDao().updateCashRegisterHistoryId(sale.localSaleId, serverHistory.history_id)
        }

        Log.d(TAG, "Cash register synced: $localHistoryId -> ${serverHistory.history_id}")
    }
}
