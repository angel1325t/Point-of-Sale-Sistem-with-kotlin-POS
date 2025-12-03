package com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory

import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.inventory.InventorySortMode

data class InventoryState(
    val items: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),

    val isLoading: Boolean = false,

    val error: InventoryError? = null,

    val searchQuery: String = "",
    val selectedCategoryId: Int? = null,
    val sortMode: InventorySortMode = InventorySortMode.NAME_ASC,

    val selectedItem: InventoryItem? = null,

    // Para snackbars exitosos
    val successMessageRes: Int? = null
)
