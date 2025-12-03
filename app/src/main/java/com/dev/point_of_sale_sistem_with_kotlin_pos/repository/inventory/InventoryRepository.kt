package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.inventory

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryItem
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryItemDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InventoryRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TABLE = "products"
    }

    // ============================================================
    // 1. OBTENER INVENTARIO (sin categorías, totalmente funcional)
    // ============================================================
    suspend fun getInventory(): Result<List<InventoryItem>> = withContext(Dispatchers.IO) {
        try {
            val query = supabase.from(TABLE)
                .select() // ← compatible con tu versión actual

            val list = query.decodeList<InventoryItemDTO>()

            Result.success(list.map { it.toInventoryItem() })

        } catch (e: Exception) {
            Log.e("INVENTORY", "Error getInventory(): $e")
            Result.failure(InventoryError.UnknownError(e))
        }
    }

    // ============================================================
    // 2. AUMENTAR STOCK — MÉTODO CORRECTO
    // ============================================================
    suspend fun increaseStock(productId: Int, amount: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Obtener producto actual
                val product = supabase.from(TABLE)
                    .select { filter { eq("product_id", productId) } }
                    .decodeSingleOrNull<InventoryItemDTO>()

                if (product == null) {
                    return@withContext Result.failure(InventoryError.NotFound)
                }

                // 2. Calcular nuevo stock
                val newStock = product.stock + amount

                // 3. Actualizar stock
                supabase.from(TABLE)
                    .update(
                        mapOf("current_stock" to newStock)
                    ) {
                        filter { eq("product_id", productId) }
                    }

                Result.success(true)

            } catch (e: Exception) {
                Log.e("INVENTORY", "Error increaseStock(): $e")
                Result.failure(InventoryError.UnknownError(e))
            }
        }

    // ============================================================
    // 3. REDUCIR STOCK — MÉTODO CORRECTO
    // ============================================================
    suspend fun decreaseStock(productId: Int, amount: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Obtener producto actual
                val product = supabase.from(TABLE)
                    .select { filter { eq("product_id", productId) } }
                    .decodeSingleOrNull<InventoryItemDTO>()

                if (product == null) {
                    return@withContext Result.failure(InventoryError.NotFound)
                }

                // 2. Calcular nuevo stock, evita negativos
                val newStock = (product.stock - amount).coerceAtLeast(0)

                // 3. Actualizar stock
                supabase.from(TABLE)
                    .update(
                        mapOf("current_stock" to newStock)
                    ) {
                        filter { eq("product_id", productId) }
                    }

                Result.success(true)

            } catch (e: Exception) {
                Log.e("INVENTORY", "Error decreaseStock(): $e")
                Result.failure(InventoryError.UnknownError(e))
            }
        }

    // ============================================================
    // 4. Mapeo DTO → Modelo Final
    // ============================================================
    private fun InventoryItemDTO.toInventoryItem() = InventoryItem(
        id = id,
        name = name,
        description = description,
        price = price,
        barcode = barcode,
        categoryId = categoryId,
        categoryName = null, // SIN JOIN (lo agregarás después)
        stock = stock,
        minStock = minStock,
        image = image
    )
}
