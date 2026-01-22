package com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de datos que usará la app internamente
 */
@Serializable
data class Category(
    @SerialName("category_id")
    val categoryId: Int = 0,
    val name: String = "",
    val description: String? = null,
    @SerialName("parent_id")
    val parentId: Int? = null,
    val parentName: String? = null,

    // 🔥 NUEVO
    @SerialName("company_id")
    val companyId: String,

    @SerialName("branch_id")
    val branchId: String
)

/**
 * DTO para leer desde Supabase (DB → App)
 */
@Serializable
data class CategoryDTO(
    @SerialName("category_id")
    val categoryId: Int = 0,
    val name: String,
    val description: String? = null,
    @SerialName("parent_id")
    val parentId: Int? = null,

    // 🔥 NUEVO
    @SerialName("company_id")
    val companyId: String,

    @SerialName("branch_id")
    val branchId: String
)


/**
 * DTO para insertar categorías
 */
@Serializable
data class CategoryInsertDTO(
    val name: String,
    val description: String? = null,
    @SerialName("parent_id")
    val parentId: Int? = null,

    // 🔥 CLAVE
    @SerialName("company_id")
    val companyId: String,

    @SerialName("branch_id")
    val branchId: String
)

/**
 * DTO para actualizar categorías
 */
@Serializable
data class CategoryUpdateDTO(
    val name: String,
    val description: String? = null,
    @SerialName("parent_id")
    val parentId: Int? = null
)

/**
 * Estado global del CRUD de categorías
 */
data class CategoryState(
    val categories: List<Category> = emptyList(),
    val filteredCategories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: CategoryError? = null,
    val searchQuery: String = "",
    val filterParentId: Int? = null,
    val operationSuccess: Boolean = false,
    val successMessage: String? = null,

    // Validaciones
    val nameError: String? = null,
    val descriptionError: String? = null,

    // Paginación
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val itemsPerPage: Int = 20,
    val totalItems: Int = 0
) {
    val hasCategories: Boolean
        get() = categories.isNotEmpty()

    val isFiltered: Boolean
        get() = searchQuery.isNotEmpty() || filterParentId != null

    val displayCategories: List<Category>
        get() = if (isFiltered) filteredCategories else categories

    val isProcessing: Boolean
        get() = isLoading || isRefreshing
}
