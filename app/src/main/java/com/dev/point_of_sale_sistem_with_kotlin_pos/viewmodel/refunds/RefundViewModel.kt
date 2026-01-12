package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.refunds

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.refunds.RefundIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.BusinessInfo
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds.RefundState
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.refunds.RefundRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.CreditNoteInvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class RefundViewModel(
    private val repository: RefundRepository,
    private val creditNoteInvoiceRepository: CreditNoteInvoiceRepository,
    private val userId: UUID,
) : ViewModel() {

    private val _state = MutableStateFlow(RefundState())
    val state: StateFlow<RefundState> = _state.asStateFlow()

    companion object {
        private const val TAG = "RefundViewModel"
        private const val ITBIS_RATE = 0.18 // 18% de ITBIS
    }

    init {
        Log.d(TAG, "RefundViewModel initialized with userId: $userId")
    }

    fun handleIntent(intent: RefundIntent) {
        when (intent) {
            is RefundIntent.SearchByInvoiceNumber -> searchByInvoiceNumber(intent.invoiceNumber)
            is RefundIntent.SearchByQRCode -> searchByQRCode(intent.qrContent)
            RefundIntent.ClearSearch -> clearSearch()

            is RefundIntent.SelectItemForRefund -> selectItemForRefund(
                intent.saleDetailId,
                intent.quantity
            )
            is RefundIntent.UpdateRefundQuantity -> updateRefundQuantity(
                intent.saleDetailId,
                intent.quantity
            )
            is RefundIntent.RemoveRefundItem -> removeRefundItem(intent.saleDetailId)

            RefundIntent.ProcessRefund -> processRefund()
            RefundIntent.ConfirmRefund -> confirmRefund()

            RefundIntent.ShowQRScanner -> _state.value = _state.value.copy(showQRScanner = true)
            RefundIntent.HideQRScanner -> _state.value = _state.value.copy(showQRScanner = false)
            RefundIntent.ClearError -> clearError()
            RefundIntent.NavigateBack -> _state.value = _state.value.copy(navigateBack = true)
        }
    }

    // ═══════════════════════════════════════════════════
    // BÚSQUEDA DE VENTA
    // ═══════════════════════════════════════════════════

    private fun searchByInvoiceNumber(invoiceNumber: String) {
        if (invoiceNumber.isBlank()) {
            setError(RefundError.Validation("Ingrese un número de factura"))
            return
        }

        Log.d(TAG, "Searching sale by invoice: $invoiceNumber")
        _state.value = _state.value.copy(
            isLoading = true,
            searchQuery = invoiceNumber
        )

        viewModelScope.launch {
            repository.findSaleByInvoiceNumber(invoiceNumber)
                .onSuccess { sale ->
                    if (sale == null) {
                        setError(RefundError.InvoiceNotFound)
                        return@launch
                    }

                    if (sale.isCreditNote) {
                        setError(RefundError.Validation("No se puede devolver una nota de crédito"))
                        return@launch
                    }

                    if (!sale.canBeRefunded) {
                        setError(RefundError.SaleAlreadyFullyRefunded)
                        return@launch
                    }

                    Log.d(TAG, "Sale found: ${sale.invoiceNumber}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        foundSale = sale,
                        selectedItems = emptyList()
                    )
                }
                .onFailure { e ->
                    Log.e(TAG, "Error searching sale", e)
                    setError(mapExceptionToRefundError(e))
                }
        }
    }

    private fun searchByQRCode(qrContent: String) {
        // El QR contiene solo el invoice_number
        searchByInvoiceNumber(qrContent)
        _state.value = _state.value.copy(showQRScanner = false)
    }

    private fun clearSearch() {
        _state.value = RefundState()
    }

    // ═══════════════════════════════════════════════════
    // SELECCIÓN DE ITEMS PARA DEVOLUCIÓN
    // ═══════════════════════════════════════════════════

    private fun selectItemForRefund(saleDetailId: Int, quantity: Int) {
        val sale = _state.value.foundSale ?: return
        val detail = sale.details.find { it.saleDetailId == saleDetailId } ?: return

        if (!detail.canBeRefunded) {
            setError(RefundError.Validation("Este producto ya fue devuelto completamente"))
            return
        }

        if (quantity > detail.availableQuantity) {
            setError(RefundError.ExceedsAvailableQuantity)
            return
        }

        if (quantity <= 0) {
            setError(RefundError.Validation("Cantidad debe ser mayor a 0"))
            return
        }

        val newItem = RefundItem(
            saleDetailId = detail.saleDetailId,
            productId = detail.productId,
            productName = detail.productName,
            quantityToRefund = quantity,
            unitPrice = detail.unitPrice,
            discount = (detail.discount / detail.quantity) * quantity
        )

        val updatedItems = _state.value.selectedItems
            .filter { it.saleDetailId != saleDetailId } + newItem

        // Calcular totales con ITBIS
        val itemsSubtotal = updatedItems.sumOf { (it.unitPrice * it.quantityToRefund) - it.discount }
        val itbis = itemsSubtotal * ITBIS_RATE
        val totalRefund = itemsSubtotal + itbis

        Log.d(TAG, "Refund calculation - Subtotal: $itemsSubtotal, ITBIS: $itbis, Total: $totalRefund")

        _state.value = _state.value.copy(
            selectedItems = updatedItems,
            refundSubtotal = itemsSubtotal,
            refundItbis = itbis,
            totalRefundAmount = totalRefund
        )
    }

    private fun updateRefundQuantity(saleDetailId: Int, quantity: Int) {
        if (quantity <= 0) {
            removeRefundItem(saleDetailId)
            return
        }

        selectItemForRefund(saleDetailId, quantity)
    }

    private fun removeRefundItem(saleDetailId: Int) {
        val updatedItems = _state.value.selectedItems
            .filter { it.saleDetailId != saleDetailId }

        // Recalcular totales
        val itemsSubtotal = updatedItems.sumOf { (it.unitPrice * it.quantityToRefund) - it.discount }
        val itbis = itemsSubtotal * ITBIS_RATE
        val totalRefund = itemsSubtotal + itbis

        _state.value = _state.value.copy(
            selectedItems = updatedItems,
            refundSubtotal = itemsSubtotal,
            refundItbis = itbis,
            totalRefundAmount = totalRefund
        )
    }

    // ═══════════════════════════════════════════════════
    // PROCESAMIENTO DE DEVOLUCIÓN CON IMPRESIÓN
    // ═══════════════════════════════════════════════════

    private fun processRefund() {
        if (!_state.value.canProcessRefund) {
            setError(RefundError.Validation("Seleccione productos para devolver"))
            return
        }

        _state.value = _state.value.copy(showConfirmationDialog = true)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun confirmRefund() {
        val sale = _state.value.foundSale ?: return
        val items = _state.value.selectedItems

        if (items.isEmpty()) {
            setError(RefundError.Validation("No hay productos seleccionados"))
            return
        }

        _state.value = _state.value.copy(
            isLoading = true,
            showConfirmationDialog = false
        )

        Log.d(TAG, "Creating credit note with userId: $userId")
        Log.d(TAG, "Subtotal: ${_state.value.refundSubtotal}")
        Log.d(TAG, "ITBIS: ${_state.value.refundItbis}")
        Log.d(TAG, "Total: ${_state.value.totalRefundAmount}")

        viewModelScope.launch {
            repository.createCreditNote(
                originalSaleId = sale.saleId,
                userId = userId,
                refundItems = items,
                subtotal = _state.value.refundSubtotal,
                itbis = _state.value.refundItbis,
                totalRefund = _state.value.totalRefundAmount
            )
                .onSuccess { creditNoteId ->
                    Log.d(TAG, "Credit note created successfully: $creditNoteId")

                    // 🔥 NUEVO: Obtener la venta de crédito para imprimir
                    fetchAndPrintCreditNote(creditNoteId)
                }
                .onFailure { e ->
                    Log.e(TAG, "Error creating credit note", e)
                    setError(RefundError.Server("Error al crear nota de crédito: ${e.message}"))
                }
        }
    }

    /**
     * Obtiene la nota de crédito creada y la imprime
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun fetchAndPrintCreditNote(creditNoteId: UUID) {
        viewModelScope.launch {
            // Buscar la nota de crédito por ID
            repository.findCreditNoteBySaleId(creditNoteId)
                .onSuccess { creditNoteSale ->
                    if (creditNoteSale != null) {
                        Log.d(TAG, "Fetched credit note for printing: ${creditNoteSale.invoiceNumber}")

                        // Crear mapa de productos
                        val productsMap = _state.value.selectedItems.associate {
                            it.productId.toString() to it.productName
                        }

                        // Imprimir la factura
                        printCreditNoteInvoice(creditNoteSale, productsMap)

                        // Actualizar estado como exitoso
                        _state.value = _state.value.copy(
                            isLoading = false,
                            showSuccessDialog = true,
                            createdCreditNoteId = creditNoteId
                        )
                    } else {
                        Log.w(TAG, "Credit note not found after creation")
                        setError(RefundError.Server("No se pudo recuperar la nota de crédito"))
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error fetching credit note for printing", e)
                    // Aún así marcamos como exitoso porque se creó
                    _state.value = _state.value.copy(
                        isLoading = false,
                        showSuccessDialog = true,
                        createdCreditNoteId = creditNoteId
                    )
                }
        }
    }

    /**
     * Imprime la factura de nota de crédito
     */
    private fun printCreditNoteInvoice(
        sale: RefundableSale,
        productsMap: Map<String, String>
    ) {
        viewModelScope.launch {
            creditNoteInvoiceRepository
                .generateAndPrintCreditNoteInvoice(
                    sale = sale,
                    productsMap = productsMap
                )
                .onSuccess { pdfFile ->
                    Log.d(TAG, "Credit note invoice printed: ${pdfFile.absolutePath}")
                }
                .onFailure { e ->
                    Log.e(TAG, "Error printing credit note invoice", e)
                    // No bloqueamos el flujo si falla la impresión
                }
        }
    }


    // ═══════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════

    private fun setError(error: RefundError) {
        _state.value = _state.value.copy(
            isLoading = false,
            error = error
        )
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun mapExceptionToRefundError(throwable: Throwable): RefundError {
        return when (throwable) {
            is java.net.UnknownHostException -> RefundError.Network
            is IllegalArgumentException -> RefundError.Validation(throwable.message ?: "Error de validación")
            else -> RefundError.Unknown(throwable)
        }
    }
}