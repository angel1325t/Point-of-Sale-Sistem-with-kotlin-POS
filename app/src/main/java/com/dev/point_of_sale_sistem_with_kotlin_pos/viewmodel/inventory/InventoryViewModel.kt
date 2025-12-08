package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.inventory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.inventory.InventoryIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.inventory.SortMode
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryItem
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.inventory.InventoryState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.inventory.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    companion object {
        private const val TAG = "INVENTORY_VM"
    }

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    init {
        Log.d(TAG, "🚀 InventoryViewModel inicializado")
        loadInventory()
    }

    fun handleIntent(intent: InventoryIntent) {
        Log.d(TAG, "📨 Intent recibido: ${intent::class.simpleName}")
        when (intent) {
            is InventoryIntent.LoadInventory -> loadInventory()
            is InventoryIntent.Search -> search(intent.query)
            is InventoryIntent.SortBy -> sortBy(intent.mode)
            is InventoryIntent.IncreaseStock -> increaseStock(intent.productId, intent.amount)
            is InventoryIntent.DecreaseStock -> decreaseStock(intent.productId, intent.amount)
            is InventoryIntent.ClearError -> clearError()
            is InventoryIntent.SendTestNotification -> sendTestNotification()
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            Log.d(TAG, "📦 Cargando inventario...")
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getInventory()
                .onSuccess { items ->
                    Log.d(TAG, "✅ ${items.size} productos cargados")
                    _state.update {
                        it.copy(
                            items = items,
                            filteredItems = applySortAndFilter(items, it.searchQuery, it.sortMode),
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error cargando inventario: ${error.message}")
                    _state.update {
                        it.copy(
                            error = error.message ?: "Error desconocido",
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun search(query: String) {
        Log.d(TAG, "🔍 Buscando: '$query'")
        _state.update {
            it.copy(
                searchQuery = query,
                filteredItems = applySortAndFilter(it.items, query, it.sortMode)
            )
        }
    }

    private fun sortBy(mode: SortMode) {
        Log.d(TAG, "🔀 Ordenando por: $mode")
        _state.update {
            it.copy(
                sortMode = mode,
                filteredItems = applySortAndFilter(it.items, it.searchQuery, mode)
            )
        }
    }

    private fun increaseStock(productId: Int, amount: Int) {
        viewModelScope.launch {
            Log.d(TAG, "📈 Aumentando stock: ID=$productId, amount=$amount")
            _state.update { it.copy(isLoading = true) }

            repository.increaseStock(productId, amount)
                .onSuccess {
                    Log.d(TAG, "✅ Stock aumentado exitosamente")

                    val product = _state.value.items.find { it.id == productId }
                    if (product != null) {
                        sendStockChangeNotification(
                            productName = product.name,
                            newStock = product.stock + amount,
                            isIncrease = true
                        )
                    }

                    loadInventory()
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error aumentando stock: ${error.message}")
                    _state.update {
                        it.copy(
                            error = error.message ?: "Error aumentando stock",
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun decreaseStock(productId: Int, amount: Int) {
        viewModelScope.launch {
            Log.d(TAG, "📉 Reduciendo stock: ID=$productId, amount=$amount")
            _state.update { it.copy(isLoading = true) }

            repository.decreaseStock(productId, amount)
                .onSuccess {
                    Log.d(TAG, "✅ Stock reducido exitosamente")

                    val product = _state.value.items.find { it.id == productId }
                    if (product != null) {
                        val newStock = (product.stock - amount).coerceAtLeast(0)
                        sendStockChangeNotification(
                            productName = product.name,
                            newStock = newStock,
                            isIncrease = false
                        )
                    }

                    loadInventory()
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error reduciendo stock: ${error.message}")
                    _state.update {
                        it.copy(
                            error = error.message ?: "Error reduciendo stock",
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun sendStockChangeNotification(
        productName: String,
        newStock: Int,
        isIncrease: Boolean
    ) {
        viewModelScope.launch {
            val action = if (isIncrease) "aumentó" else "disminuyó"
            val emoji = if (isIncrease) "📦" else "📉"

            val title = "$emoji Stock Actualizado"
            val body = "\"$productName\" $action a $newStock unidades"

            Log.d(TAG, "🔔 Enviando notificación: $title - $body")

            val sentCount = repository.sendNotificationToAll(title, body)

            _state.update {
                it.copy(
                    notificationSent = true,
                    lastNotificationMessage = "✅ Enviada a $sentCount dispositivos"
                )
            }

            kotlinx.coroutines.delay(3000)
            _state.update {
                it.copy(
                    notificationSent = false,
                    lastNotificationMessage = null
                )
            }
        }
    }

    private fun sendTestNotification() {
        viewModelScope.launch {
            Log.d(TAG, "🔔 Enviando notificación de prueba...")
            _state.update { it.copy(isLoading = true) }

            val title = "🔔 Test de Notificación"
            val body = "Esta es una prueba del sistema POS. ¡Funciona correctamente!"

            val sentCount = repository.sendNotificationToAll(title, body)

            _state.update {
                it.copy(
                    isLoading = false,
                    notificationSent = true,
                    lastNotificationMessage = "✅ Prueba enviada a $sentCount dispositivos"
                )
            }

            Log.d(TAG, "✅ Notificación de prueba enviada a $sentCount dispositivos")

            kotlinx.coroutines.delay(5000)
            _state.update {
                it.copy(
                    notificationSent = false,
                    lastNotificationMessage = null
                )
            }
        }
    }

    private fun clearError() {
        Log.d(TAG, "🧹 Limpiando error")
        _state.update { it.copy(error = null) }
    }

    private fun applySortAndFilter(
        items: List<InventoryItem>,
        query: String,
        mode: SortMode
    ): List<InventoryItem> {
        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description?.contains(query, ignoreCase = true) == true ||
                        it.barcode?.contains(query, ignoreCase = true) == true
            }
        }

        return when (mode) {
            SortMode.NAME_ASC -> filtered.sortedBy { it.name }
            SortMode.NAME_DESC -> filtered.sortedByDescending { it.name }
            SortMode.STOCK_ASC -> filtered.sortedBy { it.stock }
            SortMode.STOCK_DESC -> filtered.sortedByDescending { it.stock }
            SortMode.PRICE_ASC -> filtered.sortedBy { it.price }
            SortMode.PRICE_DESC -> filtered.sortedByDescending { it.price }
        }
    }
}