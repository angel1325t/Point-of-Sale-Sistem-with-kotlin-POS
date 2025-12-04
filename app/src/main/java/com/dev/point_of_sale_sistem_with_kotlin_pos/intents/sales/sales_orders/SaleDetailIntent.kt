package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders

sealed class SaleDetailIntent {
    
    // 🗑 Eliminar item
    data class RemoveSaleDetail(
        val saleDetailId: Int
    ) : SaleDetailIntent()

    // 🔁 Actualizar producto ya agregado
    data class UpdateSaleDetail(
        val saleDetailId: Int,
        val quantity: Int,
        val unitPrice: Double,
        val discount: Double
    ) : SaleDetailIntent()
}
