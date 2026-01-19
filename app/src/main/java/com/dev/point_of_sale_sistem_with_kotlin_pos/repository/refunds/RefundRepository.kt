package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.refunds

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.ApplyCreditNoteRpcDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreateCreditNoteRpcDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditNote
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditNoteDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditNoteItemRpcDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import java.util.UUID

class RefundRepository(
    private val supabase: SupabaseClient
) {

    companion object {
        private const val TAG = "RefundRepository"
    }

    // ─────────────────────────────────────────────
    // BUSCAR VENTA POR FACTURA
    // ─────────────────────────────────────────────
    suspend fun findSaleByInvoiceNumber(
        invoiceNumber: String
    ): Result<RefundableSale?> = runCatching {

        Log.d(TAG, "Searching sale by invoice: $invoiceNumber")

        val response = supabase.from("sales")
            .select(
                columns = Columns.raw(
                    """
                    sale_id,
                    invoice_number,
                    sale_date,
                    total,
                    payment_method,
                    refunded_total,
                    is_credit_note,
                    original_sale_id,
                    credit_remaining,
                    sale_details (
                        sale_detail_id,
                        product_id,
                        quantity,
                        refunded_quantity,
                        unit_price,
                        discount,
                        final_price,
                        products ( name )
                    )
                    """.trimIndent()
                )
            ) {
                filter {
                    eq("invoice_number", invoiceNumber)
                }
            }
            .decodeSingleOrNull<RefundableSaleDTO>()

        if (response == null) {
            Log.d(TAG, "No sale found with invoice: $invoiceNumber")
            return@runCatching null
        }

        Log.d(TAG, "Sale found: ${response.invoiceNumber} with ${response.saleDetails.size} details")

        response.toDomain()
    }

    // ─────────────────────────────────────────────
    // BUSCAR NOTA DE CRÉDITO POR SALE_ID (PARA IMPRIMIR)
    // ─────────────────────────────────────────────
    suspend fun findCreditNoteBySaleId(
        saleId: UUID
    ): Result<RefundableSale?> = runCatching {

        Log.d(TAG, "Fetching credit note by sale_id: $saleId")

        val response = supabase.from("sales")
            .select(
                columns = Columns.raw(
                    """
                    sale_id,
                    invoice_number,
                    sale_date,
                    total,
                    payment_method,
                    refunded_total,
                    is_credit_note,
                    original_sale_id,
                    credit_remaining,
                    sale_details (
                        sale_detail_id,
                        product_id,
                        quantity,
                        refunded_quantity,
                        unit_price,
                        discount,
                        final_price,
                        products ( name )
                    )
                    """.trimIndent()
                )
            ) {
                filter {
                    eq("sale_id", saleId.toString())
                    eq("is_credit_note", true)
                }
            }
            .decodeSingleOrNull<RefundableSaleDTO>()

        if (response == null) {
            Log.d(TAG, "No credit note found with sale_id: $saleId")
            return@runCatching null
        }

        Log.d(TAG, "Credit note found: ${response.invoiceNumber}")
        response.toDomain()
    }

    // ─────────────────────────────────────────────
    // CREAR NOTA DE CRÉDITO (RPC)
    // ─────────────────────────────────────────────
    suspend fun createCreditNote(
        originalSaleId: UUID,
        userId: UUID,
        refundItems: List<RefundItem>,
        subtotal: Double,
        itbis: Double,
        totalRefund: Double
    ): Result<UUID> = runCatching {

        Log.d(TAG, "Creating credit note via RPC")
        Log.d(TAG, "Original Sale ID: $originalSaleId")
        Log.d(TAG, "User ID: $userId")
        Log.d(TAG, "Subtotal: $subtotal")
        Log.d(TAG, "ITBIS: $itbis")
        Log.d(TAG, "Total Refund: $totalRefund")
        Log.d(TAG, "Items to refund: ${refundItems.size}")

        // Validar que los cálculos sean correctos
        val calculatedTotal = subtotal + itbis
        if (kotlin.math.abs(calculatedTotal - totalRefund) > 0.01) {
            Log.e(TAG, "Total mismatch: calculated=$calculatedTotal, provided=$totalRefund")
            throw IllegalArgumentException("Error en cálculo: subtotal + ITBIS no coincide con total")
        }

        val params = CreateCreditNoteRpcDTO(
            items = refundItems.map { item ->
                Log.d(TAG, "Item: Product ${item.productId}, Qty ${item.quantityToRefund}, Amount ${item.refundAmount}")
                CreditNoteItemRpcDTO(
                    saleDetailId = item.saleDetailId,
                    productId = item.productId,
                    quantity = item.quantityToRefund,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    refundAmount = item.refundAmount
                )
            },
            originalSaleId = originalSaleId.toString(),
            totalRefund = totalRefund,
            userId = userId.toString()
        )

        // La RPC devuelve un string UUID directamente
        val creditNoteIdString = supabase
            .postgrest
            .rpc(
                function = "create_credit_note_v4",
                parameters = params
            )
            .decodeAs<String>()

        Log.d(TAG, "RPC Response: $creditNoteIdString")

        // Limpiar comillas si existen y convertir a UUID
        val cleanedId = creditNoteIdString.trim().removeSurrounding("\"")
        val creditNoteId = UUID.fromString(cleanedId)

        Log.d(TAG, "Credit note created successfully: $creditNoteId")
        creditNoteId
    }

    // ─────────────────────────────────────────────
    // BUSCAR NOTA DE CRÉDITO POR INVOICE
    // ─────────────────────────────────────────────
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun findCreditNoteByInvoiceNumber(
        invoiceNumber: String
    ): Result<CreditNote?> = runCatching {

        Log.d(TAG, "Searching credit note by invoice: $invoiceNumber")

        val response = supabase.from("sales")
            .select {
                filter {
                    eq("invoice_number", invoiceNumber)
                    eq("is_credit_note", true)
                }
            }
            .decodeSingleOrNull<CreditNoteDTO>()

        if (response == null) {
            Log.d(TAG, "No credit note found with invoice: $invoiceNumber")
            return@runCatching null
        }

        response.toDomain()
    }

    // ─────────────────────────────────────────────
    // APLICAR CRÉDITO A UNA VENTA (RPC)
    // ─────────────────────────────────────────────
    suspend fun applyCreditToSale(
        creditNoteId: UUID,
        amountToApply: Double
    ): Result<Unit> = runCatching {

        Log.d(TAG, "Applying credit note: $creditNoteId, amount: $amountToApply")

        val params = ApplyCreditNoteRpcDTO(
            creditNoteId = creditNoteId.toString(),
            amount = amountToApply
        )

        supabase
            .postgrest
            .rpc(
                function = "apply_credit_note",
                parameters = params
            )

        Log.d(TAG, "Credit applied successfully")
        Unit
    }
}