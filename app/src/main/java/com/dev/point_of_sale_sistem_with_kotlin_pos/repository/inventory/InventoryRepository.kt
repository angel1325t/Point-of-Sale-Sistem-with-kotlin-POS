package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.inventory

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryItem
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryItemDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class InventoryRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TABLE = "products"
        private const val TAG = "INVENTORY_REPO_FCM"
    }

    // ============================================================
    // 1. OBTENER INVENTARIO
    // ============================================================
    suspend fun getInventory(): Result<List<InventoryItem>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo inventario...")
            val query = supabase.from(TABLE).select()
            val list = query.decodeList<InventoryItemDTO>()
            Log.d(TAG, "✅ Inventario obtenido: ${list.size} items")
            Result.success(list.map { it.toInventoryItem() })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getInventory(): ${e.message}", e)
            Result.failure(InventoryError.UnknownError(e))
        }
    }

    // ============================================================
    // 2. AUMENTAR STOCK
    // ============================================================
    suspend fun increaseStock(productId: Int, amount: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Aumentando stock - Product ID: $productId, Amount: $amount")

                val product = supabase.from(TABLE)
                    .select { filter { eq("product_id", productId) } }
                    .decodeSingleOrNull<InventoryItemDTO>()

                if (product == null) {
                    Log.w(TAG, "⚠️ Producto no encontrado: $productId")
                    return@withContext Result.failure(InventoryError.NotFound)
                }

                val oldStock = product.stock
                val newStock = product.stock + amount
                Log.d(TAG, "Stock anterior: $oldStock, nuevo: $newStock")

                supabase.from(TABLE)
                    .update(mapOf("current_stock" to newStock)) {
                        filter { eq("product_id", productId) }
                    }

                Log.d(TAG, "✅ Stock aumentado exitosamente")
                Result.success(true)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error increaseStock(): ${e.message}", e)
                Result.failure(InventoryError.UnknownError(e))
            }
        }

    // ============================================================
    // 3. REDUCIR STOCK
    // ============================================================
    suspend fun decreaseStock(productId: Int, amount: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Reduciendo stock - Product ID: $productId, Amount: $amount")

                val product = supabase.from(TABLE)
                    .select { filter { eq("product_id", productId) } }
                    .decodeSingleOrNull<InventoryItemDTO>()

                if (product == null) {
                    Log.w(TAG, "⚠️ Producto no encontrado: $productId")
                    return@withContext Result.failure(InventoryError.NotFound)
                }

                val oldStock = product.stock
                val newStock = (product.stock - amount).coerceAtLeast(0)
                Log.d(TAG, "Stock anterior: $oldStock, nuevo: $newStock")

                supabase.from(TABLE)
                    .update(mapOf("current_stock" to newStock)) {
                        filter { eq("product_id", productId) }
                    }

                Log.d(TAG, "✅ Stock reducido exitosamente")
                Result.success(true)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error decreaseStock(): ${e.message}", e)
                Result.failure(InventoryError.UnknownError(e))
            }
        }

    // ============================================================
    // 4. OBTENER TOKENS FCM DE TODOS LOS USUARIOS
    // ============================================================
    suspend fun getAllFCMTokens(): List<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========== OBTENIENDO TOKENS FCM ==========")

            val tokens = supabase.from("fcm_tokens")
                .select()
                .decodeList<FCMTokenRecord>()

            Log.d(TAG, "Tokens encontrados: ${tokens.size}")
            tokens.forEachIndexed { index, token ->
                Log.d(TAG, "Token $index: userId=${token.userId}, platform=${token.platform}")
            }

            val tokenStrings = tokens.map { it.token }
            Log.d(TAG, "✅ Tokens FCM obtenidos: ${tokenStrings.size}")
            Log.d(TAG, "========================================")

            tokenStrings

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo tokens FCM: ${e.message}", e)
            emptyList()
        }
    }

    // ============================================================
    // 5. ENVIAR NOTIFICACIÓN A UN TOKEN ESPECÍFICO
    // ============================================================
    suspend fun sendStockNotification(
        token: String,
        title: String,
        body: String
    ): Boolean {
        return try {
            Log.d(TAG, "========== ENVIANDO NOTIFICACIÓN ==========")
            Log.d(TAG, "Token: ${token.take(20)}...")
            Log.d(TAG, "Title: $title")
            Log.d(TAG, "Body: $body")

            val payload = buildJsonObject {
                put("token", token)
                put("title", title)
                put("body", body)
            }

            Log.d(TAG, "Payload: $payload")
            Log.d(TAG, "Invocando función: send_notifications")

            val response = supabase.functions.invoke(
                function = "send_notifications",
                body = payload
            )

            Log.d(TAG, "Response status: ${response.status}")
            Log.d(TAG, "✅ Notificación enviada correctamente")
            Log.d(TAG, "========================================")
            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando notificación push")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            Log.d(TAG, "========================================")
            false
        }
    }

    // ============================================================
    // 6. ENVIAR NOTIFICACIONES A TODOS LOS DISPOSITIVOS
    // ============================================================
    suspend fun sendNotificationToAll(title: String, body: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "========== ENVIANDO NOTIFICACIONES MASIVAS ==========")
                Log.d(TAG, "Title: $title")
                Log.d(TAG, "Body: $body")

                val tokens = getAllFCMTokens()
                Log.d(TAG, "Enviando a ${tokens.size} dispositivos...")

                var successCount = 0

                tokens.forEachIndexed { index, token ->
                    Log.d(TAG, "Enviando $index/${tokens.size}...")
                    val success = sendStockNotification(token, title, body)
                    if (success) {
                        successCount++
                        Log.d(TAG, "✅ Enviado $index/${tokens.size}")
                    } else {
                        Log.w(TAG, "⚠️ Falló $index/${tokens.size}")
                    }
                }

                Log.d(TAG, "✅ COMPLETADO: $successCount/${tokens.size} notificaciones enviadas")
                Log.d(TAG, "=====================================================")
                successCount

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en sendNotificationToAll: ${e.message}", e)
                0
            }
        }
    }

    // ============================================================
    // 7. DTO → Modelo
    // ============================================================
    private fun InventoryItemDTO.toInventoryItem() = InventoryItem(
        id = id,
        name = name,
        description = description,
        price = price,
        barcode = barcode,
        categoryId = categoryId,
        categoryName = null,
        stock = stock,
        minStock = minStock,
        image = image
    )
}

// ============================================================
// MODELO PARA TOKENS FCM
// ============================================================
@Serializable
data class FCMTokenRecord(
    val userId: String,
    val token: String,
    val platform: String,
    val createdAt: String? = null
)