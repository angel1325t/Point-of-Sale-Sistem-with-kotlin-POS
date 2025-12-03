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
            // Listado y consulta
            is ProductsIntent.LoadProducts -> loadProductsInternal()
            is ProductsIntent.LoadProductById -> loadProductByIdInternal(intent.productId)
            is ProductsIntent.SearchProducts -> searchProductsInternal(intent.query)
            is ProductsIntent.FilterByCategory -> filterByCategoryInternal(intent.categoryId)
            is ProductsIntent.LoadLowStockProducts -> loadLowStockProductsInternal()

            // Creación
            is ProductsIntent.CreateProduct -> createProductInternal(
                name = intent.name,
                description = intent.description,
                price = intent.price,
                categoryId = intent.categoryId,
                currentStock = intent.currentStock,
                minimumStock = intent.minimumStock,
                barcode = intent.barcode,
                image = intent.image
            )

            // Actualización
            is ProductsIntent.UpdateProduct -> updateProductInternal(
                productId = intent.productId,
                name = intent.name,
                description = intent.description,
                price = intent.price,
                categoryId = intent.categoryId,
                currentStock = intent.currentStock,
                minimumStock = intent.minimumStock,
                barcode = intent.barcode,
                image = intent.image
            )

            // Eliminación
            is ProductsIntent.DeleteProduct -> deleteProductInternal(intent.productId)
            is ProductsIntent.DeleteMultipleProducts -> deleteMultipleProductsInternal(intent.productIds)

            // UI
            is ProductsIntent.SelectProduct -> selectProduct(intent.productId)
            is ProductsIntent.ClearSelection -> clearSelection()
            is ProductsIntent.ClearError -> clearError()
            is ProductsIntent.ResetState -> resetState()

            // Validaciones
            is ProductsIntent.ValidateProductName -> validateProductName(intent.name)
            is ProductsIntent.ValidatePrice -> validatePrice(intent.price)
            is ProductsIntent.ValidateStock -> validateStock(intent.stock)
            is ProductsIntent.ValidateBarcode -> validateBarcode(intent.barcode)
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
                    Log.d(TAG, "loadProductsInternal: Productos cargados exitosamente - ${products.size} productos")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = products,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "loadProductsInternal: Error al cargar productos", exception)
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
        Log.d(TAG, "loadProductByIdInternal: Cargando producto con ID: $productId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getProductById(productId)
                .onSuccess { product ->
                    Log.d(TAG, "loadProductByIdInternal: Producto cargado exitosamente - $product")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            selectedProduct = product,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "loadProductByIdInternal: Error al cargar producto ID $productId", exception)
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
        currentStock: Int,
        minimumStock: Int,
        barcode: String? = null,
        image: String? = null,
        discountType: String = "none",
        discountValue: Double = 0.0
    ) {
        Log.d(TAG, """
            createProductInternal: Iniciando creación de producto
            - name: $name
            - description: $description
            - price: $price
            - categoryId: $categoryId
            - currentStock: $currentStock
            - minimumStock: $minimumStock
            - barcode: $barcode
            - discountType: $discountType
            - discountValue: $discountValue
        """.trimIndent())

        viewModelScope.launch {
            // Validaciones
            when {
                !isValidProductName(name) -> {
                    Log.w(TAG, "createProductInternal: Nombre de producto inválido: $name")
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
                    Log.w(TAG, "createProductInternal: Precio inválido: $price")
                    _state.update { it.copy(error = ProductsError.PriceZeroOrNegative) }
                    return@launch
                }
                !isValidStock(currentStock) || !isValidStock(minimumStock) -> {
                    Log.w(TAG, "createProductInternal: Stock inválido - currentStock: $currentStock, minimumStock: $minimumStock")
                    _state.update { it.copy(error = ProductsError.StockNegative) }
                    return@launch
                }
            }

            _state.update { it.copy(isLoading = true, error = null) }

            val parsedDiscountType = try {
                DiscountType.valueOf(discountType.uppercase())
            } catch (e: Exception) {
                Log.w(TAG, "createProductInternal: Tipo de descuento inválido: $discountType, usando NONE")
                DiscountType.NONE
            }

            Log.d(TAG, "createProductInternal: Llamando a repository.createProduct con discountType: $parsedDiscountType")

            repository.createProduct(
                name = name,
                description = description,
                price = price,
                categoryId = categoryId,
                currentStock = currentStock,
                minimumStock = minimumStock,
                discountType = parsedDiscountType,
                discountValue = discountValue
            )
                .onSuccess { product ->
                    Log.d(TAG, "createProductInternal: Producto creado exitosamente - $product")
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
                    Log.e(TAG, "createProductInternal: Error al crear producto", exception)
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
        currentStock: Int,
        minimumStock: Int,
        barcode: String? = null,
        image: String? = null,
        discountType: String = "none",
        discountValue: Double = 0.0
    ) {
        Log.d(TAG, """
            updateProductInternal: Actualizando producto ID: $productId
            - name: $name
            - price: $price
            - categoryId: $categoryId
            - currentStock: $currentStock
            - minimumStock: $minimumStock
        """.trimIndent())

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val updates = ProductUpdateDTO(
                name = name,
                description = description,
                price = price,
                categoryId = categoryId,
                currentStock = currentStock,
                minimumStock = minimumStock,
                barcode = barcode,
                image = image,
                discountType = discountType,
                discountValue = discountValue
            )

            Log.d(TAG, "updateProductInternal: DTO creado - $updates")

            repository.updateProduct(productId, updates)
                .onSuccess { updatedProduct ->
                    Log.d(TAG, "updateProductInternal: Producto actualizado exitosamente - $updatedProduct")
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
                    Log.e(TAG, "updateProductInternal: Error al actualizar producto ID $productId", exception)
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
        Log.d(TAG, "deleteProductInternal: Eliminando producto ID: $productId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.deleteProduct(productId)
                .onSuccess {
                    Log.d(TAG, "deleteProductInternal: Producto eliminado exitosamente - ID: $productId")
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
                    Log.e(TAG, "deleteProductInternal: Error al eliminar producto ID $productId", exception)
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
        Log.d(TAG, "deleteMultipleProductsInternal: Eliminando ${productIds.size} productos - IDs: $productIds")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            var deletedCount = 0
            productIds.forEach { id ->
                repository.deleteProduct(id).onSuccess {
                    deletedCount++
                    Log.d(TAG, "deleteMultipleProductsInternal: Producto ID $id eliminado ($deletedCount/${productIds.size})")
                }
            }

            Log.d(TAG, "deleteMultipleProductsInternal: Proceso completado - $deletedCount/${productIds.size} productos eliminados")

            _state.update {
                it.copy(
                    isLoading = false,
                    products = it.products.filter { p -> p.productId !in productIds },
                    successMessage = if (deletedCount == productIds.size) {
                        "product_deleted_success"
                    } else {
                        "product_deleted_success"
                    },
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
        Log.d(TAG, "searchProductsInternal: Buscando productos con query: '$query'")

        if (query.isBlank()) {
            Log.d(TAG, "searchProductsInternal: Query vacío, cargando todos los productos")
            loadProductsInternal()
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.searchProductsByName(query)
                .onSuccess { products ->
                    Log.d(TAG, "searchProductsInternal: Búsqueda exitosa - ${products.size} productos encontrados")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = products,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "searchProductsInternal: Error en búsqueda", exception)
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
        Log.d(TAG, "filterByCategoryInternal: Filtrando por categoría ID: $categoryId")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getProducts()
                .onSuccess { products ->
                    val filtered = if (categoryId != null) {
                        products.filter { it.categoryId == categoryId }
                    } else {
                        products
                    }

                    Log.d(TAG, "filterByCategoryInternal: Filtrado exitoso - ${filtered.size} productos")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = filtered,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "filterByCategoryInternal: Error al filtrar", exception)
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
        Log.d(TAG, "loadLowStockProductsInternal: Cargando productos con stock bajo")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            repository.getProducts()
                .onSuccess { products ->
                    val lowStock = products.filter {
                        it.currentStock <= it.minimumStock
                    }

                    Log.d(TAG, "loadLowStockProductsInternal: ${lowStock.size} productos con stock bajo encontrados")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            products = lowStock,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "loadLowStockProductsInternal: Error al cargar productos con stock bajo", exception)
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
        Log.d(TAG, "validateProductName: Validando nombre '$name'")
        if (!isValidProductName(name)) {
            Log.w(TAG, "validateProductName: Nombre inválido")
            _state.update {
                it.copy(error = if (name.length < 2) {
                    ProductsError.ProductNameTooShort
                } else {
                    ProductsError.InvalidProductName
                })
            }
        } else {
            Log.d(TAG, "validateProductName: Nombre válido")
            _state.update { it.copy(error = null) }
        }
    }

    private fun validatePrice(price: Double) {
        Log.d(TAG, "validatePrice: Validando precio $price")
        if (!isValidPrice(price)) {
            Log.w(TAG, "validatePrice: Precio inválido")
            _state.update { it.copy(error = ProductsError.PriceZeroOrNegative) }
        } else {
            Log.d(TAG, "validatePrice: Precio válido")
            _state.update { it.copy(error = null) }
        }
    }

    private fun validateStock(stock: Int) {
        Log.d(TAG, "validateStock: Validando stock $stock")
        if (!isValidStock(stock)) {
            Log.w(TAG, "validateStock: Stock inválido")
            _state.update { it.copy(error = ProductsError.StockNegative) }
        } else {
            Log.d(TAG, "validateStock: Stock válido")
            _state.update { it.copy(error = null) }
        }
    }

    private fun validateBarcode(barcode: String?) {
        Log.d(TAG, "validateBarcode: Validando código de barras '$barcode'")
        if (barcode != null && !isValidBarcode(barcode)) {
            Log.w(TAG, "validateBarcode: Código de barras inválido")
            _state.update { it.copy(error = ProductsError.InvalidBarcode) }
        } else {
            Log.d(TAG, "validateBarcode: Código de barras válido")
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
        Log.d(TAG, "selectProduct: Seleccionando producto ID: $productId")
        val product = productId?.let { id ->
            _state.value.products.find { it.productId == id }
        }
        _state.update { it.copy(selectedProduct = product) }
    }

    private fun clearSelection() {
        Log.d(TAG, "clearSelection: Limpiando selección de producto")
        _state.update { it.copy(selectedProduct = null) }
    }

    private fun clearError() {
        Log.d(TAG, "clearError: Limpiando error")
        _state.update { it.copy(error = null) }
    }

    fun clearMessages() {
        Log.d(TAG, "clearMessages: Limpiando mensajes")
        _state.update { it.copy(successMessage = null, error = null) }
    }

    private fun resetState() {
        Log.d(TAG, "resetState: Reseteando estado completo")
        _state.update { ProductState() }
    }

    // ═══════════════════════════════════════════════════
    // MÉTODOS PÚBLICOS (API pública del ViewModel)
    // ═══════════════════════════════════════════════════

    fun loadProducts() {
        handleIntent(ProductsIntent.LoadProducts)
    }

    fun createProduct(
        name: String,
        description: String?,
        price: Double,
        categoryId: Int,
        currentStock: Int,
        minimumStock: Int,
        discountType: String,
        discountValue: Double
    ) {
        Log.d(TAG, """
            createProduct (public): 
            - name: $name
            - price: $price
            - categoryId: $categoryId
            - stock: $currentStock
            - discountType: $discountType
            - discountValue: $discountValue
        """.trimIndent())

        handleIntent(
            ProductsIntent.CreateProduct(
                name = name,
                description = description,
                price = price,
                barcode = null,
                categoryId = categoryId,
                image = null,
                currentStock = currentStock,
                minimumStock = minimumStock
            )
        )
    }

    fun updateProduct(id: Int, updateDTO: ProductUpdateDTO) {
        Log.d(TAG, "updateProduct (public): ID: $id, DTO: $updateDTO")
        handleIntent(
            ProductsIntent.UpdateProduct(
                productId = id,
                name = updateDTO.name ?: "",
                description = updateDTO.description,
                price = updateDTO.price ?: 0.0,
                barcode = updateDTO.barcode,
                categoryId = updateDTO.categoryId ?: 0,
                image = updateDTO.image,
                currentStock = updateDTO.currentStock ?: 0,
                minimumStock = updateDTO.minimumStock ?: 0
            )
        )
    }

    fun deleteProduct(productId: Int) {
        handleIntent(ProductsIntent.DeleteProduct(productId))
    }
}