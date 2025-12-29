package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class SalePaymentProof(
    @SerialName("proof_id")
    val proofId: String,

    @SerialName("sale_id")
    val saleId: String,

    @SerialName("payment_method")
    val paymentMethod: String,

    @SerialName("voucher_path")
    val voucherPath: String,

    @SerialName("reference_number")
    val referenceNumber: String,

    @SerialName("amount")
    val amount: Double,

    @SerialName("captured_by")
    val capturedBy: String,

    @SerialName("captured_at")
    val capturedAt: String
)

// ═══════════════════════════════════════════════════
// ESTADOS PARA LA UI
// ═══════════════════════════════════════════════════

sealed class PaymentFlowState {
    object Idle : PaymentFlowState()
    object SelectingMethod : PaymentFlowState()
    data class CashPayment(val total: Double) : PaymentFlowState()
    data class TransferPayment(val total: Double) : PaymentFlowState()
    data class CapturingEvidence(val saleId: String, val total: Double) : PaymentFlowState()
    data class ProcessingPayment(val method: String) : PaymentFlowState()
    data class Success(val saleId: String) : PaymentFlowState()
    data class Error(val message: String) : PaymentFlowState()
}

data class TransferEvidenceData(
    val imageFile: java.io.File?,
    val referenceNumber: String = "",
    val isValid: Boolean = false
)