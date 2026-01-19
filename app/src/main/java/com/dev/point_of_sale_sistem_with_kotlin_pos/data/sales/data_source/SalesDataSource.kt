package com.dev.point_of_sale_sistem_with_kotlin_pos.data.sales.data_source

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale

interface SalesDataSource {

    suspend fun createSale(sale: Sale): Result<Unit>
}