package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.purchase_orders.PurchaseOrderIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.purchase_orders.PurchaseOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PurchaseOrderViewModel(
    private val repository: PurchaseOrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseOrderState())
    val state: StateFlow<PurchaseOrderState> = _state.asStateFlow()

    init {
        handleIntent(PurchaseOrderIntent.LoadOrders)
    }

    // ───────────────────────────────────────────────
    // MANEJO DE INTENTS
    // ───────────────────────────────────────────────

    fun handleIntent(intent: PurchaseOrderIntent) {
        when (intent) {
            is PurchaseOrderIntent.LoadOrders -> loadOrders()
            is PurchaseOrderIntent.LoadOrderById -> loadOrderById(intent.orderId)
            is PurchaseOrderIntent.SearchOrders -> searchOrders(intent.query)
            is PurchaseOrderIntent.FilterByStatus -> filterByStatus(intent.status)
            is PurchaseOrderIntent.FilterBySupplier -> filterBySupplier(intent.supplierId)
            is PurchaseOrderIntent.LoadPendingOrders -> loadPendingOrders()

            is PurchaseOrderIntent.CreateOrder -> createOrder(
                supplierId = intent.supplierId,
                productId = intent.productId,
                quantity = intent.quantity,
                orderDate = intent.orderDate,
                notes = intent.notes
            )

            is PurchaseOrderIntent.MarkAsReceived -> markAsReceived(
                orderId = intent.orderId,
                receivedDate = intent.receivedDate
            )
            is PurchaseOrderIntent.UpdateNotes -> updateNotes(
                orderId = intent.orderId,
                notes = intent.notes
            )

            is PurchaseOrderIntent.DeleteOrder -> deleteOrder(intent.orderId)
            is PurchaseOrderIntent.CancelOrder -> cancelOrder(intent.orderId)

            is PurchaseOrderIntent.SelectOrder -> selectOrder(intent.orderId)
            is PurchaseOrderIntent.ClearSelection -> clearSelection()
            is PurchaseOrderIntent.ClearError -> clearError()
            is PurchaseOrderIntent.ResetState -> resetState()

            is PurchaseOrderIntent.ValidateQuantity -> validateQuantity(intent.quantity)
            is PurchaseOrderIntent.ValidateSupplier -> validateSupplier(intent.supplierId)
            is PurchaseOrderIntent.ValidateProduct -> validateProduct(intent.productId)
        }
    }

    // ───────────────────────────────────────────────
    // OPERACIONES CRUD
    // ───────────────────────────────────────────────

    private fun loadOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getAllOrders()
                .onSuccess { orders ->
                    _state.update {
                        it.copy(
                            orders = orders,
                            filteredOrders = orders,
                            isLoading = false,
                            totalItems = orders.size
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? PurchaseOrderError
                                ?: PurchaseOrderError.UnknownError(cause = error)
                        )
                    }
                }
        }
    }

    private fun loadOrderById(orderId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getOrderById(orderId)
                .onSuccess { order ->
                    _state.update {
                        it.copy(
                            selectedOrder = order,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? PurchaseOrderError
                                ?: PurchaseOrderError.UnknownError(cause = error)
                        )
                    }
                }
        }
    }

    private fun searchOrders(query: String) {
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
                        filteredOrders = it.orders,
                        isLoading = false
                    )
                }
                return@launch
            }

            repository.searchOrders(query)
                .onSuccess { orders ->
                    _state.update {
                        it.copy(
                            filteredOrders = orders,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? PurchaseOrderError
                                ?: PurchaseOrderError.UnknownError(cause = error)
                        )
                    }
                }
        }
    }

    private fun filterByStatus(status: OrderStatus?) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    filterStatus = status,
                    isLoading = true,
                    error = null
                )
            }

            if (status == null) {
                _state.update {
                    it.copy(
                        filteredOrders = it.orders,
                        isLoading = false
                    )
                }
                return@launch
            }

            repository.filterByStatus(status)
                .onSuccess { orders ->
                    _state.update {
                        it.copy(
                            filteredOrders = orders,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? PurchaseOrderError
                                ?: PurchaseOrderError.UnknownError(cause = error)
                        )
                    }
                }
        }
    }

    private fun filterBySupplier(supplierId: Int?) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    filterSupplierId = supplierId,
                    isLoading = false
                )
            }

            val filtered = if (supplierId == null) {
                state.value.orders
            } else {
                state.value.orders.filter { it.supplierId == supplierId }
            }

            _state.update { it.copy(filteredOrders = filtered) }
        }
    }

    private fun loadPendingOrders() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.filterByStatus(OrderStatus.PENDING)
                .onSuccess { orders ->
                    _state.update {
                        it.copy(
                            filteredOrders = orders,
                            filterStatus = OrderStatus.PENDING,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? PurchaseOrderError
                                ?: PurchaseOrderError.UnknownError(cause = error)
                        )
                    }
                }
        }
    }

    private fun createOrder(
        supplierId: Int,
        productId: Int,
        quantity: Int,
        orderDate: String,
        notes: String?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.createOrder(supplierId, productId, quantity, orderDate, notes)
                .onSuccess { order ->
                    _state.update {
                        it.copy(
                            orders = it.orders + order,
                            filteredOrders = it.filteredOrders + order,
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Pedido creado exitosamente",
                            selectedOrder = null,
                            quantityError = null,
                            supplierError = null,
                            productError = null
                        )
                    }
                    loadOrders()
                }
                .onFailure { error ->
                    val err = error as? PurchaseOrderError
                        ?: PurchaseOrderError.UnknownError(cause = error)

                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = err,
                            operationSuccess = false
                        )
                    }
                }
        }
    }

    private fun markAsReceived(orderId: Int, receivedDate: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.markAsReceived(orderId, receivedDate)
                .onSuccess { order ->
                    _state.update {
                        it.copy(
                            orders = it.orders.map { o ->
                                if (o.orderId == orderId) order else o
                            },
                            filteredOrders = it.filteredOrders.map { o ->
                                if (o.orderId == orderId) order else o
                            },
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Pedido recibido. Stock actualizado"
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? PurchaseOrderError
                                ?: PurchaseOrderError.UnknownError(cause = error)
                        )
                    }
                }
        }
    }

    private fun updateNotes(orderId: Int, notes: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.updateNotes(orderId, notes)
                .onSuccess { order ->
                    _state.update {
                        it.copy(
                            orders = it.orders.map { o ->
                                if (o.orderId == orderId) order else o
                            },
                            filteredOrders = it.filteredOrders.map { o ->
                                if (o.orderId == orderId) order else o
                            },
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Notas actualizadas"
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? PurchaseOrderError
                                ?: PurchaseOrderError.UnknownError(cause = error)
                        )
                    }
                }
        }
    }

    private fun deleteOrder(orderId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.deleteOrder(orderId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            orders = it.orders.filter { o -> o.orderId != orderId },
                            filteredOrders = it.filteredOrders.filter { o -> o.orderId != orderId },
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Pedido eliminado",
                            selectedOrder = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? PurchaseOrderError
                                ?: PurchaseOrderError.UnknownError(cause = error)
                        )
                    }
                }
        }
    }

    private fun cancelOrder(orderId: Int) {
        deleteOrder(orderId)
    }

    // ───────────────────────────────────────────────
    // UI Y SELECCIÓN
    // ───────────────────────────────────────────────

    private fun selectOrder(orderId: Int?) {
        if (orderId == null) {
            clearSelection()
            return
        }

        val order = _state.value.orders.find { it.orderId == orderId }
        _state.update { it.copy(selectedOrder = order) }
    }

    private fun clearSelection() {
        _state.update {
            it.copy(
                selectedOrder = null,
                quantityError = null,
                supplierError = null,
                productError = null
            )
        }
    }

    fun clearError() {
        _state.update {
            it.copy(
                error = null,
                operationSuccess = false,
                successMessage = null
            )
        }
    }

    private fun resetState() {
        _state.update { PurchaseOrderState() }
        loadOrders()
    }

    // ───────────────────────────────────────────────
    // VALIDACIONES
    // ───────────────────────────────────────────────

    private fun validateQuantity(quantity: Int) {
        val error = when {
            quantity <= 0 -> "La cantidad debe ser mayor a 0"
            quantity > 10000 -> "La cantidad es demasiado grande"
            else -> null
        }
        _state.update { it.copy(quantityError = error) }
    }

    private fun validateSupplier(supplierId: Int?) {
        val error = when {
            supplierId == null -> "Debe seleccionar un proveedor"
            else -> null
        }
        _state.update { it.copy(supplierError = error) }
    }

    private fun validateProduct(productId: Int?) {
        val error = when {
            productId == null -> "Debe seleccionar un producto"
            else -> null
        }
        _state.update { it.copy(productError = error) }
    }
}