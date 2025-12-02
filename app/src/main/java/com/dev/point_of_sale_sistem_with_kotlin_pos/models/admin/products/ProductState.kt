package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de datos interno que usará la app
 */
@Serializable
data class Product(
    @SerialName("product_id")
    val productId: Int = 0,
    val name: String = "",
    val description: String? = null,
    val price: Double = 0.0,
    val barcode: String? = null,
    @SerialName("category_id")
    val categoryId: Int = 0,
    val categoryName: String? = null, // útil para UI
    val image: String? = null,
    @SerialName("current_stock")
    val currentStock: Int = 0,
    @SerialName("minimum_stock")
    val minimumStock: Int = 0
)

/**
 * DTO para leer desde Supabase (DB → App)
 */
@Serializable
data class ProductDTO(
    @SerialName("product_id")
    val productId: Int = 0,
    val name: String,
    val description: String? = null,
    val price: Double,
    val barcode: String? = null,
    @SerialName("category_id")
    val categoryId: Int,
    val image: String? = null,
    @SerialName("current_stock")
    val currentStock: Int = 0,
    @SerialName("minimum_stock")
    val minimumStock: Int = 0
)

/**
 * DTO para insertar productos
 */
@Serializable
data class ProductInsertDTO(
    val name: String,
    val description: String? = null,
    val price: Double,
    val barcode: String? = null,
    @SerialName("category_id")
    val categoryId: Int,
    val image: String? = null,
    @SerialName("current_stock")
    val currentStock: Int = 0,
    @SerialName("minimum_stock")
    val minimumStock: Int = 0
)

/**
 * DTO para actualizar productos
 */
@Serializable
data class ProductUpdateDTO(
    val name: String,
    val description: String? = null,
    val price: Double,
    val barcode: String? = null,
    @SerialName("category_id")
    val categoryId: Int,
    val image: String? = null,
    @SerialName("current_stock")
    val currentStock: Int = 0,
    @SerialName("minimum_stock")
    val minimumStock: Int = 0
)

/**
 * Estado global del CRUD de productos
 */
data class ProductState(
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: ProductsError? = null,
    val searchQuery: String = "",
    val filterCategoryId: Int? = null,
    val operationSuccess: Boolean = false,
    val successMessage: String? = null,

    // Validaciones
    val nameError: String? = null,
    val priceError: String? = null,
    val stockError: String? = null,
    val barcodeError: String? = null,

    // Paginación
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val itemsPerPage: Int = 20,
    val totalItems: Int = 0
) {

    val hasProducts: Boolean
        get() = products.isNotEmpty()

    val isFiltered: Boolean
        get() = searchQuery.isNotEmpty() || filterCategoryId != null

    val displayProducts: List<Product>
        get() = if (isFiltered) filteredProducts else products

    val isProcessing: Boolean
        get() = isLoading || isRefreshing
}
