package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.categories

sealed class CategoryIntent {
    // Acciones de listado y consulta
    data object LoadCategories : CategoryIntent()
    data class LoadCategoryById(val categoryId: Int) : CategoryIntent()
    data class SearchCategories(val query: String) : CategoryIntent()
    data class FilterByParent(val parentId: Int?) : CategoryIntent()

    // Acciones de creación
    data class CreateCategory(
        val name: String,
        val description: String?,
        val parentId: Int?
    ) : CategoryIntent()

    // Acciones de actualización
    data class UpdateCategory(
        val categoryId: Int,
        val name: String,
        val description: String?,
        val parentId: Int?
    ) : CategoryIntent()

    // Acciones de eliminación
    data class DeleteCategory(val categoryId: Int) : CategoryIntent()
    data class DeleteMultipleCategories(val categoryIds: List<Int>) : CategoryIntent()

    // Acciones de UI
    data class SelectCategory(val categoryId: Int?) : CategoryIntent()
    data object ClearSelection : CategoryIntent()
    data object ClearError : CategoryIntent()
    data object ResetState : CategoryIntent()

    // Validaciones
    data class ValidateCategoryName(val name: String) : CategoryIntent()
}