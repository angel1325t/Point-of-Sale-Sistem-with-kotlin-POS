package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders

import android.content.Context
import android.util.Log
import com.stripe.android.PaymentConfiguration
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class StripePaymentRepository(
    private val context: Context,
    private val publishableKey: String,
    private val supabase: SupabaseClient
) {
    companion object {
        private const val TAG = "StripePaymentRepository"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        // Configurar Stripe
        PaymentConfiguration.init(context, publishableKey)
    }

    /**
     * Crea un Payment Intent usando Edge Function
     */
    suspend fun createPaymentIntent(
        amount: Double,
        currency: String = "USD",
        saleId: String
    ): Result<PaymentIntentResponse> = runCatching {
        Log.d(TAG, "Creating Stripe Payment Intent for amount: $amount, saleId: $saleId")

        val response = supabase.functions.invoke(
            function = "create-stripe-payment-intent",
            body = buildJsonObject {
                put("amount", amount)
                put("currency", currency)
                put("saleId", saleId)
            }
        )

        val responseBody = response.body<String>()
        Log.d(TAG, "Edge Function response: $responseBody")

        val intentResponse = json.decodeFromString<CreatePaymentIntentResponse>(responseBody)

        if (intentResponse.success && intentResponse.clientSecret != null) {
            Log.d(TAG, "Payment Intent created successfully: ${intentResponse.paymentIntentId}")
            PaymentIntentResponse(
                clientSecret = intentResponse.clientSecret,
                paymentIntentId = intentResponse.paymentIntentId ?: ""
            )
        } else {
            throw Exception(intentResponse.error ?: "Error desconocido al crear payment intent")
        }
    }

    /**
     * Confirma el pago usando Edge Function
     */
    suspend fun confirmPayment(paymentIntentId: String): Result<StripePaymentConfirmation> = runCatching {
        Log.d(TAG, "Confirming Stripe payment for: $paymentIntentId")

        val response = supabase.functions.invoke(
            function = "confirm-stripe-payment",
            body = buildJsonObject {
                put("paymentIntentId", paymentIntentId)
            }
        )

        val responseBody = response.body<String>()
        Log.d(TAG, "Confirmation response: $responseBody")

        val confirmResponse = json.decodeFromString<ConfirmPaymentResponse>(responseBody)

        if (confirmResponse.success && confirmResponse.paymentIntentId != null) {
            Log.d(TAG, "Payment confirmed successfully: ${confirmResponse.paymentIntentId}")
            StripePaymentConfirmation(
                paymentIntentId = confirmResponse.paymentIntentId,
                status = confirmResponse.status ?: "succeeded",
                amount = confirmResponse.amount ?: 0.0,
                chargeId = confirmResponse.chargeId
            )
        } else {
            throw Exception(confirmResponse.error ?: "Error desconocido al confirmar pago")
        }
    }
}

// ═══════════════════════════════════════════════════
// Data Classes para Edge Functions
// ═══════════════════════════════════════════════════

@Serializable
data class CreatePaymentIntentResponse(
    val success: Boolean,
    val clientSecret: String? = null,
    val paymentIntentId: String? = null,
    val error: String? = null
)

@Serializable
data class ConfirmPaymentResponse(
    val success: Boolean,
    val paymentIntentId: String? = null,
    val status: String? = null,
    val amount: Double? = null,
    val chargeId: String? = null,
    val error: String? = null
)

// Result classes
data class PaymentIntentResponse(
    val clientSecret: String,
    val paymentIntentId: String
)

data class StripePaymentConfirmation(
    val paymentIntentId: String,
    val status: String,
    val amount: Double,
    val chargeId: String?
)

class StripeCanceledException : Exception("Usuario canceló el pago con Stripe")