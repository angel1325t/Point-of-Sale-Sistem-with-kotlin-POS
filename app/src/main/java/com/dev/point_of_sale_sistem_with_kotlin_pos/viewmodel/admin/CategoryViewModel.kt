package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.categories.CategoryIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.CategoryError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.CategoryState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.categories.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar el estado de las categorías usando MVI
 */
class CategoryViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryState())
    val state: StateFlow<CategoryState> = _state.asStateFlow()

    init {
        handleIntent(CategoryIntent.LoadCategories)
    }

    /**
     * Maneja todos los intents del usuario
     */
    fun handleIntent(intent: CategoryIntent) {
        when (intent) {
            is CategoryIntent.LoadCategories -> loadCategories()
            is CategoryIntent.LoadCategoryById -> loadCategoryById(intent.categoryId)
            is CategoryIntent.SearchCategories -> searchCategories(intent.query)
            is CategoryIntent.FilterByParent -> filterByParent(intent.parentId)
            is CategoryIntent.CreateCategory -> createCategory(
                intent.name,
                intent.description,
                intent.parentId
            )
            is CategoryIntent.UpdateCategory -> updateCategory(
                intent.categoryId,
                intent.name,
                intent.description,
                intent.parentId
            )
            is CategoryIntent.DeleteCategory -> deleteCategory(intent.categoryId)
            is CategoryIntent.DeleteMultipleCategories -> deleteMultipleCategories(intent.categoryIds)
            is CategoryIntent.SelectCategory -> selectCategory(intent.categoryId)
            is CategoryIntent.ClearSelection -> clearSelection()
            is CategoryIntent.ClearError -> clearError()
            is CategoryIntent.ResetState -> resetState()
            is CategoryIntent.ValidateCategoryName -> validateCategoryName(intent.name)
        }
    }

    /**
     * Carga todas las categorías
     */
    private fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getAllCategories()
                .onSuccess { categories ->
                    _state.update {
                        it.copy(
                            categories = categories,
                            filteredCategories = categories,
                            isLoading = false,
                            totalItems = categories.size
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? CategoryError ?: CategoryError.UnknownError(
                                exception = error
                            )
                        )
                    }
                }
        }
    }

    /**
     * Carga una categoría específica por ID
     */
    private fun loadCategoryById(categoryId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getCategoryById(categoryId)
                .onSuccess { category ->
                    _state.update {
                        it.copy(
                            selectedCategory = category,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? CategoryError ?: CategoryError.UnknownError(
                                exception = error
                            )
                        )
                    }
                }
        }
    }

    /**
     * Busca categorías por nombre
     */
    private fun searchCategories(query: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    searchQuery = query,
                    isLoading = true,
                    error = null
                )
            }

            if (query.isBlank()) {
                _state.update {
                    it.copy(
                        filteredCategories = it.categories,
                        isLoading = false
                    )
                }
                return@launch
            }

            repository.searchCategories(query)
                .onSuccess { categories ->
                    _state.update {
                        it.copy(
                            filteredCategories = categories,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? CategoryError ?: CategoryError.UnknownError(
                                exception = error
                            )
                        )
                    }
                }
        }
    }

    /**
     * Filtra categorías por ID de padre
     */
    private fun filterByParent(parentId: Int?) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    filterParentId = parentId,
                    isLoading = true,
                    error = null
                )
            }

            repository.getCategoriesByParentId(parentId)
                .onSuccess { categories ->
                    _state.update {
                        it.copy(
                            filteredCategories = categories,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? CategoryError ?: CategoryError.UnknownError(
                                exception = error
                            )
                        )
                    }
                }
        }
    }

    /**
     * Crea una nueva categoría
     */
    private fun createCategory(name: String, description: String?, parentId: Int?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.createCategory(name, description, parentId)
                .onSuccess { category ->
                    _state.update {
                        it.copy(
                            categories = it.categories + category,
                            filteredCategories = it.filteredCategories + category,
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Categoría creada exitosamente",
                            selectedCategory = null,
                            nameError = null,
                            descriptionError = null
                        )
                    }
                    // Recargar para obtener datos actualizados
                    loadCategories()
                }
                .onFailure { error ->
                    val categoryError = error as? CategoryError ?: CategoryError.UnknownError(
                        exception = error
                    )
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = categoryError,
                            operationSuccess = false
                        )
                    }
                }
        }
    }

    /**
     * Actualiza una categoría existente
     */
    private fun updateCategory(
        categoryId: Int,
        name: String,
        description: String?,
        parentId: Int?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.updateCategory(categoryId, name, description, parentId)
                .onSuccess { category ->
                    _state.update {
                        it.copy(
                            categories = it.categories.map { cat ->
                                if (cat.categoryId == categoryId) category else cat
                            },
                            filteredCategories = it.filteredCategories.map { cat ->
                                if (cat.categoryId == categoryId) category else cat
                            },
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Categoría actualizada exitosamente",
                            selectedCategory = null,
                            nameError = null,
                            descriptionError = null
                        )
                    }
                    // Recargar para obtener datos actualizados
                    loadCategories()
                }
                .onFailure { error ->
                    val categoryError = error as? CategoryError ?: CategoryError.UnknownError(
                        exception = error
                    )
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = categoryError,
                            operationSuccess = false
                        )
                    }
                }
        }
    }

    /**
     * Elimina una categoría
     */
    private fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.deleteCategory(categoryId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            categories = it.categories.filter { cat -> cat.categoryId != categoryId },
                            filteredCategories = it.filteredCategories.filter { cat ->
                                cat.categoryId != categoryId
                            },
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Categoría eliminada exitosamente",
                            selectedCategory = null
                        )
                    }
                }
                .onFailure { error ->
                    val categoryError = error as? CategoryError ?: CategoryError.UnknownError(
                        exception = error
                    )
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = categoryError,
                            operationSuccess = false
                        )
                    }
                }
        }
    }

    /**
     * Elimina múltiples categorías
     */
    private fun deleteMultipleCategories(categoryIds: List<Int>) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.deleteMultipleCategories(categoryIds)
                .onSuccess { deletedCount ->
                    _state.update {
                        it.copy(
                            categories = it.categories.filter { cat ->
                                cat.categoryId !in categoryIds
                            },
                            filteredCategories = it.filteredCategories.filter { cat ->
                                cat.categoryId !in categoryIds
                            },
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Se eliminaron $deletedCount categorías",
                            selectedCategory = null
                        )
                    }
                }
                .onFailure { error ->
                    val categoryError = error as? CategoryError ?: CategoryError.UnknownError(
                        exception = error
                    )
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = categoryError,
                            operationSuccess = false
                        )
                    }
                }
        }
    }

    /**
     * Selecciona una categoría
     */
    private fun selectCategory(categoryId: Int?) {
        if (categoryId == null) {
            clearSelection()
            return
        }

        val category = _state.value.categories.find { it.categoryId == categoryId }
        _state.update { it.copy(selectedCategory = category) }
    }

    /**
     * Limpia la selección actual
     */
    private fun clearSelection() {
        _state.update {
            it.copy(
                selectedCategory = null,
                nameError = null,
                descriptionError = null
            )
        }
    }

    /**
     * Limpia el error actual
     */
    private fun clearError() {
        _state.update {
            it.copy(
                error = null,
                operationSuccess = false,
                successMessage = null
            )
        }
    }

    /**
     * Resetea el estado completo
     */
    private fun resetState() {
        _state.update { CategoryState() }
        loadCategories()
    }

    /**
     * Valida el nombre de la categoría
     */
    private fun validateCategoryName(name: String) {
        val error = when {
            name.isBlank() -> "El nombre no puede estar vacío"
            name.length < 2 -> "El nombre debe tener al menos 2 caracteres"
            name.length > 100 -> "El nombre no puede exceder 100 caracteres"
            else -> null
        }
        _state.update { it.copy(nameError = error) }
    }
}