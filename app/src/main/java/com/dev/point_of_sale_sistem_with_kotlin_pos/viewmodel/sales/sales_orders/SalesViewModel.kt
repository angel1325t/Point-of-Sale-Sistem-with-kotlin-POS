package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.sales_orders

import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders.SalesIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.PaymentProofRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesProductRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.StripeCanceledException
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.StripePaymentRepository
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class SalesViewModel(
    private val salesRepository: SalesRepository,
    private val salesProductRepository: SalesProductRepository,
    private val paymentProofRepository: PaymentProofRepository,
    private val stripePaymentRepository: StripePaymentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SaleState())
    val state: StateFlow<SaleState> = _state.asStateFlow()

    private val saleItems = mutableListOf<SaleDetail>()

    companion object {
        private const val TAG = "SalesViewModel"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun currentUtcDateTime(): LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(System.currentTimeMillis()),
            ZoneOffset.UTC
        )
    }

    private fun setError(error: SaleError) {
        _state.value = _state.value.copy(
            isLoading = false,
            error = error,
            paymentFlowState = PaymentFlowState.Error(error.toString())
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
            is SalesIntent.UpdatePaymentMethod -> updatePaymentMethod(intent.method)
            SalesIntent.CompleteSale -> initiateSaleCompletion()
            is SalesIntent.ConfirmCashPayment -> processCashPayment(intent.amountReceived)
            SalesIntent.InitiateCardPayment -> initiateCardPayment()
            is SalesIntent.ProcessStripeResult -> processStripeResult(intent.paymentIntentId)
            SalesIntent.CaptureTransferEvidence -> captureTransferEvidence()
            is SalesIntent.SubmitTransferEvidence -> submitTransferEvidence(intent.imageFile, intent.referenceNumber)
            SalesIntent.ClearSale -> clearSale()
            SalesIntent.ClearError -> clearError()
        }
    }

    // ═══════════════════════════════════════════════════
    // 🔍 BÚSQUEDA DE PRODUCTOS
    // ═══════════════════════════════════════════════════
    private fun searchByName(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList())
            return
        }

        Log.d(TAG, "searchByName: Searching '$query'")
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            salesProductRepository.searchProductsByName(query)
                .onSuccess { products ->
                    Log.d(TAG, "searchByName: Found ${products.size} products")
                    val validProducts = products.filterNotNull()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        searchResults = validProducts
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

        Log.d(TAG, "searchByBarcode: Searching '$barcode'")
        _state.value = _state.value.copy(isLoading = true, lastScannedBarcode = barcode)

        viewModelScope.launch {
            salesProductRepository.getProductByBarcode(barcode)
                .onSuccess { product ->
                    if (product != null) {
                        Log.d(TAG, "searchByBarcode: Found product: ${product.name}")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            searchResults = listOf(product)
                        )
                    } else {
                        Log.d(TAG, "searchByBarcode: No product found with code '$barcode'")
                        setError(SaleError.Server("Producto no encontrado: $barcode"))
                        _state.value = _state.value.copy(isLoading = false, searchResults = emptyList())
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "searchByBarcode: Error", e)
                    setError(SaleError.Server("Error al buscar producto"))
                    _state.value = _state.value.copy(isLoading = false)
                }
        }
    }

    private fun clearSearchResults() {
        _state.value = _state.value.copy(searchResults = emptyList(), lastScannedBarcode = null)
    }

    // ═══════════════════════════════════════════════════
    // 🛍️ GESTIÓN DE PRODUCTOS EN LA VENTA
    // ═══════════════════════════════════════════════════
    @RequiresApi(Build.VERSION_CODES.O)
    private fun addProductToSale(intent: SalesIntent.AddSaleDetail) {
        if (intent.quantity <= 0) {
            setError(SaleError.ValidationFailed)
            return
        }

        val currentSale = _state.value.sale ?: run {
            setError(SaleError.ValidationFailed)
            return
        }

        val productFromSearch = _state.value.searchResults.find { it.productId == intent.productId }
        productFromSearch?.let { product ->
            val updatedCache = _state.value.productsCache.toMutableMap()
            updatedCache[product.productId] = product
            _state.value = _state.value.copy(productsCache = updatedCache)
        }

        val nowUtc = currentUtcDateTime()
        val existingIndex = saleItems.indexOfFirst { it.productId == intent.productId }

        if (existingIndex != -1) {
            val existing = saleItems[existingIndex]
            val newQuantity = existing.quantity + intent.quantity
            saleItems[existingIndex] = existing.copy(
                quantity = newQuantity,
                finalPrice = (intent.unitPrice * newQuantity) - intent.discount
            )
        } else {
            saleItems.add(
                SaleDetail(
                    saleDetailId = (saleItems.maxOfOrNull { it.saleDetailId } ?: 0) + 1,
                    saleId = currentSale.saleId.toString(),
                    productId = intent.productId,
                    quantity = intent.quantity,
                    unitPrice = intent.unitPrice,
                    discount = intent.discount,
                    finalPrice = (intent.unitPrice * intent.quantity) - intent.discount,
                    createdAt = nowUtc
                )
            )
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

        val existing = saleItems[index]
        saleItems[index] = existing.copy(
            quantity = intent.quantity,
            unitPrice = intent.unitPrice,
            discount = intent.discount,
            finalPrice = (intent.unitPrice * intent.quantity) - intent.discount
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

    // ═══════════════════════════════════════════════════
    // 💳 GESTIÓN DE MÉTODOS DE PAGO
    // ═══════════════════════════════════════════════════
    private fun updatePaymentMethod(method: String) {
        val currentSale = _state.value.sale ?: return
        _state.value = _state.value.copy(
            sale = currentSale.copy(paymentMethod = method)
        )
        Log.d(TAG, "Payment method updated to: $method")
    }

    // ═══════════════════════════════════════════════════
    // 🚀 INICIO DEL PROCESO DE PAGO
    // ═══════════════════════════════════════════════════
    private fun initiateSaleCompletion() {
        val sale = _state.value.sale

        if (sale == null || saleItems.isEmpty()) {
            setError(SaleError.ValidationFailed)
            return
        }

        Log.d(TAG, "Initiating sale completion with method: ${sale.paymentMethod}")

        when (sale.paymentMethod) {
            "cash" -> {
                _state.value = _state.value.copy(
                    paymentFlowState = PaymentFlowState.CashPayment(sale.total)
                )
            }
            "card" -> {
                initiateCardPayment()
            }
            "transfer" -> {
                _state.value = _state.value.copy(
                    paymentFlowState = PaymentFlowState.TransferPayment(sale.total)
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // 💵 PAGO EN EFECTIVO
    // ═══════════════════════════════════════════════════
    private fun processCashPayment(amountReceived: Double) {
        val sale = _state.value.sale ?: return

        if (amountReceived < sale.total) {
            setError(SaleError.ValidationFailed)
            return
        }

        val change = amountReceived - sale.total
        Log.d(TAG, "Cash payment: Received $amountReceived, Change: $change")

        finalizeSale()
    }

    // ═══════════════════════════════════════════════════
    // 💳 PAGO CON TARJETA (STRIPE)
    // ═══════════════════════════════════════════════════
    private fun initiateCardPayment() {
        val sale = _state.value.sale ?: return

        _state.value = _state.value.copy(
            isLoading = true,
            paymentFlowState = PaymentFlowState.CardPayment(
                total = sale.total,
                paymentIntentId = null,
                clientSecret = null
            )
        )

        viewModelScope.launch {
            stripePaymentRepository.createPaymentIntent(
                amount = sale.total,
                currency = "USD",
                saleId = sale.saleId.toString()
            ).onSuccess { intent ->
                Log.d(TAG, "Stripe Payment Intent created: ${intent.paymentIntentId}")

                _state.value = _state.value.copy(
                    isLoading = false,
                    paymentFlowState = PaymentFlowState.CardPayment(
                        total = sale.total,
                        paymentIntentId = intent.paymentIntentId,
                        clientSecret = intent.clientSecret
                    )
                )

            }.onFailure { e ->
                Log.e(TAG, "Error creating Stripe Payment Intent", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    paymentFlowState = PaymentFlowState.Error("Error al iniciar pago: ${e.message}")
                )
            }
        }
    }

    fun onStripePaymentComplete(result: com.stripe.android.paymentsheet.PaymentSheetResult) {
        when (result) {
            is com.stripe.android.paymentsheet.PaymentSheetResult.Completed -> {
                val paymentIntentId = _state.value.paymentFlowState.let {
                    (it as? PaymentFlowState.CardPayment)?.paymentIntentId
                } ?: return
                processStripeResult(paymentIntentId)
            }
            is com.stripe.android.paymentsheet.PaymentSheetResult.Canceled -> {
                Log.d(TAG, "Stripe payment cancelled by user")
                val sale = _state.value.sale ?: return
                _state.value = _state.value.copy(
                    isLoading = false,
                    paymentFlowState = PaymentFlowState.CardPayment(
                        total = sale.total,
                        errorMessage = "Pago cancelado"
                    )
                )
            }
            is com.stripe.android.paymentsheet.PaymentSheetResult.Failed -> {
                Log.e(TAG, "Stripe payment failed: ${result.error.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    paymentFlowState = PaymentFlowState.Error("Error en el pago: ${result.error.localizedMessage}")
                )
            }
        }
    }

    private fun processStripeResult(paymentIntentId: String) {
        val sale = _state.value.sale ?: return

        _state.value = _state.value.copy(
            isLoading = true,
            paymentFlowState = PaymentFlowState.ProcessingPayment("card")
        )

        viewModelScope.launch {
            stripePaymentRepository.confirmPayment(paymentIntentId)
                .onSuccess { confirmation ->
                    if (confirmation.status == "succeeded") {
                        Log.d(TAG, "Stripe payment confirmed: ${confirmation.paymentIntentId}")

                        val saleToCreate = sale.copy(status = "completed")
                        salesRepository.createSale(saleToCreate)
                            .onSuccess { saleId ->
                                Log.d(TAG, "Sale created in database: $saleId")

                                paymentProofRepository.createPaymentProof(
                                    saleId = saleId,
                                    paymentMethod = "card",
                                    voucherPath = "stripe_${confirmation.chargeId ?: confirmation.paymentIntentId}",
                                    referenceNumber = paymentIntentId,
                                    amount = sale.total,
                                    capturedBy = sale.userId.toString()
                                ).onSuccess {
                                    Log.d(TAG, "Stripe payment proof created")
                                }.onFailure { e ->
                                    Log.w(TAG, "Could not create payment proof (non-critical)", e)
                                }

                                _state.value = _state.value.copy(
                                    sale = saleToCreate,
                                    isLoading = false,
                                    paymentFlowState = PaymentFlowState.Success(saleId)
                                )

                            }.onFailure { e ->
                                Log.e(TAG, "Error creating sale after Stripe payment", e)
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    paymentFlowState = PaymentFlowState.Error("Pago procesado pero error al guardar venta")
                                )
                            }

                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            paymentFlowState = PaymentFlowState.Error("Pago no completado: ${confirmation.status}")
                        )
                    }

                }.onFailure { e ->
                    Log.e(TAG, "Error confirming Stripe payment", e)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        paymentFlowState = PaymentFlowState.Error("Error al confirmar pago: ${e.message}")
                    )
                }
        }
    }

    // ═══════════════════════════════════════════════════
    // 🏦 PAGO POR TRANSFERENCIA CON EVIDENCIA
    // ═══════════════════════════════════════════════════
    private fun captureTransferEvidence() {
        val sale = _state.value.sale ?: return

        _state.value = _state.value.copy(
            paymentFlowState = PaymentFlowState.CapturingEvidence(
                saleId = sale.saleId.toString(),
                total = sale.total
            )
        )
    }

    private fun submitTransferEvidence(imageFile: File, referenceNumber: String) {
        val sale = _state.value.sale ?: return

        if (referenceNumber.isBlank()) {
            setError(SaleError.ValidationFailed)
            return
        }

        _state.value = _state.value.copy(
            isLoading = true,
            paymentFlowState = PaymentFlowState.ProcessingPayment("transfer")
        )

        viewModelScope.launch {
            paymentProofRepository.validateReferenceNumber(referenceNumber)
                .onSuccess { isValid ->
                    if (!isValid) {
                        setError(SaleError.Server("Número de referencia duplicado"))
                        return@launch
                    }

                    val saleToCreate = sale.copy(status = "completed")
                    salesRepository.createSale(saleToCreate)
                        .onSuccess { saleId ->
                            Log.d(TAG, "Sale created in database: $saleId")

                            paymentProofRepository.uploadVoucherImage(
                                saleId = saleId,
                                imageFile = imageFile
                            ).onSuccess { voucherPath ->

                                paymentProofRepository.createPaymentProof(
                                    saleId = saleId,
                                    paymentMethod = "transfer",
                                    voucherPath = voucherPath,
                                    referenceNumber = referenceNumber,
                                    amount = sale.total,
                                    capturedBy = sale.userId.toString()
                                ).onSuccess { proof ->
                                    Log.d(TAG, "Payment proof created: ${proof.proofId}")

                                    _state.value = _state.value.copy(
                                        sale = saleToCreate,
                                        isLoading = false,
                                        paymentFlowState = PaymentFlowState.Success(saleId)
                                    )

                                }.onFailure { e ->
                                    Log.e(TAG, "Error creating proof", e)
                                    setError(SaleError.Server("Error al registrar evidencia"))
                                }

                            }.onFailure { e ->
                                Log.e(TAG, "Error uploading voucher", e)
                                setError(SaleError.Server("Error al subir comprobante"))
                            }

                        }.onFailure { e ->
                            Log.e(TAG, "Error creating sale", e)
                            setError(SaleError.Server("Error al crear la venta"))
                        }

                }.onFailure { e ->
                    Log.e(TAG, "Error validating reference", e)
                    setError(SaleError.Server("Error al validar referencia"))
                }
        }
    }

    // ═══════════════════════════════════════════════════
    // ✅ FINALIZACIÓN DE VENTA
    // ═══════════════════════════════════════════════════
    private fun finalizeSale() {
        val sale = _state.value.sale ?: return

        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            val saleToCreate = sale.copy(status = "completed")

            salesRepository.createSale(saleToCreate)
                .onSuccess { saleId ->
                    Log.d(TAG, "Sale created successfully: $saleId")

                    _state.value = _state.value.copy(
                        sale = saleToCreate,
                        isLoading = false,
                        paymentFlowState = PaymentFlowState.Success(saleId)
                    )
                }
                .onFailure { e ->
                    Log.e(TAG, "Error finalizing sale", e)
                    setError(SaleError.Server("Error al crear la venta"))
                }
        }
    }

    // ═══════════════════════════════════════════════════
    // 🔄 CICLO DE VIDA
    // ═══════════════════════════════════════════════════
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
            searchResults = emptyList(),
            productsCache = emptyMap(),
            paymentFlowState = PaymentFlowState.Idle
        )
    }

    private fun clearSale() {
        saleItems.clear()
        _state.value = SaleState()
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun getProductName(productId: Int): String {
        return _state.value.productsCache[productId]?.name ?: "Producto $productId"
    }
}