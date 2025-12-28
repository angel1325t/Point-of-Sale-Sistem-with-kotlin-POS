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

    companion object {
        private const val TAG = "ProductViewModel"
    }

    private val _state = MutableStateFlow(ProductState())
    val state: StateFlow<ProductState> = _state.asStateFlow()

    // ═══════════════════════════════════════════════════
    // FUNCIÓN PRINCIPAL PARA MANEJAR INTENTS
    // ═══════════════════════════════════════════════════
    fun handleIntent(intent: ProductsIntent) {
        Log.d(TAG, "handleIntent: $intent")
        when (intent) {
            is ProductsIntent.LoadProducts -> loadProductsInternal()
            is ProductsIntent.LoadProductById -> loadProductByIdInternal(intent.productId)
            is ProductsIntent.SearchProducts -> searchProductsInternal(intent.query)
            is ProductsIntent.FilterByCategory -> filterByCategoryInternal(intent.categoryId)
            is ProductsIntent.FilterByBranch -> filterByBranchInternal(intent.branchId)
            is ProductsIntent.LoadLowStockProducts -> loadLowStockProductsInternal()
            is ProductsIntent.CreateProduct -> createProductInternal(
                name = intent.name,
                description = intent.description,
                price = intent.price,
                categoryId = intent.categoryId,
                branchId = intent.branchId,
                currentStock = intent.currentStock,
                minimumStock = intent.minimumStock,
                discountType = intent.discountType,
                discountValue = intent.discountValue
            )
            is ProductsIntent.UpdateProduct -> updateProductInternal(
                productId = intent.productId,
                name = intent.name,
                description = intent.description,
                price = intent.price,
                categoryId = intent.categoryId,
                branchId = intent.branchId,
                currentStock = intent.currentStock,
                minimumStock = intent.minimumStock,
                discountType = intent.discountType,
                discountValue = intent.discountValue
            )
            is ProductsIntent.DeleteProduct -> deleteProductInternal(intent.productId)
            is ProductsIntent.DeleteMultipleProducts -> deleteMultipleProductsInternal(intent.productIds)
            is ProductsIntent.SelectProduct -> selectProduct(intent.productId)
            is ProductsIntent.ClearSelection -> clearSelection()
            is ProductsIntent.ClearError -> clearError()
            is ProductsIntent.ResetState -> resetState()
            is ProductsIntent.ValidateProductName -> validateProductName(intent.name)
            is ProductsIntent.ValidatePrice -> validatePrice(intent.price)
            is ProductsIntent.ValidateStock -> validateStock(intent.stock)
            is ProductsIntent.ValidateBarcode -> validateBarcode(intent.barcode)
            is ProductsIntent.ValidateBranch -> validateBranch(intent.branchId)
        }
    }

    // ═══════════════════════════════════════════════════
    // OPERACIONES CRUD
    // ═══════════════════════════════════════════════════

    private fun loadProductsInternal() {
        Log.d(TAG, "loadProductsInternal: Iniciando carga de productos")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getProducts()
                .onSuccess { products ->
                    Log.d(TAG, "loadProductsInternal: ${products.size} productos cargados")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = products,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "loadProductsInternal: Error", exception)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.toProductsError()
                        )
                    }
                }
        }
    }

    private fun loadProductByIdInternal(productId: Int) {
        Log.d(TAG, "loadProductByIdInternal: ID: $productId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getProductById(productId)
                .onSuccess { product ->
                    Log.d(TAG, "loadProductByIdInternal: Producto cargado")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            selectedProduct = product,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "loadProductByIdInternal: Error", exception)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.toProductsError()
                        )
                    }
                }
        }
    }

    private fun createProductInternal(
        name: String,
        description: String?,
        price: Double,
        categoryId: Int,
        branchId: String,
        currentStock: Int,
        minimumStock: Int,
        discountType: String = "none",
        discountValue: Double = 0.0
    ) {
        Log.d(TAG, "createProductInternal: name=$name, branchId=$branchId")

        viewModelScope.launch {
            when {
                !isValidProductName(name) -> {
                    _state.update {
                        it.copy(error = if (name.length < 2) {
                            ProductsError.ProductNameTooShort
                        } else {
                            ProductsError.InvalidProductName
                        })
                    }
                    return@launch
                }
                !isValidPrice(price) -> {
                    _state.update { it.copy(error = ProductsError.PriceZeroOrNegative) }
                    return@launch
                }
                !isValidStock(currentStock) || !isValidStock(minimumStock) -> {
                    _state.update { it.copy(error = ProductsError.StockNegative) }
                    return@launch
                }
                branchId.isBlank() -> {
                    _state.update {
                        it.copy(error = ProductsError.ValidationError("branch_id", "Debe seleccionar una sucursal"))
                    }
                    return@launch
                }
            }

            _state.update { it.copy(isLoading = true, error = null) }

            val parsedDiscountType = try {
                DiscountType.valueOf(discountType.uppercase())
            } catch (e: Exception) {
                DiscountType.NONE
            }

            repository.createProduct(
                name = name,
                description = description,
                price = price,
                categoryId = categoryId,
                branchId = branchId,
                currentStock = currentStock,
                minimumStock = minimumStock,
                discountType = parsedDiscountType,
                discountValue = discountValue
            )
                .onSuccess { product ->
                    Log.d(TAG, "createProductInternal: Producto creado")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = it.products + product,
                            successMessage = "product_created_success",
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "createProductInternal: Error", exception)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.toProductsError()
                        )
                    }
                }
        }
    }

    private fun updateProductInternal(
        productId: Int,
        name: String,
        description: String?,
        price: Double,
        categoryId: Int,
        branchId: String,
        currentStock: Int,
        minimumStock: Int,
        discountType: String = "none",
        discountValue: Double = 0.0
    ) {
        Log.d(TAG, "updateProductInternal: ID=$productId, branchId=$branchId")

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val updates = ProductUpdateDTO(
                name = name,
                description = description,
                price = price,
                categoryId = categoryId,
                branchId = branchId,
                currentStock = currentStock,
                minimumStock = minimumStock,
                discountType = discountType,
                discountValue = discountValue
            )

            repository.updateProduct(productId, updates)
                .onSuccess { updatedProduct ->
                    Log.d(TAG, "updateProductInternal: Producto actualizado")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = it.products.map { p ->
                                if (p.productId == productId) updatedProduct else p
                            },
                            successMessage = "product_updated_success",
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "updateProductInternal: Error", exception)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.toProductsError()
                        )
                    }
                }
        }
    }

    private fun deleteProductInternal(productId: Int) {
        Log.d(TAG, "deleteProductInternal: ID=$productId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.deleteProduct(productId)
                .onSuccess {
                    Log.d(TAG, "deleteProductInternal: Eliminado")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = it.products.filter { p -> p.productId != productId },
                            successMessage = "product_deleted_success",
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "deleteProductInternal: Error", exception)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.toProductsError()
                        )
                    }
                }
        }
    }

    private fun deleteMultipleProductsInternal(productIds: List<Int>) {
        Log.d(TAG, "deleteMultipleProductsInternal: ${productIds.size} productos")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            var deletedCount = 0
            productIds.forEach { id ->
                repository.deleteProduct(id).onSuccess {
                    deletedCount++
                }
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    products = it.products.filter { p -> p.productId !in productIds },
                    successMessage = "product_deleted_success",
                    error = if (deletedCount < productIds.size) {
                        ProductsError.DeleteProductFailed
                    } else null
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // BÚSQUEDA Y FILTRADO
    // ═══════════════════════════════════════════════════

    private fun searchProductsInternal(query: String) {
        if (query.isBlank()) {
            loadProductsInternal()
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.searchProductsByName(query)
                .onSuccess { products ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = products,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.toProductsError()
                        )
                    }
                }
        }
    }

    private fun filterByCategoryInternal(categoryId: Int?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getProducts()
                .onSuccess { products ->
                    val filtered = if (categoryId != null) {
                        products.filter { it.categoryId == categoryId }
                    } else {
                        products
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = filtered,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.toProductsError()
                        )
                    }
                }
        }
    }

    private fun filterByBranchInternal(branchId: String?) {
        Log.d(TAG, "filterByBranchInternal: branchId=$branchId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            if (branchId == null) {
                repository.getProducts()
            } else {
                repository.getProductsByBranch(branchId)
            }
                .onSuccess { products ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = products,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.toProductsError()
                        )
                    }
                }
        }
    }

    private fun loadLowStockProductsInternal() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getProducts()
                .onSuccess { products ->
                    val lowStock = products.filter {
                        it.currentStock <= it.minimumStock
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = lowStock,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.toProductsError()
                        )
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════
    // VALIDACIONES
    // ═══════════════════════════════════════════════════

    private fun validateProductName(name: String) {
        if (!isValidProductName(name)) {
            _state.update {
                it.copy(error = if (name.length < 2) {
                    ProductsError.ProductNameTooShort
                } else {
                    ProductsError.InvalidProductName
                })
            }
        } else {
            _state.update { it.copy(error = null) }
        }
    }

    private fun validatePrice(price: Double) {
        if (!isValidPrice(price)) {
            _state.update { it.copy(error = ProductsError.PriceZeroOrNegative) }
        } else {
            _state.update { it.copy(error = null) }
        }
    }

    private fun validateStock(stock: Int) {
        if (!isValidStock(stock)) {
            _state.update { it.copy(error = ProductsError.StockNegative) }
        } else {
            _state.update { it.copy(error = null) }
        }
    }

    private fun validateBarcode(barcode: String?) {
        if (barcode != null && !isValidBarcode(barcode)) {
            _state.update { it.copy(error = ProductsError.InvalidBarcode) }
        } else {
            _state.update { it.copy(error = null) }
        }
    }

    private fun validateBranch(branchId: String?) {
        if (branchId.isNullOrBlank()) {
            _state.update {
                it.copy(error = ProductsError.ValidationError("branch_id", "Debe seleccionar una sucursal"))
            }
        } else {
            _state.update { it.copy(error = null) }
        }
    }

    private fun isValidProductName(name: String): Boolean = name.isNotBlank() && name.length >= 2
    private fun isValidPrice(price: Double): Boolean = price > 0
    private fun isValidStock(stock: Int): Boolean = stock >= 0
    private fun isValidBarcode(barcode: String): Boolean = barcode.length == 13 && barcode.all { it.isDigit() }

    // ═══════════════════════════════════════════════════
    // GESTIÓN DE UI
    // ═══════════════════════════════════════════════════

    private fun selectProduct(productId: Int?) {
        val product = productId?.let { id ->
            _state.value.products.find { it.productId == id }
        }
        _state.update { it.copy(selectedProduct = product) }
    }

    private fun clearSelection() {
        _state.update { it.copy(selectedProduct = null) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearMessages() {
        _state.update { it.copy(successMessage = null, error = null) }
    }

    private fun resetState() {
        _state.update { ProductState() }
    }

    // ═══════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS
    // ═══════════════════════════════════════════════════

    fun loadProducts() {
        handleIntent(ProductsIntent.LoadProducts)
    }

    fun filterByBranch(branchId: String?) {
        handleIntent(ProductsIntent.FilterByBranch(branchId))
    }
}