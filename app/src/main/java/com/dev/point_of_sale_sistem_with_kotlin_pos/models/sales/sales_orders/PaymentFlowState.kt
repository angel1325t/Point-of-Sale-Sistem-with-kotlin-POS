package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

sealed class PaymentFlowState {
    object Idle : PaymentFlowState()

    data class CashPayment(val total: Double) : PaymentFlowState()

    data class CardPayment(
        val total: Double,
        val paymentIntentId: String? = null,
        val clientSecret: String? = null,
        val errorMessage: String? = null
    ) : PaymentFlowState()

    data class TransferPayment(val total: Double) : PaymentFlowState()

    data class CapturingEvidence(
        val saleId: String,
        val total: Double
    ) : PaymentFlowState()

    data class ProcessingPayment(val method: String) : PaymentFlowState()

    data class Success(val saleId: String) : PaymentFlowState()

    data class Error(val message: String) : PaymentFlowState()
}