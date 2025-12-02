package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.products.ProductsIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductsError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.products.ProductsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductsViewModel(
    private val repository: ProductsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProductState())
    val state: StateFlow<ProductState> = _state.asStateFlow()

    init {
        handleIntent(ProductsIntent.LoadProducts)
    }

    /**
     * Maneja todos los intents del usuario
     */
    fun handleIntent(intent: ProductsIntent) {
        when (intent) {

            is ProductsIntent.LoadProducts -> loadProducts()
            is ProductsIntent.LoadProductById -> loadProductById(intent.productId)
            is ProductsIntent.SearchProducts -> searchProducts(intent.query)
            is ProductsIntent.FilterByCategory -> filterByCategory(intent.categoryId)
            is ProductsIntent.LoadLowStockProducts -> loadLowStockProducts()

            is ProductsIntent.CreateProduct -> createProduct(
                intent.name,
                intent.description,
                intent.price,
                intent.barcode,
                intent.categoryId,
                intent.image,
                intent.currentStock,
                intent.minimumStock
            )

            is ProductsIntent.UpdateProduct -> updateProduct(
                intent.productId,
                intent.name,
                intent.description,
                intent.price,
                intent.barcode,
                intent.categoryId,
                intent.image,
                intent.currentStock,
                intent.minimumStock
            )

            is ProductsIntent.DeleteProduct -> deleteProduct(intent.productId)
            is ProductsIntent.DeleteMultipleProducts -> deleteMultipleProducts(intent.productIds)

            is ProductsIntent.SelectProduct -> selectProduct(intent.productId)
            is ProductsIntent.ClearSelection -> clearSelection()
            is ProductsIntent.ClearError -> clearError()
            is ProductsIntent.ResetState -> resetState()

            is ProductsIntent.ValidateProductName -> validateProductName(intent.name)
            is ProductsIntent.ValidatePrice -> validatePrice(intent.price)
            is ProductsIntent.ValidateStock -> validateStock(intent.stock)
            is ProductsIntent.ValidateBarcode -> validateBarcode(intent.barcode)
        }
    }

    // ────────────────────────────────────────
    // CRUD BASICO
    // ────────────────────────────────────────

    private fun loadProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getAllProducts()
                .onSuccess { products ->
                    _state.update {
                        it.copy(
                            products = products,
                            filteredProducts = products,
                            isLoading = false,
                            totalItems = products.size
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? ProductsError ?: ProductsError.UnknownError(exception = error)
                        )
                    }
                }
        }
    }

    private fun loadProductById(productId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getProductById(productId)
                .onSuccess { product ->
                    _state.update {
                        it.copy(
                            selectedProduct = product,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? ProductsError ?: ProductsError.UnknownError(exception = error)
                        )
                    }
                }
        }
    }

    private fun searchProducts(query: String) {
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
                        filteredProducts = it.products,
                        isLoading = false
                    )
                }
                return@launch
            }

            repository.searchProducts(query)
                .onSuccess { products ->
                    _state.update {
                        it.copy(
                            filteredProducts = products,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? ProductsError ?: ProductsError.UnknownError(exception = error)
                        )
                    }
                }
        }
    }

    private fun filterByCategory(categoryId: Int?) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    filterCategoryId = categoryId,
                    isLoading = true,
                    error = null
                )
            }

            repository.getProductsByCategoryId(categoryId)
                .onSuccess { products ->
                    _state.update {
                        it.copy(
                            filteredProducts = products,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error as? ProductsError ?: ProductsError.UnknownError(exception = error)
                        )
                    }
                }
        }
    }

    private fun loadLowStockProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = state.value.products.filter { it.currentStock <= it.minimumStock }

            _state.update {
                it.copy(
                    filteredProducts = result,
                    isLoading = false
                )
            }
        }
    }

    private fun createProduct(
        name: String,
        description: String?,
        price: Double,
        barcode: String?,
        categoryId: Int,
        image: String?,
        currentStock: Int,
        minimumStock: Int
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.createProduct(
                name,
                description,
                price,
                barcode,
                categoryId,
                image,
                currentStock,
                minimumStock
            ).onSuccess { product ->

                _state.update {
                    it.copy(
                        products = it.products + product,
                        filteredProducts = it.filteredProducts + product,
                        isLoading = false,
                        operationSuccess = true,
                        successMessage = "Producto creado exitosamente",
                        selectedProduct = null,
                        nameError = null,
                        priceError = null,
                        stockError = null,
                        barcodeError = null
                    )
                }

                loadProducts()

            }.onFailure { error ->
                val err = error as? ProductsError ?: ProductsError.UnknownError(exception = error)

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

    private fun updateProduct(
        productId: Int,
        name: String,
        description: String?,
        price: Double,
        barcode: String?,
        categoryId: Int,
        image: String?,
        currentStock: Int,
        minimumStock: Int
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.updateProduct(
                productId,
                name,
                description,
                price,
                barcode,
                categoryId,
                image,
                currentStock,
                minimumStock
            ).onSuccess { product ->

                _state.update {
                    it.copy(
                        products = it.products.map { p -> if (p.productId == productId) product else p },
                        filteredProducts = it.filteredProducts.map { p -> if (p.productId == productId) product else p },
                        isLoading = false,
                        operationSuccess = true,
                        successMessage = "Producto actualizado exitosamente",
                        selectedProduct = null,
                        nameError = null,
                        priceError = null,
                        stockError = null,
                        barcodeError = null
                    )
                }

                loadProducts()

            }.onFailure { error ->
                val err = error as? ProductsError ?: ProductsError.UnknownError(exception = error)

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

    private fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.deleteProduct(productId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            products = it.products.filter { p -> p.productId != productId },
                            filteredProducts = it.filteredProducts.filter { p -> p.productId != productId },
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Producto eliminado exitosamente",
                            selectedProduct = null
                        )
                    }
                }
                .onFailure { error ->
                    val err = error as? ProductsError ?: ProductsError.UnknownError(exception = error)

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

    private fun deleteMultipleProducts(productIds: List<Int>) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.deleteMultipleProducts(productIds)
                .onSuccess { deletedCount ->
                    _state.update {
                        it.copy(
                            products = it.products.filter { p -> p.productId !in productIds },
                            filteredProducts = it.filteredProducts.filter { p -> p.productId !in productIds },
                            isLoading = false,
                            operationSuccess = true,
                            successMessage = "Se eliminaron $deletedCount productos",
                            selectedProduct = null
                        )
                    }
                }
                .onFailure { error ->
                    val err = error as? ProductsError ?: ProductsError.UnknownError(exception = error)

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

    // ─────────────────────────────────────
    // SELECCIÓN Y CONTROLES DE UI
    // ─────────────────────────────────────

    private fun selectProduct(productId: Int?) {
        if (productId == null) {
            clearSelection()
            return
        }

        val product = _state.value.products.find { it.productId == productId }
        _state.update { it.copy(selectedProduct = product) }
    }

    private fun clearSelection() {
        _state.update {
            it.copy(
                selectedProduct = null,
                nameError = null,
                priceError = null,
                stockError = null,
                barcodeError = null
            )
        }
    }

    private fun clearError() {
        _state.update {
            it.copy(
                error = null,
                operationSuccess = false,
                successMessage = null
            )
        }
    }

    private fun resetState() {
        _state.update { ProductState() }
        loadProducts()
    }

    // ─────────────────────────────────────
    // VALIDACIONES
    // ─────────────────────────────────────

    private fun validateProductName(name: String) {
        val error = when {
            name.isBlank() -> "El nombre no puede estar vacío"
            name.length < 2 -> "Debe tener al menos 2 caracteres"
            name.length > 100 -> "No puede exceder 100 caracteres"
            else -> null
        }
        _state.update { it.copy(nameError = error) }
    }

    private fun validatePrice(price: Double) {
        val error = when {
            price <= 0 -> "El precio debe ser mayor que 0"
            else -> null
        }
        _state.update { it.copy(priceError = error) }
    }

    private fun validateStock(stock: Int) {
        val error = when {
            stock < 0 -> "El stock no puede ser negativo"
            else -> null
        }
        _state.update { it.copy(stockError = error) }
    }

    private fun validateBarcode(barcode: String?) {
        val error = when {
            barcode != null && barcode.length > 50 -> "El código de barras no puede exceder 50 caracteres"
            else -> null
        }
        _state.update { it.copy(barcodeError = error) }
    }
}
