package com.dev.point_of_sale_sistem_with_kotlin_pos.data.sales.data_source

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesRepository

class OnlineSalesDataSource(
    private val repository: SalesRepository
) : SalesDataSource {

    override suspend fun createSale(sale: Sale): Result<Unit> {
        return repository.createSale(sale).map { }
    }
}