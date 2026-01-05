package com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// ═══════════════════════════════════════════════════
// MODELOS DE DOMINIO (USADOS POR VIEWMODEL)
// ═══════════════════════════════════════════════════

data class CreditNote(
    val saleId: UUID,
    val invoiceNumber: String,
    val createdAt: LocalDateTime,
    val originalTotal: Double,
    val creditRemaining: Double,
    val originalSaleId: UUID?
) {
    val isFullyUsed: Boolean
        get() = creditRemaining <= 0.01
}

data class CreditApplication(
    val creditNoteId: UUID,
    val amountToApply: Double,
    val remainingCredit: Double,
    val saleTotal: Double
) {
    val needsAdditionalPayment: Boolean
        get() = saleTotal > amountToApply

    val additionalPaymentRequired: Double
        get() = if (needsAdditionalPayment) saleTotal - amountToApply else 0.0
}

// ═══════════════════════════════════════════════════
// DTO PARA DESERIALIZAR DESDE SUPABASE
// ═══════════════════════════════════════════════════

@Serializable
data class CreditNoteDTO(
    @SerialName("sale_id")
    val saleId: String,

    @SerialName("invoice_number")
    val invoiceNumber: String,

    @SerialName("total")
    val total: Double,

    @SerialName("credit_remaining")
    val creditRemaining: Double,

    @SerialName("original_sale_id")
    val originalSaleId: String?,

    @SerialName("created_at")
    val createdAt: String
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun toDomain(): CreditNote {
        return CreditNote(
            saleId = UUID.fromString(saleId),
            invoiceNumber = invoiceNumber,
            createdAt = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_DATE_TIME),
            originalTotal = total,
            creditRemaining = creditRemaining,
            originalSaleId = originalSaleId?.let { UUID.fromString(it) }
        )
    }
}

// ═══════════════════════════════════════════════════
// DTOs PARA RPC: CREAR NOTA DE CRÉDITO
// ═══════════════════════════════════════════════════
@Serializable
data class CreateCreditNoteRpcDTO(

    @SerialName("p_items")
    val items: List<CreditNoteItemRpcDTO>,

    @SerialName("p_original_sale_id")
    val originalSaleId: String,

    @SerialName("p_total_refund")
    val totalRefund: Double,

    @SerialName("p_user_id")
    val userId: String
)
@Serializable
data class CreditNoteDebugDTO(
    val items: JsonElement,

    @SerialName("items_total")
    val itemsTotal: Double,

    @SerialName("expected_subtotal")
    val expectedSubtotal: Double,

    @SerialName("original_sale")
    val originalSale: String,

    @SerialName("user_id")
    val userId: String
)




@Serializable
data class CreditNoteItemRpcDTO(
    @SerialName("sale_detail_id")
    val saleDetailId: Int,

    @SerialName("product_id")
    val productId: Int,

    @SerialName("quantity")
    val quantity: Int,

    @SerialName("unit_price")
    val unitPrice: Double,

    @SerialName("discount")
    val discount: Double,

    @SerialName("refund_amount")
    val refundAmount: Double
)

// ═══════════════════════════════════════════════════
// DTO PARA RPC: APLICAR NOTA DE CRÉDITO
// ═══════════════════════════════════════════════════

@Serializable
data class ApplyCreditNoteRpcDTO(
    @SerialName("p_credit_note_id")
    val creditNoteId: String,

    @SerialName("p_amount")
    val amount: Double
)
