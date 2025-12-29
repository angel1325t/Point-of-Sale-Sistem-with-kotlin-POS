package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO

data class SaleState(
    val sale: Sale? = null,
    val searchResults: List<ProductDTO> = emptyList(),
    val productsCache: Map<Int,ProductDTO> = emptyMap(),
    val isLoading: Boolean = false,
    val error: SaleError? = null,
    val lastScannedBarcode: String? = null,
    val paymentFlowState: PaymentFlowState = PaymentFlowState.Idle,
    val transferEvidence: TransferEvidenceData? = null
)