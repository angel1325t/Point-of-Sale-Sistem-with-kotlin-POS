package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SalePaymentProof
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

@Serializable
data class PaymentProofInsertDTO(
    val sale_id: String,
    val payment_method: String,
    val voucher_path: String,
    val reference_number: String,
    val amount: Double,
    val captured_by: String
)

class PaymentProofRepository(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "PaymentProofRepository"
        private const val BUCKET_NAME = "transaction-vouchers"
    }

    // ═══════════════════════════════════════════════════
    // 📤 SUBIR EVIDENCIA A SUPABASE STORAGE
    // ═══════════════════════════════════════════════════
    suspend fun uploadVoucherImage(
        saleId: String,
        imageFile: File
    ): Result<String> {
        return try {
            val fileName = "${saleId}_${System.currentTimeMillis()}.jpg"
            val path = "sales/$fileName"

            Log.d(TAG, "Uploading voucher: $path")

            val bytes = imageFile.readBytes()

            // Subir usando la sintaxis correcta de Supabase Storage
            supabase.storage
                .from(BUCKET_NAME)
                .upload(
                    path = path,
                    data = bytes
                ) {
                    upsert = false
                }

            Log.d(TAG, "Voucher uploaded successfully: $path")

            // Borrar archivo local
            imageFile.delete()
            Log.d(TAG, "Local file deleted")

            Result.success(path)

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading voucher", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════
    // 💾 GUARDAR REGISTRO DE EVIDENCIA EN BD
    // ═══════════════════════════════════════════════════
    suspend fun createPaymentProof(
        saleId: String,
        paymentMethod: String,
        voucherPath: String,
        referenceNumber: String,
        amount: Double,
        capturedBy: String
    ): Result<SalePaymentProof> {
        return try {
            Log.d(TAG, "Creating payment proof record for sale: $saleId")

            val insertDTO = PaymentProofInsertDTO(
                sale_id = saleId,
                payment_method = paymentMethod,
                voucher_path = voucherPath,
                reference_number = referenceNumber,
                amount = amount,
                captured_by = capturedBy
            )

            val proof = supabase.from("sale_payment_proofs")
                .insert(insertDTO) {
                    select()
                }
                .decodeSingle<SalePaymentProof>()

            Log.d(TAG, "Payment proof created: ${proof.proofId}")
            Result.success(proof)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating payment proof", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════
    // 🔍 VALIDAR SI REFERENCIA YA EXISTE (ANTIFRAUDE)
    // ═══════════════════════════════════════════════════
    suspend fun validateReferenceNumber(referenceNumber: String): Result<Boolean> {
        return try {
            val existing = supabase.from("sale_payment_proofs")
                .select {
                    filter {
                        eq("reference_number", referenceNumber)
                    }
                }
                .decodeList<SalePaymentProof>()

            val isValid = existing.isEmpty()
            Log.d(TAG, "Reference validation: $referenceNumber -> ${if (isValid) "VALID" else "DUPLICATE"}")

            Result.success(isValid)

        } catch (e: Exception) {
            Log.e(TAG, "Error validating reference", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════
    // 🔒 OBTENER EVIDENCIA DE UNA VENTA
    // ═══════════════════════════════════════════════════
    suspend fun getPaymentProofBySale(saleId: String): Result<SalePaymentProof?> {
        return try {
            val proofs = supabase.from("sale_payment_proofs")
                .select {
                    filter {
                        eq("sale_id", saleId)
                    }
                }
                .decodeList<SalePaymentProof>()

            Result.success(proofs.firstOrNull())

        } catch (e: Exception) {
            Log.e(TAG, "Error getting payment proof", e)
            Result.failure(e)
        }
    }
}