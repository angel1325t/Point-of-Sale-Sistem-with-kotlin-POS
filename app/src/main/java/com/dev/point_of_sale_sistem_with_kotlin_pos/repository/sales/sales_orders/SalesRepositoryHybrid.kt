package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders

import android.content.Context
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.utils.NetworkUtils
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.sales.data_source.OfflineSalesDataSource
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.sales.data_source.OnlineSalesDataSource
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleCreatedDTO

class HybridSalesRepository(
    private val context: Context,
    private val onlineRepository: SalesRepository
) {

    private val offlineDb = OfflineDatabase.getInstance(context)
    private val offlineDataSource = OfflineSalesDataSource(offlineDb)
    private val onlineDataSource = OnlineSalesDataSource(onlineRepository)

    suspend fun createSale(sale: Sale, branchId: String? = null): Result<SaleCreatedDTO> {
        return if (NetworkUtils.isOnline(context)) {
            onlineRepository.createSale(sale).map {
                SaleCreatedDTO(
                    saleId = it.saleId.toString(),
                    invoiceNumber = it.invoiceNumber
                )
            }
        } else {
            offlineDataSource.createSale(sale).map {
                SaleCreatedDTO(
                    saleId = sale.saleId.toString(),
                    invoiceNumber = "OFFLINE-${System.currentTimeMillis()}"
                )
            }
        }
    }

    suspend fun getPendingSalesCount(): Int {
        return offlineDb.salesDao().getPendingSalesCount()
    }
}
