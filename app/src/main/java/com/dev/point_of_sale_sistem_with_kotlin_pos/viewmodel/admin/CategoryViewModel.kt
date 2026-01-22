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

class CategoryViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryState())
    val state: StateFlow<CategoryState> = _state.asStateFlow()

    init {
        handleIntent(CategoryIntent.LoadCategories)
    }

    fun handleIntent(intent: CategoryIntent) {
        when (intent) {
            is CategoryIntent.LoadCategories -> loadCategories()
            is CategoryIntent.LoadCategoryById -> loadCategoryById(intent.categoryId)
            is CategoryIntent.SearchCategories -> searchCategories(intent.query)
            is CategoryIntent.FilterByParent -> filterByParent(intent.parentId)
            is CategoryIntent.CreateCategory ->
                createCategory(intent.name, intent.description, intent.parentId)
            is CategoryIntent.UpdateCategory ->
                updateCategory(intent.categoryId, intent.name, intent.description, intent.parentId)
            is CategoryIntent.DeleteCategory -> deleteCategory(intent.categoryId)
            is CategoryIntent.DeleteMultipleCategories ->
                deleteMultipleCategories(intent.categoryIds)
            is CategoryIntent.SelectCategory -> selectCategory(intent.categoryId)
            is CategoryIntent.ClearSelection -> clearSelection()
            is CategoryIntent.ClearError -> clearError()
            is CategoryIntent.ResetState -> resetState()
            is CategoryIntent.ValidateCategoryName -> validateCategoryName(intent.name)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getAllCategories()
                .onSuccess { categories ->
                    _state.update {
                        it.copy(
                            categories = categories,
                            filteredCategories = categories,
                            totalItems = categories.size,
                            isLoading = false
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(isLoading = false, error = it as? CategoryError)
                    }
                }
        }
    }

    private fun loadCategoryById(categoryId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getCategoryById(categoryId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(selectedCategory = it, isLoading = false)
                    }
                }
                .onFailure {
                    _state.update { state ->
                        state.copy(isLoading = false, error = it as? CategoryError)
                    }
                }
        }
    }

    private fun searchCategories(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(searchQuery = query, isLoading = true) }

            if (query.isBlank()) {
                _state.update {
                    it.copy(filteredCategories = it.categories, isLoading = false)
                }
                return@launch
            }

            repository.searchCategories(query)
                .onSuccess {
                    _state.update { state ->
                        state.copy(filteredCategories = it, isLoading = false)
                    }
                }
                .onFailure {
                    _state.update { state ->
                        state.copy(isLoading = false, error = it as? CategoryError)
                    }
                }
        }
    }

    private fun filterByParent(parentId: Int?) {
        viewModelScope.launch {
            _state.update { it.copy(filterParentId = parentId, isLoading = true) }

            repository.getCategoriesByParentId(parentId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(filteredCategories = it, isLoading = false)
                    }
                }
                .onFailure {
                    _state.update { state ->
                        state.copy(isLoading = false, error = it as? CategoryError)
                    }
                }
        }
    }

    private fun createCategory(name: String, description: String?, parentId: Int?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.createCategory(name, description, parentId)
                .onSuccess {
                    loadCategories()
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Categoría creada exitosamente"
                        )
                    }
                }
                .onFailure {
                    _state.update { state ->
                        state.copy(isLoading = false, error = it as? CategoryError)
                    }
                }
        }
    }

    private fun updateCategory(
        categoryId: Int,
        name: String,
        description: String?,
        parentId: Int?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.updateCategory(categoryId, name, description, parentId)
                .onSuccess {
                    loadCategories()
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Categoría actualizada exitosamente"
                        )
                    }
                }
                .onFailure {
                    _state.update { state ->
                        state.copy(isLoading = false, error = it as? CategoryError)
                    }
                }
        }
    }

    private fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.deleteCategory(categoryId)
                .onSuccess {
                    loadCategories()
                }
                .onFailure {
                    _state.update { state ->
                        state.copy(isLoading = false, error = it as? CategoryError)
                    }
                }
        }
    }

    private fun deleteMultipleCategories(categoryIds: List<Int>) {
        viewModelScope.launch {
            repository.deleteMultipleCategories(categoryIds)
            loadCategories()
        }
    }

    private fun selectCategory(categoryId: Int?) {
        val category = _state.value.categories.find { it.categoryId == categoryId }
        _state.update { it.copy(selectedCategory = category) }
    }

    private fun clearSelection() {
        _state.update { it.copy(selectedCategory = null) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null, successMessage = null) }
    }

    private fun resetState() {
        _state.update { CategoryState() }
        loadCategories()
    }

    private fun validateCategoryName(name: String) {
        val error = when {
            name.isBlank() -> "El nombre no puede estar vacío"
            name.length < 2 -> "Debe tener al menos 2 caracteres"
            else -> null
        }
        _state.update { it.copy(nameError = error) }
    }
}
