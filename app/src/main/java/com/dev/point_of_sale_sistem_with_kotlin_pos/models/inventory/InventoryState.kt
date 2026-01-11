package com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory

import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.inventory.SortMode

data class InventoryState(
    val items: List<InventoryItem> = emptyList(),
    val filteredItems: List<InventoryItem> = emptyList(),
    val searchQuery: String = "",
    val sortMode: SortMode = SortMode.NAME_ASC,
    val isLoading: Boolean = false,
    val error: String? = null,
    val notificationSent: Boolean = false,
    val lastNotificationMessage: String? = null
)