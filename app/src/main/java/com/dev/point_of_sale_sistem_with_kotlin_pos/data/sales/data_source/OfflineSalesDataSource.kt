package com.dev.point_of_sale_sistem_with_kotlin_pos.data.sales.data_source

import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineSaleDetailEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineSaleEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.SyncQueueEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesProductRepository
import java.util.UUID

class OfflineSalesDataSource(
    private val db: OfflineDatabase,
    private val salesProductRepository: SalesProductRepository? = null
) : SalesDataSource {

    override suspend fun createSale(sale: Sale): Result<Unit> = runCatching {

        val saleId = UUID.randomUUID().toString()

        db.salesDao().insertSale(
            OfflineSaleEntity(
                localSaleId = saleId,
                userId = sale.userId.toString(),
                cashRegisterHistoryId = sale.cashRegisterHistoryId,
                subtotal = sale.subtotal,
                itbis = sale.itbis,
                total = sale.total,
                paymentMethod = sale.paymentMethod,
                status = sale.status,
                createdAt = System.currentTimeMillis(),
                pendingSync = true,
                globalDiscount = sale.globalDiscount
            )
        )

        db.salesDao().insertDetails(
            sale.saleDetails.map {
                OfflineSaleDetailEntity(
                    localSaleId = saleId,
                    productId = it.productId,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    discount = it.discount,
                    finalPrice = it.finalPrice
                )
            }
        )

        sale.saleDetails.forEach {
            salesProductRepository?.reduceStock(it.productId, it.quantity)
                ?: db.stockDao().reduceStock(it.productId, it.quantity)
        }

        db.syncQueueDao().insert(
            SyncQueueEntity(
                entityType = "CASH_REGISTER",
                entityId = sale.cashRegisterHistoryId
            )
        )

        db.syncQueueDao().insert(
            SyncQueueEntity(
                entityType = "SALE",
                entityId = saleId
            )
        )
    }
}