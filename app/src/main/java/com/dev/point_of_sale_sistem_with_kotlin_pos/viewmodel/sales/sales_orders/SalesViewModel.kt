package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.sales_orders

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders.SalesIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.Instant
import java.util.UUID

data class SaleState(
    val isLoading: Boolean = false,
    val sale: Sale? = null,
    val searchResults: List<ProductDTO> = emptyList(),
    val error: SaleError? = null,
    val lastScannedBarcode: String? = null
)

class SalesViewModel(
    private val repository: SalesProductRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SaleState())
    val state: StateFlow<SaleState> = _state.asStateFlow()

    private val saleItems = mutableListOf<SaleDetail>()

    companion object {
        private const val TAG = "SalesViewModel"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun currentUtcDateTime(): LocalDateTime {
        val millis = System.currentTimeMillis()
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
    }

    private fun setError(error: SaleError) {
        _state.value = _state.value.copy(
            isLoading = false,
            error = error
        )
    }

    private fun mapExceptionToSaleError(throwable: Throwable): SaleError {
        return when (throwable) {
            is java.net.UnknownHostException -> SaleError.Network
            is IllegalArgumentException -> SaleError.ValidationFailed
            else -> SaleError.Unknown(throwable)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handleIntent(intent: SalesIntent) {
        when (intent) {
            is SalesIntent.SearchProductByName -> searchByName(intent.query)
            is SalesIntent.SearchProductByBarcode -> searchByBarcode(intent.barcode)
            SalesIntent.ClearSearchResults -> clearSearchResults()

            is SalesIntent.AddSaleDetail -> addProductToSale(intent)
            is SalesIntent.RemoveSaleDetail -> removeProductFromSale(intent.productId)
            is SalesIntent.UpdateSaleDetail -> updateProductInSale(intent)

            is SalesIntent.CreateSale -> createSale(intent.paymentMethod, intent.globalDiscount, intent.userId)
            SalesIntent.CompleteSale -> completeSale()
            SalesIntent.CancelSale -> cancelSale()
            SalesIntent.ClearError -> clearError()
        }
    }

    // =====================================================
    // 🔍 SEARCH PRODUCTS
    // =====================================================
    private fun searchByName(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList())
            return
        }

        Log.d(TAG, "searchByName: Buscando '$query'")
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            repository.searchProductsByName(query)
                .onSuccess { products ->
                    Log.d(TAG, "searchByName: Encontrados ${products.size} productos")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        searchResults = products.filterNotNull()
                    )
                }
                .onFailure { e ->
                    Log.e(TAG, "searchByName: Error", e)
                    setError(mapExceptionToSaleError(e))
                    _state.value = _state.value.copy(searchResults = emptyList())
                }
        }
    }

    private fun searchByBarcode(barcode: String) {
        if (barcode.isBlank()) {
            setError(SaleError.ValidationFailed)
            return
        }

        Log.d(TAG, "searchByBarcode: Buscando '$barcode'")
        _state.value = _state.value.copy(
            isLoading = true,
            lastScannedBarcode = barcode
        )

        viewModelScope.launch {
            repository.getProductByBarcode(barcode)
                .onSuccess { product ->
                    if (product != null) {
                        Log.d(TAG, "searchByBarcode: Producto encontrado: ${product.name}")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            searchResults = listOf(product)
                        )
                    } else {
                        Log.d(TAG, "searchByBarcode: No se encontró producto con código '$barcode'")
                        setError(SaleError.Server("No se encontró producto con código: $barcode"))
                        _state.value = _state.value.copy(
                            isLoading = false,
                            searchResults = emptyList()
                        )
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "searchByBarcode: Error", e)
                    setError(SaleError.Server("Error al buscar producto por código"))
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    private fun clearSearchResults() {
        _state.value = _state.value.copy(
            searchResults = emptyList(),
            lastScannedBarcode = null
        )
    }

    // =====================================================
    // 🛍️ SALE ITEMS MANAGEMENT
    // =====================================================
    @RequiresApi(Build.VERSION_CODES.O)
    private fun addProductToSale(intent: SalesIntent.AddSaleDetail) {
        if (intent.quantity <= 0) {
            setError(SaleError.ValidationFailed)
            return
        }

        val currentSale = _state.value.sale
        if (currentSale == null) {
            setError(SaleError.ValidationFailed)
            return
        }

        val nowUtc = currentUtcDateTime()
        val existingItemIndex = saleItems.indexOfFirst { it.productId == intent.productId }

        if (existingItemIndex != -1) {
            val existingItem = saleItems[existingItemIndex]
            val newQuantity = existingItem.quantity + intent.quantity

            saleItems[existingItemIndex] = SaleDetail(
                saleDetailId = existingItem.saleDetailId,
                saleId = currentSale.saleId.toString(),
                productId = intent.productId,
                quantity = newQuantity,
                unitPrice = intent.unitPrice,
                discount = intent.discount,
                finalPrice = (intent.unitPrice * newQuantity) - intent.discount,
                createdAt = existingItem.createdAt
            )
        } else {
            val newDetail = SaleDetail(
                saleDetailId = (saleItems.maxOfOrNull { it.saleDetailId } ?: 0) + 1,
                saleId = currentSale.saleId.toString(),
                productId = intent.productId,
                quantity = intent.quantity,
                unitPrice = intent.unitPrice,
                discount = intent.discount,
                finalPrice = (intent.unitPrice * intent.quantity) - intent.discount,
                createdAt = nowUtc
            )

            saleItems.add(newDetail)
        }

        updateSaleState()
        _state.value = _state.value.copy(searchResults = emptyList())
    }

    private fun removeProductFromSale(productId: Int) {
        saleItems.removeAll { it.productId == productId }
        updateSaleState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateProductInSale(intent: SalesIntent.UpdateSaleDetail) {
        val index = saleItems.indexOfFirst { it.productId == intent.productId }
        if (index == -1) {
            setError(SaleError.ValidationFailed)
            return
        }

        if (intent.quantity <= 0) {
            removeProductFromSale(intent.productId)
            return
        }

        val existingItem = saleItems[index]

        saleItems[index] = SaleDetail(
            saleDetailId = existingItem.saleDetailId,
            saleId = _state.value.sale?.saleId.toString(),
            productId = intent.productId,
            quantity = intent.quantity,
            unitPrice = intent.unitPrice,
            discount = intent.discount,
            finalPrice = (intent.unitPrice * intent.quantity) - intent.discount,
            createdAt = existingItem.createdAt
        )

        updateSaleState()
    }

    private fun updateSaleState() {
        val currentSale = _state.value.sale ?: return

        val subtotal = saleItems.sumOf { it.unitPrice * it.quantity }
        val totalDiscount = saleItems.sumOf { it.discount }
        val total = subtotal - totalDiscount - currentSale.globalDiscount

        _state.value = _state.value.copy(
            sale = currentSale.copy(
                saleDetails = saleItems.toList(),
                total = total
            )
        )
    }

    // =====================================================
    // 💵 CREATE / COMPLETE / CANCEL SALE
    // =====================================================
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createSale(paymentMethod: String, globalDiscount: Double, userId: UUID) {
        val nowUtc = currentUtcDateTime()

        saleItems.clear()

        _state.value = _state.value.copy(
            sale = Sale(
                saleId = UUID.randomUUID(),
                userId = userId,
                saleDate = nowUtc,
                createdAt = nowUtc,
                paymentMethod = paymentMethod,
                status = "pending",
                total = 0.0,
                globalDiscount = globalDiscount,
                saleDetails = emptyList()
            ),
            searchResults = emptyList()
        )
    }

    private fun completeSale() {
        val sale = _state.value.sale

        if (sale == null || saleItems.isEmpty()) {
            setError(SaleError.ValidationFailed)
            return
        }

        _state.value = _state.value.copy(
            isLoading = true
        )

        viewModelScope.launch {
            // TODO: Call repository to save sale
            // repository.createSale(sale)

            _state.value = _state.value.copy(
                sale = sale.copy(status = "completed"),
                isLoading = false
            )
        }
    }

    private fun cancelSale() {
        saleItems.clear()
        _state.value = SaleState()
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}