package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders

import java.util.UUID

sealed class SalesIntent {
    // Search
    data class SearchProductByName(val query: String) : SalesIntent()
    data class SearchProductByBarcode(val barcode: String) : SalesIntent()
    object ClearSearchResults : SalesIntent()  // NUEVO

    // Sale Details
    data class AddSaleDetail(
        val productId: Int,
        val quantity: Int,
        val unitPrice: Double,
        val discount: Double
    ) : SalesIntent()

    data class RemoveSaleDetail(val productId: Int) : SalesIntent()

    data class UpdateSaleDetail(
        val productId: Int,
        val quantity: Int,
        val unitPrice: Double,
        val discount: Double
    ) : SalesIntent()

    // Sale Operations
    data class CreateSale(
        val paymentMethod: String,
        val globalDiscount: Double,
        val userId: UUID
    ) : SalesIntent()

    object CompleteSale : SalesIntent()
    object CancelSale : SalesIntent()
    object ClearError : SalesIntent()
}