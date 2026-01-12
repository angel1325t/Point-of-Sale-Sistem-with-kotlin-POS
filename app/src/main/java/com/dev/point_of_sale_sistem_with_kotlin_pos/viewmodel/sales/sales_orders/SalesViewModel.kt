package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.sales_orders

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders.SalesIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.AppliedCreditNote
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.PaymentFlowState
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.Sale
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetail
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleState
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.credit_notes.CreditNoteRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.InvoiceRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.PaymentProofRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesProductRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.StripePaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class SalesViewModel(
    private val salesRepository: SalesRepository,
    private val salesProductRepository: SalesProductRepository,
    private val paymentProofRepository: PaymentProofRepository,
    private val stripePaymentRepository: StripePaymentRepository,
    private val invoiceRepository: InvoiceRepository,
    private val creditNoteRepository: CreditNoteRepository,
    private val cashRegisterRepository: CashRegisterRepository,
    private val activityRef: WeakReference<Activity>? = null
) : ViewModel() {

    constructor(
        salesRepository: SalesRepository,
        salesProductRepository: SalesProductRepository,
        paymentProofRepository: PaymentProofRepository,
        stripePaymentRepository: StripePaymentRepository,
        invoiceRepository: InvoiceRepository,
        creditNoteRepository: CreditNoteRepository,
        cashRegisterRepository: CashRegisterRepository,
        activity: Activity
    ) : this(
        salesRepository,
        salesProductRepository,
        paymentProofRepository,
        stripePaymentRepository,
        invoiceRepository,
        creditNoteRepository,
        cashRegisterRepository,
        WeakReference(activity)
    )

    private val _state = MutableStateFlow(SaleState())
    val state: StateFlow<SaleState> = _state.asStateFlow()

    private val saleItems = mutableListOf<SaleDetail>()

    companion object {
        private const val TAG = "SalesViewModel"
        private const val ITBIS_RATE = 0.18
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

            // 🆕 LECTOR DE CÓDIGOS
            SalesIntent.ToggleBarcodeReader -> toggleBarcodeReader()
            is SalesIntent.ProcessBarcodeFromReader -> processBarcodeFromReader(intent.barcode)

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
            is SalesIntent.ApplyCreditNote -> applyCreditNote(intent.creditNoteId, intent.amountApplied)
            is SalesIntent.RemoveCreditNote -> removeCreditNote(intent.creditNoteId)
            SalesIntent.ClearSale -> clearSale()
            SalesIntent.ClearError -> clearError()
        }
    }

    // ═══════════════════════════════════════════════════
    // 🔓 VALIDACIÓN DE CAJA ABIERTA
    // ═══════════════════════════════════════════════════

    private suspend fun validateCashRegisterOpen(): String? {
        return try {
            val openRegister = cashRegisterRepository.getOpenCashRegister()
            if (openRegister == null) {
                Log.w(TAG, "⚠️ No hay caja abierta")
                null
            } else {
                Log.d(TAG, "✅ Caja abierta: ${openRegister.history_id}")
                openRegister.history_id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validando caja abierta", e)
            null
        }
    }

// ═══════════════════════════════════════════════════
// 📟 LECTOR DE CÓDIGOS DE BARRA
// ═══════════════════════════════════════════════════

    private fun toggleBarcodeReader() {
        val newState = !_state.value.isBarcodeReaderActive
        _state.value = _state.value.copy(
            isBarcodeReaderActive = newState,
            barcodeReaderBuffer = "" // Limpiar buffer al activar/desactivar
        )
        Log.d(TAG, "Barcode reader toggled: $newState")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun processBarcodeFromReader(barcode: String) {
        if (!_state.value.isBarcodeReaderActive) return

        Log.d(TAG, "Processing barcode from reader: $barcode")

        viewModelScope.launch {
            salesProductRepository.getProductByBarcode(barcode)
                .onSuccess { product ->
                    if (product != null) {
                        Log.d(TAG, "Product found via reader: ${product.name}")

                        // Agregar producto automáticamente
                        addProductToSale(
                            SalesIntent.AddSaleDetail(
                                productId = product.productId,
                                quantity = 1,
                                unitPrice = product.price,
                                discount = 0.0
                            )
                        )

                        // Limpiar buffer
                        _state.value = _state.value.copy(barcodeReaderBuffer = "")
                    } else {
                        Log.w(TAG, "Product not found: $barcode")
                        setError(SaleError.Server("Producto no encontrado: $barcode"))
                        _state.value = _state.value.copy(barcodeReaderBuffer = "")
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error processing barcode from reader", e)
                    setError(SaleError.Server("Error al buscar producto"))
                    _state.value = _state.value.copy(barcodeReaderBuffer = "")
                }
        }
    }

    // ═══════════════════════════════════════════════════
    // 🎫 GESTIÓN DE NOTAS DE CRÉDITO
    // ═══════════════════════════════════════════════════
    private fun applyCreditNote(creditNoteId: String, amountApplied: Double) {
        viewModelScope.launch {
            // Validar que el monto no exceda el total pendiente
            val remainingToPay = _state.value.remainingToPay
            if (amountApplied > remainingToPay) {
                setError(SaleError.ValidationFailed)
                return@launch
            }

            val newCreditNote = AppliedCreditNote(
                creditNoteId = UUID.fromString(creditNoteId),
                invoiceNumber = "", // Se puede obtener del scan
                amountApplied = amountApplied
            )

            _state.value = _state.value.copy(
                appliedCreditNotes = _state.value.appliedCreditNotes + newCreditNote
            )

            Log.d(TAG, "Credit note applied: $creditNoteId, amount: $amountApplied")
        }
    }

    private fun removeCreditNote(creditNoteId: UUID) {
        _state.value = _state.value.copy(
            appliedCreditNotes = _state.value.appliedCreditNotes.filter {
                it.creditNoteId != creditNoteId
            }
        )
        Log.d(TAG, "Credit note removed: $creditNoteId")
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

        val itemsSubtotal = saleItems.sumOf { it.unitPrice * it.quantity }
        val subtotalAfterDiscount = itemsSubtotal - currentSale.globalDiscount
        val itbis = subtotalAfterDiscount * ITBIS_RATE
        val total = subtotalAfterDiscount + itbis

        _state.value = _state.value.copy(
            sale = currentSale.copy(
                saleDetails = saleItems.toList(),
                subtotal = subtotalAfterDiscount,
                itbis = itbis,
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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initiateSaleCompletion() {
        val sale = _state.value.sale

        if (sale == null || saleItems.isEmpty()) {
            setError(SaleError.ValidationFailed)
            return
        }

        // 🔐 VALIDAR QUE HAYA CAJA ABIERTA
        viewModelScope.launch {
            val cashRegisterHistoryId = validateCashRegisterOpen()

            if (cashRegisterHistoryId == null) {
                setError(SaleError.Server("⚠️ No hay caja abierta. Por favor, abre una caja antes de realizar ventas."))
                return@launch
            }

            // Actualizar sale con el ID de la caja
            _state.value = _state.value.copy(
                sale = sale.copy(cashRegisterHistoryId = cashRegisterHistoryId)
            )

            val remainingToPay = _state.value.remainingToPay
            val isCoveredByCredit = _state.value.isCoveredByCredit

            Log.d(TAG, "Initiating sale completion - Remaining: $remainingToPay, Covered by credit: $isCoveredByCredit")

            if (isCoveredByCredit) {
                finalizeSaleWithInvoice()
                return@launch
            }

            when (sale.paymentMethod) {
                "cash" -> {
                    _state.value = _state.value.copy(
                        paymentFlowState = PaymentFlowState.CashPayment(remainingToPay)
                    )
                }
                "card" -> {
                    initiateCardPayment()
                }
                "transfer" -> {
                    _state.value = _state.value.copy(
                        paymentFlowState = PaymentFlowState.TransferPayment(remainingToPay)
                    )
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // 💵 PAGO EN EFECTIVO
    // ═══════════════════════════════════════════════════
    @RequiresApi(Build.VERSION_CODES.O)
    private fun processCashPayment(amountReceived: Double) {
        val remainingToPay = _state.value.remainingToPay

        if (amountReceived < remainingToPay) {
            setError(SaleError.ValidationFailed)
            return
        }

        val change = amountReceived - remainingToPay
        Log.d(TAG, "Cash payment - Remaining: $remainingToPay, Received: $amountReceived, Change: $change")

        finalizeSaleWithInvoice()
    }

    // ═══════════════════════════════════════════════════
    // 💳 PAGO CON TARJETA (STRIPE)
    // ═══════════════════════════════════════════════════
    private fun initiateCardPayment() {
        val remainingToPay = _state.value.remainingToPay

        _state.value = _state.value.copy(
            isLoading = true,
            paymentFlowState = PaymentFlowState.CardPayment(
                total = remainingToPay,
                paymentIntentId = null,
                clientSecret = null
            )
        )

        viewModelScope.launch {
            val sale = _state.value.sale ?: return@launch

            stripePaymentRepository.createPaymentIntent(
                amount = remainingToPay,
                currency = "USD",
                saleId = sale.saleId.toString()
            ).onSuccess { intent ->
                Log.d(TAG, "Stripe Payment Intent created: $remainingToPay")

                _state.value = _state.value.copy(
                    isLoading = false,
                    paymentFlowState = PaymentFlowState.CardPayment(
                        total = remainingToPay,
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

    @RequiresApi(Build.VERSION_CODES.O)
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
                val remainingToPay = _state.value.remainingToPay
                _state.value = _state.value.copy(
                    isLoading = false,
                    paymentFlowState = PaymentFlowState.CardPayment(
                        total = remainingToPay,
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

    @RequiresApi(Build.VERSION_CODES.O)
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
                        Log.d(TAG, "Stripe payment confirmed")

                        finalizeSaleWithInvoice(
                            onSuccess = { createdSaleId ->
                                viewModelScope.launch {
                                    paymentProofRepository.createPaymentProof(
                                        saleId = createdSaleId,
                                        paymentMethod = "card",
                                        voucherPath = "stripe_${confirmation.chargeId ?: confirmation.paymentIntentId}",
                                        referenceNumber = paymentIntentId,
                                        amount = _state.value.remainingToPay,
                                        capturedBy = sale.userId.toString()
                                    ).onSuccess {
                                        Log.d(TAG, "Stripe payment proof created")
                                    }.onFailure { e ->
                                        Log.w(TAG, "Could not create payment proof (non-critical)", e)
                                    }
                                }
                            }
                        )
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
                total = _state.value.remainingToPay
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

                    paymentProofRepository.uploadVoucherImage(
                        saleId = sale.saleId.toString(),
                        imageFile = imageFile
                    ).onSuccess { voucherPath ->

                        finalizeSaleWithInvoice(
                            onSuccess = { createdSaleId ->
                                viewModelScope.launch {
                                    paymentProofRepository.createPaymentProof(
                                        saleId = createdSaleId,
                                        paymentMethod = "transfer",
                                        voucherPath = voucherPath,
                                        referenceNumber = referenceNumber,
                                        amount = _state.value.remainingToPay,
                                        capturedBy = sale.userId.toString()
                                    ).onSuccess {
                                        Log.d(TAG, "Transfer payment proof created")
                                    }.onFailure { e ->
                                        Log.e(TAG, "Error creating proof (non-critical)", e)
                                    }
                                }
                            }
                        )

                    }.onFailure { e ->
                        Log.e(TAG, "Error uploading voucher", e)
                        setError(SaleError.Server("Error al subir comprobante"))
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Error validating reference", e)
                    setError(SaleError.Server("Error al validar referencia"))
                }
        }
    }

    // ═══════════════════════════════════════════════════
    // 🧾 FINALIZACIÓN DE VENTA CON FACTURACIÓN Y CRÉDITOS
    // ═══════════════════════════════════════════════════

    @RequiresApi(Build.VERSION_CODES.O)
    private fun finalizeSaleWithInvoice(onSuccess: ((String) -> Unit)? = null) {
        val sale = _state.value.sale ?: return
        val appliedCredits = _state.value.appliedCreditNotes

        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            val saleToCreate = sale.copy(status = "completed")

            salesRepository.createSale(saleToCreate)
                .onSuccess { result ->

                    Log.d(TAG, "Sale created: ${result.saleId}, invoice: ${result.invoiceNumber}")

                    // 🎫 APLICAR NOTAS DE CRÉDITO
                    if (appliedCredits.isNotEmpty()) {
                        appliedCredits.forEach { credit ->
                            creditNoteRepository.applyCreditNote(
                                creditNoteId = credit.creditNoteId.toString(),
                                appliedToSaleId = result.saleId,
                                amountApplied = credit.amountApplied
                            ).onFailure { e ->
                                Log.e(TAG, "Error applying credit note (non-critical)", e)
                            }
                        }
                    }

                    val completedSale = saleToCreate.copy(
                        invoiceNumber = result.invoiceNumber
                    )

                    // Generar e imprimir factura
                    if (activityRef?.get() != null) {
                        val productsMap = completedSale.saleDetails.associate { detail ->
                            val name = _state.value.productsCache[detail.productId]?.name
                                ?: "Producto #${detail.productId}"
                            detail.productId.toString() to name
                        }

                        invoiceRepository.generateAndPrintInvoice(
                            sale = completedSale,
                            productsMap = productsMap
                        ).onFailure { e ->
                            Log.e(TAG, "Error generating invoice (non-critical)", e)
                        }
                    } else {
                        Log.w(TAG, "Cannot print invoice: Activity not available")
                    }

                    _state.value = _state.value.copy(
                        sale = completedSale,
                        isLoading = false,
                        paymentFlowState = PaymentFlowState.Success(
                            UUID.fromString(result.saleId)
                        )
                    )

                    onSuccess?.invoke(result.saleId)
                }
                .onFailure { e ->
                    Log.e(TAG, "Error finalizing sale", e)
                    setError(SaleError.Server("Error al crear la venta: ${e.message}"))
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
                subtotal = 0.0,
                itbis = 0.0,
                total = 0.0,
                globalDiscount = globalDiscount,
                saleDetails = emptyList()
            ),
            searchResults = emptyList(),
            productsCache = emptyMap(),
            paymentFlowState = PaymentFlowState.Idle,
            appliedCreditNotes = emptyList()
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