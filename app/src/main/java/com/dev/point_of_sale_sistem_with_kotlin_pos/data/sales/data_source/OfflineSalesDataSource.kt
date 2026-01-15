package com.dev.point_of_sale_sistem_with_kotlin_pos.data.sales.data_source

import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineSaleDetailEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineSaleEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.SyncQueueEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import java.util.UUID

class OfflineSalesDataSource(
    private val db: OfflineDatabase
) : SalesDataSource {

    override suspend fun createSale(sale: Sale): Result<Unit> = runCatching {

        // 1️⃣ Guardar venta
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

        // 2️⃣ Guardar detalles
        db.salesDao().insertDetails(
            sale.saleDetails.map {
                OfflineSaleDetailEntity(
                    localSaleId = saleId,  // ✅ Corregido
                    productId = it.productId,
                    quantity = it.quantity,
                    unitPrice = it.unitPrice,
                    discount = it.discount,
                    finalPrice = it.finalPrice
                )
            }
        )

        // 3️⃣ Reducir stock local
        sale.saleDetails.forEach {
            db.stockDao().reduceStock(it.productId, it.quantity)
        }

        // 4️⃣ Encolar sincronización
        db.syncQueueDao().insert(
            SyncQueueEntity(
                entityType = "SALE",
                entityId = saleId
            )
        )
    }
}