package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductUpdateDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class SalesProductRepository(
    private val supabase: SupabaseClient
) {

    companion object {
        private const val TAG = "SalesProductRepository"
    }

    // ============================================
    // 🔍 SEARCH BY NAME
    // ============================================
    suspend fun searchProductsByName(query: String): Result<List<ProductDTO>> {
        return try {
            Log.d(TAG, "searchProductsByName: Buscando → '$query'")

            val products = supabase.from("products")
                .select { filter { ilike("name", "%$query%") } }
                .decodeList<ProductDTO>()

            Result.success(products)

        } catch (e: Exception) {
            Log.e(TAG, "searchProductsByName error", e)
            Result.failure(e)
        }
    }

    // ============================================
    // 📌 GET PRODUCT BY BARCODE
    // ============================================
    suspend fun getProductByBarcode(barcode: String): Result<ProductDTO?> {
        return try {
            Log.d(TAG, "getProductByBarcode: $barcode")

            val products = supabase.from("products")
                .select { filter { eq("barcode", barcode) } }
                .decodeList<ProductDTO>()

            Result.success(products.firstOrNull())

        } catch (e: Exception) {
            Log.e(TAG, "getProductByBarcode error", e)
            Result.failure(e)
        }
    }

    // ============================================
    // ⚠️ REDUCE STOCK
    // ============================================
    suspend fun reduceStock(productId: Int, quantity: Int): Result<ProductDTO> {
        return try {
            Log.d(TAG, "reduceStock: Restando $quantity a productId: $productId")

            // Obtener producto actual
            val product = supabase.from("products")
                .select { filter { eq("product_id", productId) } }
                .decodeSingle<ProductDTO>()

            val newStock = (product.currentStock - quantity).coerceAtLeast(0)

            val updates = ProductUpdateDTO(currentStock = newStock)

            val updated = supabase.from("products")
                .update(updates) {
                    filter { eq("product_id", productId) }
                    select()
                }
                .decodeSingle<ProductDTO>()

            Result.success(updated)

        } catch (e: Exception) {
            Log.e(TAG, "reduceStock error", e)
            Result.failure(e)
        }
    }
}
