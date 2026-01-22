package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.inventory

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryItem
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryItemDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class InventoryRepository(
    private val supabase: SupabaseClient,
    private val sessionPreferences: SessionPreferences
) {

    companion object {
        private const val TABLE = "products"
        private const val TAG = "INVENTORY_REPO"
    }

    // ============================================================
    // 1. OBTENER INVENTARIO (FILTRADO POR BRANCH)
    // ============================================================
    suspend fun getInventory(): Result<List<InventoryItem>> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = sessionPreferences.getBranchId()

                if (branchId == null) {
                    Log.e(TAG, "❌ branch_id es null en sesión")
                    return@withContext Result.failure(
                        InventoryError.UnknownError(
                            IllegalStateException("Sucursal no seleccionada")
                        )
                    )
                }

                Log.d(TAG, "📦 Obteniendo inventario para branch_id=$branchId")

                val list = supabase
                    .from(TABLE)
                    .select {
                        filter {
                            eq("branch_id", branchId)
                        }
                    }
                    .decodeList<InventoryItemDTO>()

                Log.d(TAG, "✅ Inventario obtenido: ${list.size} items")

                Result.success(list.map { it.toInventoryItem() })

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getInventory()", e)
                Result.failure(InventoryError.UnknownError(e))
            }
        }

    // ============================================================
    // 2. AUMENTAR STOCK (VALIDA BRANCH)
    // ============================================================
    suspend fun increaseStock(productId: Int, amount: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = sessionPreferences.getBranchId()
                    ?: return@withContext Result.failure(
                        InventoryError.UnknownError(
                            IllegalStateException("Sucursal no seleccionada")
                        )
                    )

                val product = supabase
                    .from(TABLE)
                    .select {
                        filter {
                            eq("product_id", productId)
                            eq("branch_id", branchId)
                        }
                    }
                    .decodeSingleOrNull<InventoryItemDTO>()

                if (product == null) {
                    Log.w(TAG, "⚠️ Producto no encontrado o no pertenece a la sucursal")
                    return@withContext Result.failure(InventoryError.NotFound)
                }

                val newStock = product.stock + amount

                supabase.from(TABLE)
                    .update(mapOf("current_stock" to newStock)) {
                        filter {
                            eq("product_id", productId)
                            eq("branch_id", branchId)
                        }
                    }

                Log.d(TAG, "✅ Stock aumentado correctamente")
                Result.success(true)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error increaseStock()", e)
                Result.failure(InventoryError.UnknownError(e))
            }
        }

    // ============================================================
    // 3. REDUCIR STOCK (VALIDA BRANCH)
    // ============================================================
    suspend fun decreaseStock(productId: Int, amount: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = sessionPreferences.getBranchId()
                    ?: return@withContext Result.failure(
                        InventoryError.UnknownError(
                            IllegalStateException("Sucursal no seleccionada")
                        )
                    )

                val product = supabase
                    .from(TABLE)
                    .select {
                        filter {
                            eq("product_id", productId)
                            eq("branch_id", branchId)
                        }
                    }
                    .decodeSingleOrNull<InventoryItemDTO>()

                if (product == null) {
                    Log.w(TAG, "⚠️ Producto no encontrado o no pertenece a la sucursal")
                    return@withContext Result.failure(InventoryError.NotFound)
                }

                val newStock = (product.stock - amount).coerceAtLeast(0)

                supabase.from(TABLE)
                    .update(mapOf("current_stock" to newStock)) {
                        filter {
                            eq("product_id", productId)
                            eq("branch_id", branchId)
                        }
                    }

                Log.d(TAG, "✅ Stock reducido correctamente")
                Result.success(true)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error decreaseStock()", e)
                Result.failure(InventoryError.UnknownError(e))
            }
        }

    // ============================================================
    // 4. TOKENS FCM
    // ============================================================
    suspend fun getAllFCMTokens(): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val tokens = supabase.from("fcm_tokens")
                    .select()
                    .decodeList<FCMTokenRecord>()

                tokens.map { it.token }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo tokens FCM", e)
                emptyList()
            }
        }

    // ============================================================
    // 5. ENVIAR NOTIFICACIÓN
    // ============================================================
    suspend fun sendStockNotification(
        token: String,
        title: String,
        body: String
    ): Boolean {
        return try {
            val payload = buildJsonObject {
                put("token", token)
                put("title", title)
                put("body", body)
            }

            supabase.functions.invoke(
                function = "send_notifications",
                body = payload
            )

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enviando notificación", e)
            false
        }
    }

    suspend fun sendNotificationToAll(title: String, body: String): Int =
        withContext(Dispatchers.IO) {
            val tokens = getAllFCMTokens()
            var successCount = 0

            tokens.forEach {
                if (sendStockNotification(it, title, body)) {
                    successCount++
                }
            }

            successCount
        }

    // ============================================================
    // DTO → MODELO
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
// MODELO TOKEN FCM
// ============================================================
@Serializable
data class FCMTokenRecord(
    val userId: String,
    val token: String,
    val platform: String,
    val createdAt: String? = null
)
