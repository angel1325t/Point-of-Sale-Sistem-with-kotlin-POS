package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders

import android.content.Context
import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.utils.NetworkUtils
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineProductCacheEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductUpdateDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.ProductNameDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class SalesProductRepository(
    private val context: Context,
    private val supabase: SupabaseClient,
    private val sessionPreferences: SessionPreferences
) {

    companion object {
        private const val TAG = "SalesProductRepository"
    }

    private val offlineDb = OfflineDatabase.getInstance(context)

    private val isOnline: Boolean
        get() = try {
            NetworkUtils.isOnline(context)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking network status", e)
            false
        }

    private suspend fun getBranchId(): String? {
        return try {
            sessionPreferences.getBranchId()
        } catch (e: Exception) {
            Log.w(TAG, "Error getting branch ID", e)
            null
        }
    }

    suspend fun searchProductsByName(query: String): Result<List<ProductDTO>> {
        return try {
            val branchId = getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            Log.d(TAG, "searchProductsByName → '$query' | branch=$branchId | online=$isOnline")

            if (isOnline) {
                val products = supabase.from("products")
                    .select {
                        filter {
                            ilike("name", "%$query%")
                            eq("branch_id", branchId)
                        }
                    }
                    .decodeList<ProductDTO>()

                val cacheEntities = products.map { it.toCacheEntity(branchId) }
                offlineDb.productCacheDao().deleteByBranch(branchId)
                offlineDb.productCacheDao().insertAll(cacheEntities)

                Result.success(products)
            } else {
                val cachedProducts = offlineDb.productCacheDao().searchByName(branchId, query)
                val products = cachedProducts.map { it.toProductDTO() }
                Log.d(TAG, "Offline search found ${products.size} products")
                Result.success(products)
            }

        } catch (e: Exception) {
            Log.e(TAG, "searchProductsByName error", e)
            Result.failure(e)
        }
    }

    suspend fun getProductByBarcode(barcode: String): Result<ProductDTO?> {
        return try {
            val branchId = getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            Log.d(TAG, "getProductByBarcode → $barcode | branch=$branchId | online=$isOnline")

            if (isOnline) {
                val products = supabase.from("products")
                    .select {
                        filter {
                            eq("barcode", barcode)
                            eq("branch_id", branchId)
                        }
                    }
                    .decodeList<ProductDTO>()

                val product = products.firstOrNull()
                product?.let {
                    offlineDb.productCacheDao().insert(it.toCacheEntity(branchId))
                }
                Result.success(product)
            } else {
                val cachedProduct = offlineDb.productCacheDao().getByBarcode(branchId, barcode)
                Result.success(cachedProduct?.toProductDTO())
            }

        } catch (e: Exception) {
            Log.e(TAG, "getProductByBarcode error", e)
            Result.failure(e)
        }
    }

    suspend fun getProductName(productId: String): String? {
        return try {
            if (isOnline) {
                val product = supabase.from("products")
                    .select {
                        filter {
                            eq("product_id", productId)
                        }
                    }
                    .decodeSingleOrNull<ProductNameDTO>()
                product?.name
            } else {
                val productIdInt = productId.toIntOrNull() ?: return null
                offlineDb.productCacheDao().getById(productIdInt)?.name
            }
        } catch (e: Exception) {
            Log.e("SalesProductRepository", "Error getting product name", e)
            null
        }
    }

    suspend fun reduceStock(productId: Int, quantity: Int): Result<ProductDTO> {
        return try {
            if (!isOnline) {
                offlineDb.productCacheDao().reduceStock(productId, quantity)
                val cached = offlineDb.productCacheDao().getById(productId)
                if (cached != null) {
                    return Result.success(cached.toProductDTO())
                }
                return Result.failure(Exception("Producto no encontrado en cache"))
            }

            val branchId = getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            Log.d(TAG, "reduceStock → productId=$productId | qty=$quantity | branch=$branchId")

            val product = supabase.from("products")
                .select {
                    filter {
                        eq("product_id", productId)
                        eq("branch_id", branchId)
                    }
                }
                .decodeSingle<ProductDTO>()

            val newStock = (product.currentStock - quantity).coerceAtLeast(0)

            val updates = ProductUpdateDTO(currentStock = newStock)

            val updatedProduct = supabase.from("products")
                .update(updates) {
                    filter {
                        eq("product_id", productId)
                        eq("branch_id", branchId)
                    }
                    select()
                }
                .decodeSingle<ProductDTO>()

            offlineDb.productCacheDao().reduceStock(productId, quantity)

            Result.success(updatedProduct)

        } catch (e: Exception) {
            Log.e(TAG, "reduceStock error", e)
            Result.failure(e)
        }
    }

    suspend fun syncProductsFromOnline() {
        if (!isOnline) return

        try {
            val branchId = getBranchId() ?: return
            val products = supabase.from("products")
                .select { filter { eq("branch_id", branchId) } }
                .decodeList<ProductDTO>()

            val cacheEntities = products.map { it.toCacheEntity(branchId) }
            offlineDb.productCacheDao().deleteByBranch(branchId)
            offlineDb.productCacheDao().insertAll(cacheEntities)
            Log.d(TAG, "Synced ${products.size} products to offline cache")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing products", e)
        }
    }

    private fun ProductDTO.toCacheEntity(branchId: String) = OfflineProductCacheEntity(
        productId = productId,
        name = name,
        barcode = barcode,
        price = price,
        currentStock = currentStock,
        categoryId = categoryId,
        branchId = branchId
    )

    private fun OfflineProductCacheEntity.toProductDTO() = ProductDTO(
        productId = productId,
        name = name,
        barcode = barcode,
        price = price,
        currentStock = currentStock,
        categoryId = categoryId,
        branchId = branchId,
        minimumStock = 0
    )
}
