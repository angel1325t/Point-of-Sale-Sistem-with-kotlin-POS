package com.dev.point_of_sale_sistem_with_kotlin_pos.intents.inventory

sealed class InventoryIntent {
    object LoadInventory : InventoryIntent()
    data class Search(val query: String) : InventoryIntent()
    data class SortBy(val mode: SortMode) : InventoryIntent()
    data class IncreaseStock(val productId: Int, val amount: Int) : InventoryIntent()
    data class DecreaseStock(val productId: Int, val amount: Int) : InventoryIntent()
    object ClearError : InventoryIntent()
    object SendTestNotification : InventoryIntent()
}