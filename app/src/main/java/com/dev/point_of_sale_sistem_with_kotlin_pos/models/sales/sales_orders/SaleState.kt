package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

data class SaleState(
    val isLoading: Boolean = false,
    val sale: Sale? = null,
    val sales: List<Sale> = emptyList(),
    val error: SaleError? = null
)