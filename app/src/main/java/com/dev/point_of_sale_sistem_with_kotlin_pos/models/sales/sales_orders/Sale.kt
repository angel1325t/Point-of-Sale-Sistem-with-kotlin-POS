package com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders

import android.os.Build
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds.RefundableSale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds.RefundableSaleDetail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

data class Sale(
    val saleId: UUID,
    val userId: UUID,
    val saleDate: LocalDateTime,
    val subtotal: Double,  // Subtotal sin impuestos
    val itbis: Double,      // ITBIS (18%)
    val total: Double,      // Total con impuestos
    val paymentMethod: String,
    val status: String,
    val createdAt: LocalDateTime,
    val globalDiscount: Double,
    val invoiceNumber: String? = null,
    val isCreditNote: Boolean = false,
    val originalSaleId: UUID? = null,
    val creditRemaining: Double? = null,
    val cashRegisterHistoryId: String = "",

    val saleDetails: List<SaleDetail> = emptyList()
)

data class AppliedCreditNote(
    val creditNoteId: UUID,
    val invoiceNumber: String,
    val amountApplied: Double,
    val appliedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SaleInsertDTO(
    @SerialName("user_id")
    val userId: String,

    @SerialName("sale_date")
    val saleDate: String,

    @SerialName("subtotal")
    val subtotal: Double,

    @SerialName("itbis")
    val itbis: Double,

    @SerialName("total")
    val total: Double,

    @SerialName("payment_method")
    val paymentMethod: String,

    @SerialName("status")
    val status: String,

    @SerialName("global_discount")
    val globalDiscount: Double,

    @SerialName("is_credit_note")
    val isCreditNote: Boolean = false,

    @SerialName("original_sale_id")
    val originalSaleId: String? = null,

    @SerialName("credit_remaining")
    val creditRemaining: Double? = null,

    @SerialName("cash_register_history_id")
    val cashRegisterHistoryId: String,

    // ✅ OPCIONAL: Solo se usa en modo offline para tracking
    @SerialName("local_id")
    val localId: String? = null
)

@Serializable
data class SaleCreatedDTO(
    @SerialName("sale_id")
    val saleId: String,

    @SerialName("invoice_number")
    val invoiceNumber: String
)

@Serializable
data class SaleWithDetailsDTO(
    val saleId: String,
    val invoiceNumber: String,
    val saleDate: String,
    val subtotal: Double,
    val itbis: Double,
    val total: Double,
    val paymentMethod: String,
    val refundedTotal: Double = 0.0,
    val isCreditNote: Boolean = false,
    val originalSaleId: String? = null,
    val creditRemaining: Double? = null,
    val saleDetails: List<SaleDetailWithProductDTO>
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun toDomain() = RefundableSale(
        saleId = UUID.fromString(saleId),
        invoiceNumber = invoiceNumber,
        saleDate = LocalDateTime.parse(saleDate),
        total = total,
        paymentMethod = paymentMethod,
        refundedTotal = refundedTotal,
        isCreditNote = isCreditNote,
        originalSaleId = originalSaleId?.let(UUID::fromString),
        creditRemaining = creditRemaining,
        details = saleDetails.map { it.toDomain() }
    )
}

@Serializable
data class SaleDetailWithProductDTO(
    val saleDetailId: Int,
    val productId: Int,
    val quantity: Int,
    val refundedQuantity: Int = 0,
    val unitPrice: Double,
    val discount: Double,
    val finalPrice: Double,
    val products: ProductNameDTO
) {
    fun toDomain() = RefundableSaleDetail(
        saleDetailId = saleDetailId,
        productId = productId,
        productName = products.name,
        quantity = quantity,
        refundedQuantity = refundedQuantity,
        unitPrice = unitPrice,
        discount = discount,
        finalPrice = finalPrice
    )
}

@Serializable
data class ProductNameDTO(
    val name: String
)