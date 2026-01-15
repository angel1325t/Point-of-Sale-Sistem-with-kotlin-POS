package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.products.ProductsIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.products.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class ProductViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProductState())
    val state: StateFlow<ProductState> = _state.asStateFlow()

    fun handleIntent(intent: ProductsIntent) {
        when (intent) {

            is ProductsIntent.LoadProducts -> loadProducts()
            is ProductsIntent.LoadProductById -> loadProductById(intent.productId)
            is ProductsIntent.SearchProducts -> searchProducts(intent.query)
            is ProductsIntent.FilterByCategory -> filterByCategory(intent.categoryId)
            is ProductsIntent.LoadLowStockProducts -> loadLowStockProducts()

            is ProductsIntent.CreateProduct -> createProduct(
                name = intent.name,
                description = intent.description,
                price = intent.price,
                categoryId = intent.categoryId,
                currentStock = intent.currentStock,
                minimumStock = intent.minimumStock,
                discountType = intent.discountType,
                discountValue = intent.discountValue
            )

            is ProductsIntent.UpdateProduct -> updateProduct(
                productId = intent.productId,
                name = intent.name,
                description = intent.description,
                price = intent.price,
                categoryId = intent.categoryId,
                currentStock = intent.currentStock,
                minimumStock = intent.minimumStock,
                discountType = intent.discountType,
                discountValue = intent.discountValue
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
            is ProductsIntent.ValidateBranch -> validateBranch(intent.branchId)

            is ProductsIntent.FilterByBranch -> Unit
        }
    }

    // ───────────────────────────────
    // LOAD
    // ───────────────────────────────

    private fun loadProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getProducts()
                .onSuccess { products ->
                    _state.update {
                        it.copy(isLoading = false, products = products)
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.toProductsError()
                        )
                    }
                }
        }
    }

    private fun loadProductById(productId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            repository.getProductById(productId)
                .onSuccess { product ->
                    _state.update {
                        it.copy(isLoading = false, selectedProduct = product)
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.toProductsError()
                        )
                    }
                }
        }
    }

    // ───────────────────────────────
    // SEARCH / FILTER
    // ───────────────────────────────

    private fun searchProducts(query: String) {
        if (query.isBlank()) {
            loadProducts()
            return
        }

        viewModelScope.launch {
            repository.searchProductsByName(query)
                .onSuccess { products ->
                    _state.update { it.copy(products = products) }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(error = throwable.toProductsError())
                    }
                }
        }
    }

    private fun filterByCategory(categoryId: Int?) {
        viewModelScope.launch {
            repository.getProducts()
                .onSuccess { products ->
                    _state.update {
                        it.copy(
                            products = categoryId?.let { id ->
                                products.filter { p -> p.categoryId == id }
                            } ?: products
                        )
                    }
                }
        }
    }

    private fun loadLowStockProducts() {
        viewModelScope.launch {
            repository.getProducts()
                .onSuccess { products ->
                    _state.update {
                        it.copy(
                            products = products.filter {
                                it.currentStock <= it.minimumStock
                            }
                        )
                    }
                }
        }
    }

    // ───────────────────────────────
    // CREATE / UPDATE / DELETE
    // ───────────────────────────────

    private fun createProduct(
        name: String,
        description: String?,
        price: Double,
        categoryId: Int,
        currentStock: Int,
        minimumStock: Int,
        discountType: String,
        discountValue: Double
    ) {
        if (name.length < 2) {
            _state.update { it.copy(error = ProductsError.ProductNameTooShort) }
            return
        }
        if (price <= 0) {
            _state.update { it.copy(error = ProductsError.PriceZeroOrNegative) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            repository.createProduct(
                name,
                description,
                price,
                categoryId,
                currentStock,
                minimumStock,
                DiscountType.valueOf(discountType.uppercase()),
                discountValue
            )
                .onSuccess { product ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = it.products + product,
                            successMessage = "product_created_success"
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.toProductsError()
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
        categoryId: Int,
        currentStock: Int,
        minimumStock: Int,
        discountType: String,
        discountValue: Double
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            repository.updateProduct(
                productId,
                ProductUpdateDTO(
                    name,
                    description,
                    price,
                    null,
                    categoryId,
                    null,
                    currentStock,
                    minimumStock,
                    discountType,
                    discountValue
                )
            )
                .onSuccess { updated ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = it.products.map {
                                if (it.productId == productId) updated else it
                            },
                            successMessage = "product_updated_success"
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.toProductsError()
                        )
                    }
                }
        }
    }

    private fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            products = it.products.filterNot { p ->
                                p.productId == productId
                            }
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(error = throwable.toProductsError())
                    }
                }
        }
    }

    private fun deleteMultipleProducts(ids: List<Int>) {
        viewModelScope.launch {
            ids.forEach { repository.deleteProduct(it) }
            loadProducts()
        }
    }

    // ───────────────────────────────
    // UI HELPERS
    // ───────────────────────────────

    private fun selectProduct(id: Int?) {
        _state.update {
            it.copy(selectedProduct = it.products.find { p -> p.productId == id })
        }
    }

    private fun clearSelection() {
        _state.update { it.copy(selectedProduct = null) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun resetState() {
        _state.value = ProductState()
    }

    private fun validateProductName(name: String) {}
    private fun validatePrice(price: Double) {}
    private fun validateStock(stock: Int) {}
    private fun validateBarcode(barcode: String?) {}
    private fun validateBranch(branchId: String?) {}
}
