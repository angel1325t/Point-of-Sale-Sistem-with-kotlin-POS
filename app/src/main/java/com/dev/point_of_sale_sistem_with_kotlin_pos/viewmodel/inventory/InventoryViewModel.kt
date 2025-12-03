package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.inventory.InventoryIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.inventory.InventorySortMode
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.inventory.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repo: InventoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state = _state.asStateFlow()

    init {
        loadInventory()
    }

    fun handleIntent(intent: InventoryIntent) {
        when (intent) {

            is InventoryIntent.LoadInventory -> loadInventory()
            is InventoryIntent.LoadById -> loadItemById(intent.itemId)
            is InventoryIntent.Search -> search(intent.query)
            is InventoryIntent.FilterByCategory -> filterByCategory(intent.categoryId)
            is InventoryIntent.SortBy -> sortBy(intent.mode)
            is InventoryIntent.IncreaseStock -> increaseStock(intent.itemId, intent.amount)
            is InventoryIntent.DecreaseStock -> decreaseStock(intent.itemId, intent.amount)
            is InventoryIntent.SelectItem -> selectItem(intent.itemId)
            is InventoryIntent.ClearError -> clearError()
            is InventoryIntent.ResetState -> resetState()
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repo.getInventory()
                .onSuccess { items ->
                    _state.update {
                        it.copy(
                            items = items,
                            filteredItems = items,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    val err = error as? InventoryError ?: InventoryError.UnknownError(error)
                    _state.update { it.copy(isLoading = false, error = err) }
                }
        }
    }

    private fun loadItemById(id: Int) {
        val item = _state.value.items.find { it.id == id }
        _state.update {
            it.copy(
                selectedItem = item,
                error = if (item == null) InventoryError.NotFound else null
            )
        }
    }

    private fun increaseStock(id: Int, amount: Int) {
        viewModelScope.launch {
            repo.increaseStock(id, amount)
                .onSuccess {
                    _state.update { it.copy(successMessageRes = R.string.success_stock_increased) }
                    loadInventory()
                }
                .onFailure { error ->
                    val err = error as? InventoryError ?: InventoryError.UnknownError(error)
                    _state.update { it.copy(error = err) }
                }
        }
    }

    private fun decreaseStock(id: Int, amount: Int) {
        viewModelScope.launch {
            repo.decreaseStock(id, amount)
                .onSuccess {
                    _state.update { it.copy(successMessageRes = R.string.success_stock_decreased) }
                    loadInventory()
                }
                .onFailure { error ->
                    val err = error as? InventoryError ?: InventoryError.UnknownError(error)
                    _state.update { it.copy(error = err) }
                }
        }
    }

    private fun search(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                filteredItems = it.items.filter { item ->
                    item.name.contains(query, ignoreCase = true) ||
                            (item.categoryName?.contains(query, ignoreCase = true) == true)
                }
            )
        }
    }

    private fun filterByCategory(categoryId: Int?) {
        _state.update { state ->
            state.copy(
                selectedCategoryId = categoryId,
                filteredItems = when (categoryId) {
                    null -> state.items
                    else -> state.items.filter { it.categoryId == categoryId }
                }
            )
        }
    }

    private fun sortBy(mode: InventorySortMode) {
        val sorted = when (mode) {
            InventorySortMode.NAME_ASC -> _state.value.filteredItems.sortedBy { it.name }
            InventorySortMode.NAME_DESC -> _state.value.filteredItems.sortedByDescending { it.name }
            InventorySortMode.STOCK_ASC -> _state.value.filteredItems.sortedBy { it.stock }
            InventorySortMode.STOCK_DESC -> _state.value.filteredItems.sortedByDescending { it.stock }
            InventorySortMode.LOW_STOCK_FIRST -> _state.value.filteredItems.sortedBy { it.stock - it.minStock }
        }

        _state.update { it.copy(filteredItems = sorted, sortMode = mode) }
    }

    private fun selectItem(id: Int?) {
        val item = _state.value.items.find { it.id == id }
        _state.update { it.copy(selectedItem = item) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun resetState() {
        _state.update { InventoryState() }
        loadInventory()
    }
}
