package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductUpdateDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class SalesProductRepository(
    private val supabase: SupabaseClient,
    private val sessionPreferences: SessionPreferences
) {

    companion object {
        private const val TAG = "SalesProductRepository"
    }

    // ============================================
    // 🔍 SEARCH PRODUCTS BY NAME (BRANCH SAFE)
    // ============================================
    suspend fun searchProductsByName(query: String): Result<List<ProductDTO>> {
        return try {
            val branchId = sessionPreferences.getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            Log.d(TAG, "searchProductsByName → '$query' | branch=$branchId")

            val products = supabase.from("products")
                .select {
                    filter {
                        ilike("name", "%$query%")
                        eq("branch_id", branchId)
                    }
                }
                .decodeList<ProductDTO>()

            Result.success(products)

        } catch (e: Exception) {
            Log.e(TAG, "searchProductsByName error", e)
            Result.failure(e)
        }
    }

    // ============================================
    // 📌 GET PRODUCT BY BARCODE (BRANCH SAFE)
    // ============================================
    suspend fun getProductByBarcode(barcode: String): Result<ProductDTO?> {
        return try {
            val branchId = sessionPreferences.getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            Log.d(TAG, "getProductByBarcode → $barcode | branch=$branchId")

            val products = supabase.from("products")
                .select {
                    filter {
                        eq("barcode", barcode)
                        eq("branch_id", branchId)
                    }
                }
                .decodeList<ProductDTO>()

            Result.success(products.firstOrNull())

        } catch (e: Exception) {
            Log.e(TAG, "getProductByBarcode error", e)
            Result.failure(e)
        }
    }

    // ============================================
    // ⚠️ REDUCE PRODUCT STOCK (BRANCH SAFE)
    // ============================================
    suspend fun reduceStock(productId: Int, quantity: Int): Result<ProductDTO> {
        return try {
            val branchId = sessionPreferences.getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            Log.d(
                TAG,
                "reduceStock → productId=$productId | qty=$quantity | branch=$branchId"
            )

            // Obtener producto de la sucursal actual
            val product = supabase.from("products")
                .select {
                    filter {
                        eq("product_id", productId)
                        eq("branch_id", branchId)
                    }
                }
                .decodeSingle<ProductDTO>()

            val newStock = (product.currentStock - quantity).coerceAtLeast(0)

            val updates = ProductUpdateDTO(
                currentStock = newStock
            )

            val updatedProduct = supabase.from("products")
                .update(updates) {
                    filter {
                        eq("product_id", productId)
                        eq("branch_id", branchId)
                    }
                    select()
                }
                .decodeSingle<ProductDTO>()

            Result.success(updatedProduct)

        } catch (e: Exception) {
            Log.e(TAG, "reduceStock error", e)
            Result.failure(e)
        }
    }
}
