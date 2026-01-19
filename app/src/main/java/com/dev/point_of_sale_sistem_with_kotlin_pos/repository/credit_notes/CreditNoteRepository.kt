package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.credit_notes

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditNoteDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditNoteUsageDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditNoteUsageInsertDTO
import io.github.jan.supabase.SupabaseClient
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditRemainingDTO
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns




class CreditNoteRepository(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "CreditNoteRepository"
    }

    /**
     * Obtiene una nota de crédito por su ID (sale_id)
     */
    suspend fun getCreditNoteById(saleId: String): Result<CreditNoteDTO?> = runCatching {
        Log.d(TAG, "Fetching credit note: $saleId")

        val result = supabase.from("sales")
            .select(
                Columns.list(
                    "sale_id",
                    "invoice_number",
                    "credit_remaining",
                    "total",
                    "is_credit_note",
                    "original_sale_id",
                    "created_at"
                )
            ) {
                filter {
                    eq("sale_id", saleId)
                    eq("is_credit_note", true)
                    eq("status", "completed")
                }
            }
            .decodeSingleOrNull<CreditNoteDTO>()

        Log.d(TAG, "Credit note found: ${result != null}, remaining: ${result?.creditRemaining}")
        result
    }

    /**
     * Obtiene una nota de crédito por su número de factura
     */
    suspend fun getCreditNoteByInvoice(invoiceNumber: String): Result<CreditNoteDTO?> = runCatching {
        Log.d(TAG, "Fetching credit note by invoice: $invoiceNumber")

        supabase.from("sales")
            .select(
                Columns.list(
                    "sale_id",
                    "invoice_number",
                    "credit_remaining",
                    "total",
                    "is_credit_note",
                    "original_sale_id",
                    "created_at"
                )
            ) {
                filter {
                    eq("invoice_number", invoiceNumber)
                    eq("is_credit_note", true)
                    eq("status", "completed")
                }
            }
            .decodeSingleOrNull<CreditNoteDTO>()
    }

    /**
     * Registra el uso de una nota de crédito y actualiza el crédito restante
     * Esta función se llama DESPUÉS de que la venta se haya creado exitosamente
     */
    suspend fun applyCreditNote(
        creditNoteId: String,
        appliedToSaleId: String,
        amountApplied: Double
    ): Result<Unit> = runCatching {
        Log.d(TAG, "Applying credit note: $creditNoteId to sale: $appliedToSaleId, amount: $amountApplied")

        // 1. Obtener crédito actual
        val currentCredit = supabase.from("sales")
            .select(Columns.list("credit_remaining")) {
                filter {
                    eq("sale_id", creditNoteId)
                }
            }
            .decodeSingle<CreditRemainingDTO>()

        val newRemaining = currentCredit.creditRemaining - amountApplied

        if (newRemaining < -0.01) { // Tolerancia para errores de redondeo
            throw IllegalStateException(
                "Monto aplicado ($amountApplied) excede el crédito disponible (${currentCredit.creditRemaining})"
            )
        }

        // 2. Actualizar credit_remaining en la tabla sales
        supabase.from("sales")
            .update({
                set("credit_remaining", maxOf(0.0, newRemaining))
            }) {
                filter {
                    eq("sale_id", creditNoteId)
                }
            }

        Log.d(TAG, "Updated credit_remaining: ${currentCredit.creditRemaining} -> $newRemaining")

        // 3. Registrar el uso en credit_note_usage
        val usageRecord = CreditNoteUsageInsertDTO(
            creditNoteId = creditNoteId,
            appliedToSaleId = appliedToSaleId,
            amountApplied = amountApplied
        )

        supabase.from("credit_note_usage")
            .insert(usageRecord)

        Log.d(TAG, "Credit note usage recorded successfully")
    }

    /**
     * Obtiene el historial de uso de una nota de crédito
     */
    suspend fun getCreditNoteUsageHistory(creditNoteId: String): Result<List<CreditNoteUsageDTO>> = runCatching {
        supabase.from("credit_note_usage")
            .select() {
                filter {
                    eq("credit_note_id", creditNoteId)
                }
            }
            .decodeList<CreditNoteUsageDTO>()
    }

    /**
     * Valida si una nota de crédito tiene suficiente saldo
     */
    suspend fun validateCreditAmount(
        creditNoteId: String,
        requestedAmount: Double
    ): Result<Boolean> = runCatching {
        val creditNote = getCreditNoteById(creditNoteId).getOrNull()

        if (creditNote == null) {
            Log.w(TAG, "Credit note not found: $creditNoteId")
            return@runCatching false
        }

        val hasEnough = creditNote.creditRemaining >= requestedAmount
        Log.d(TAG, "Credit validation - Requested: $requestedAmount, Available: ${creditNote.creditRemaining}, Valid: $hasEnough")

        hasEnough
    }
}