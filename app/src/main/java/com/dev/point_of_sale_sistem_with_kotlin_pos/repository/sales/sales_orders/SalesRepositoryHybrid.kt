package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders

import android.content.Context
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.utils.NetworkUtils
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.sales.data_source.OfflineSalesDataSource
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.sales.data_source.OnlineSalesDataSource
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleCreatedDTO

/**
 * Repositorio híbrido que decide entre modo online u offline
 */
class HybridSalesRepository(
    private val context: Context,
    private val onlineRepository: SalesRepository
) {

    private val offlineDb = OfflineDatabase.getInstance(context)

    private val offlineDataSource = OfflineSalesDataSource(offlineDb)
    private val onlineDataSource = OnlineSalesDataSource(onlineRepository)

    /**
     * Crea una venta eligiendo automáticamente entre modo online u offline
     */
    suspend fun createSale(sale: Sale): Result<SaleCreatedDTO> {
        return if (NetworkUtils.isOnline(context)) {
            // 🌐 ONLINE: usar repositorio normal
            onlineRepository.createSale(sale)
        } else {
            // 📴 OFFLINE: guardar localmente
            offlineDataSource.createSale(sale).map {
                // Retornar un DTO simulado para compatibilidad
                SaleCreatedDTO(
                    saleId = sale.saleId.toString(),
                    invoiceNumber = "OFFLINE-${System.currentTimeMillis()}"
                )
            }
        }
    }

    /**
     * Obtiene el conteo de ventas pendientes de sincronizar
     */
    suspend fun getPendingSalesCount(): Int {
        return offlineDb.salesDao().getPendingSalesCount()
    }
}