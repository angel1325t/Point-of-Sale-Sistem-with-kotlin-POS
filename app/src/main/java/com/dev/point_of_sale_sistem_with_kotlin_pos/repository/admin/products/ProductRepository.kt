package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.products

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Objects.isNull

class ProductsRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TABLE_NAME = "products"
    }

    /**
     * Obtiene todos los productos
     */
    suspend fun getAllProducts(): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.from(TABLE_NAME)
                .select()
                .decodeList<ProductDTO>()

            Result.success(response.map { it.toProduct() })

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Obtiene un producto por ID
     */
    suspend fun getProductById(productId: Int): Result<Product> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter { eq("product_id", productId) }
                }
                .decodeSingleOrNull<ProductDTO>()

            response?.let {
                Result.success(it.toProduct())
            } ?: Result.failure(ProductsError.RecordNotFound())

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Busca productos por nombre
     */
    suspend fun searchProducts(query: String): Result<List<Product>> =
        withContext(Dispatchers.IO) {
            try {
                val response = supabase.from(TABLE_NAME)
                    .select {
                        filter { ilike("name", "%$query%") }
                    }
                    .decodeList<ProductDTO>()

                Result.success(response.map { it.toProduct() })

            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    /**
     * Filtrar productos por categoría
     */
    suspend fun getProductsByCategoryId(categoryId: Int?): Result<List<Product>> =
        withContext(Dispatchers.IO) {
            try {
                val response = if (categoryId == null) {
                    supabase.from(TABLE_NAME)
                        .select {
                            filter { isNull("category_id") }
                        }
                        .decodeList<ProductDTO>()
                } else {
                    supabase.from(TABLE_NAME)
                        .select {
                            filter { eq("category_id", categoryId) }
                        }
                        .decodeList<ProductDTO>()
                }

                Result.success(response.map { it.toProduct() })

            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    /**
     * Crear producto
     */
    suspend fun createProduct(
        name: String,
        description: String?,
        price: Double,
        barcode: String?,
        categoryId: Int,
        image: String?,
        currentStock: Int,
        minimumStock: Int
    ): Result<Product> = withContext(Dispatchers.IO) {
        try {
            // Validaciones básicas
            if (name.isBlank()) {
                return@withContext Result.failure(
                    ProductsError.ValidationError(
                        message = "El nombre no puede estar vacío",
                        field = "name"
                    )
                )
            }

            if (price <= 0) {
                return@withContext Result.failure(ProductsError.InvalidPriceError())
            }

            if (currentStock < 0 || minimumStock < 0) {
                return@withContext Result.failure(ProductsError.InvalidStockError())
            }

            // Validar categoría existente
            val categoryExists = supabase.from("categories")
                .select { filter { eq("category_id", categoryId) } }
                .decodeSingleOrNull<JsonObject>() != null

            if (!categoryExists) {
                return@withContext Result.failure(ProductsError.CategoryNotFoundError())
            }

            // Duplicado nombre
            val duplicateName = supabase.from(TABLE_NAME)
                .select { filter { eq("name", name.trim()) } }
                .decodeSingleOrNull<ProductDTO>()

            if (duplicateName != null) {
                return@withContext Result.failure(ProductsError.DuplicateNameError())
            }

            // Duplicado barcode
            if (!barcode.isNullOrBlank()) {
                val duplicateBarcode = supabase.from(TABLE_NAME)
                    .select { filter { eq("barcode", barcode.trim()) } }
                    .decodeSingleOrNull<ProductDTO>()

                if (duplicateBarcode != null) {
                    return@withContext Result.failure(ProductsError.DuplicateBarcodeError())
                }
            }

            val newProduct = ProductInsertDTO(
                name = name.trim(),
                description = description?.trim(),
                price = price,
                barcode = barcode?.trim(),
                categoryId = categoryId,
                image = image,
                currentStock = currentStock,
                minimumStock = minimumStock
            )

            val response = supabase.from(TABLE_NAME)
                .insert(newProduct) {
                    select()
                }
                .decodeSingle<ProductDTO>()

            Result.success(response.toProduct())

        } catch (e: Exception) {
            Log.e("CREATE_PRODUCT", "Error: $e")
            Result.failure(handleException(e))
        }
    }

    /**
     * Actualiza un producto
     */
    suspend fun updateProduct(
        productId: Int,
        name: String,
        description: String?,
        price: Double,
        barcode: String?,
        categoryId: Int,
        image: String?,
        currentStock: Int,
        minimumStock: Int
    ): Result<Product> = withContext(Dispatchers.IO) {
        try {
            // Validaciones
            if (name.isBlank()) {
                return@withContext Result.failure(
                    ProductsError.ValidationError(
                        message = "El nombre no puede estar vacío",
                        field = "name"
                    )
                )
            }

            if (price <= 0) {
                return@withContext Result.failure(ProductsError.InvalidPriceError())
            }

            if (currentStock < 0 || minimumStock < 0) {
                return@withContext Result.failure(ProductsError.InvalidStockError())
            }

            // Validar categoría
            val categoryExists = supabase.from("categories")
                .select { filter { eq("category_id", categoryId) } }
                .decodeSingleOrNull<JsonObject>() != null

            if (!categoryExists) {
                return@withContext Result.failure(ProductsError.CategoryNotFoundError())
            }

            // Validar nombre duplicado (excluyendo el mismo producto)
            val existingNames = supabase.from(TABLE_NAME)
                .select { filter { eq("name", name.trim()) } }
                .decodeList<ProductDTO>()

            if (existingNames.any { it.productId != productId }) {
                return@withContext Result.failure(ProductsError.DuplicateNameError())
            }

            // Validar barcode duplicado (si existe y no es el mismo)
            if (!barcode.isNullOrBlank()) {
                val existingBarcodes = supabase.from(TABLE_NAME)
                    .select { filter { eq("barcode", barcode.trim()) } }
                    .decodeList<ProductDTO>()

                if (existingBarcodes.any { it.productId != productId }) {
                    return@withContext Result.failure(ProductsError.DuplicateBarcodeError())
                }
            }

            val updatedData = ProductUpdateDTO(
                name = name.trim(),
                description = description?.trim(),
                price = price,
                barcode = barcode?.trim(),
                categoryId = categoryId,
                image = image,
                currentStock = currentStock,
                minimumStock = minimumStock
            )

            val response = supabase.from(TABLE_NAME)
                .update(updatedData) {
                    filter { eq("product_id", productId) }
                    select()
                }
                .decodeSingleOrNull<ProductDTO>()

            response?.let {
                Result.success(it.toProduct())
            } ?: Result.failure(ProductsError.RecordNotFound())

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Elimina un producto
     */
    suspend fun deleteProduct(productId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val product = supabase.from(TABLE_NAME)
                .select { filter { eq("product_id", productId) } }
                .decodeSingleOrNull<ProductDTO>()

            product ?: return@withContext Result.failure(ProductsError.RecordNotFound())

            if (product.currentStock > 0) {
                return@withContext Result.failure(
                    ProductsError.CannotDeleteProductWithStock(
                        currentStock = product.currentStock
                    )
                )
            }

            supabase.from(TABLE_NAME)
                .delete { filter { eq("product_id", productId) } }

            Result.success(true)

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Elimina múltiples productos
     */
    suspend fun deleteMultipleProducts(productIds: List<Int>): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                var deletedCount = 0

                productIds.forEach { id ->
                    val result = deleteProduct(id)
                    if (result.isSuccess) deletedCount++
                }

                Result.success(deletedCount)

            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    /**
     * Manejo centralizado de errores
     */
    private fun handleException(e: Exception): ProductsError {
        return when {
            e.message?.contains("network", ignoreCase = true) == true ->
                ProductsError.NetworkError()

            e.message?.contains("timeout", ignoreCase = true) == true ->
                ProductsError.TimeoutError()

            e.message?.contains("unauthorized", ignoreCase = true) == true ->
                ProductsError.UnauthorizedError()

            e.message?.contains("not found", ignoreCase = true) == true ->
                ProductsError.RecordNotFound()

            else -> ProductsError.UnknownError(exception = e)
        }
    }

    /**
     * Mapper: DTO → Modelo interno
     */
    private fun ProductDTO.toProduct() = Product(
        productId = productId,
        name = name,
        description = description,
        price = price,
        barcode = barcode,
        categoryId = categoryId,
        categoryName = null,
        image = image,
        currentStock = currentStock,
        minimumStock = minimumStock
    )
}

annotation class ProductRepository
