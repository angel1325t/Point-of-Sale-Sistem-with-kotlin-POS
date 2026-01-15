package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

sealed class SaleError {
    object Network : SaleError()
    object ValidationFailed : SaleError()
    data class Server(val message: String) : SaleError()
    data class Unknown(val throwable: Throwable) : SaleError()
}