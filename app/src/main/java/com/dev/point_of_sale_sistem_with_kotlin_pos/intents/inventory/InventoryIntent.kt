package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.inventory

sealed class InventoryIntent {

    // Listado
    data object LoadInventory : InventoryIntent()
    data class LoadById(val itemId: Int) : InventoryIntent()

    // Filtros y búsquedas
    data class Search(val query: String) : InventoryIntent()
    data class FilterByCategory(val categoryId: Int?) : InventoryIntent()
    data class SortBy(val mode: InventorySortMode) : InventoryIntent()

    // Stock
    data class IncreaseStock(val itemId: Int, val amount: Int) : InventoryIntent()
    data class DecreaseStock(val itemId: Int, val amount: Int) : InventoryIntent()

    // UI
    data class SelectItem(val itemId: Int?) : InventoryIntent()
    data object ClearError : InventoryIntent()
    data object ResetState : InventoryIntent()
}

enum class InventorySortMode {
    NAME_ASC,
    NAME_DESC,
    STOCK_ASC,
    STOCK_DESC,
    LOW_STOCK_FIRST
}
