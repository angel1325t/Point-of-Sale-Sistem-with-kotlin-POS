package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.credit_notes


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.credit_notes.CreditNoteIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditApplication
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditNoteState
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.refunds.RefundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CreditNoteViewModel(
    private val repository: RefundRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreditNoteState())
    val state: StateFlow<CreditNoteState> = _state.asStateFlow()

    companion object {
        private const val TAG = "CreditNoteViewModel"
    }

    fun handleIntent(intent: CreditNoteIntent) {
        when (intent) {
            is CreditNoteIntent.SearchByInvoiceNumber -> searchByInvoiceNumber(intent.invoiceNumber)
            is CreditNoteIntent.SearchByQRCode -> searchByQRCode(intent.qrContent)
            CreditNoteIntent.ClearSearch -> clearSearch()

            is CreditNoteIntent.ApplyCreditToSale -> applyCreditToSale(
                intent.creditNoteId,
                intent.saleTotal
            )
            CreditNoteIntent.ConfirmCreditApplication -> confirmCreditApplication()

            CreditNoteIntent.ShowQRScanner -> _state.value = _state.value.copy(showQRScanner = true)
            CreditNoteIntent.HideQRScanner -> _state.value = _state.value.copy(showQRScanner = false)
            CreditNoteIntent.ClearError -> clearError()
            CreditNoteIntent.NavigateBack -> _state.value = _state.value.copy(navigateBack = true)
        }
    }

    // ═══════════════════════════════════════════════════
    // BÚSQUEDA DE NOTA DE CRÉDITO
    // ═══════════════════════════════════════════════════

    private fun searchByInvoiceNumber(invoiceNumber: String) {
        if (invoiceNumber.isBlank()) {
            setError(RefundError.Validation("Ingrese un número de factura"))
            return
        }

        Log.d(TAG, "Searching credit note by invoice: $invoiceNumber")
        _state.value = _state.value.copy(
            isLoading = true,
            searchQuery = invoiceNumber
        )

        viewModelScope.launch {
            repository.findCreditNoteByInvoiceNumber(invoiceNumber)
                .onSuccess { creditNote ->
                    if (creditNote == null) {
                        setError(RefundError.CreditNoteNotFound)
                        return@launch
                    }

                    if (creditNote.isFullyUsed) {
                        setError(RefundError.CreditNoteFullyUsed)
                        return@launch
                    }

                    Log.d(TAG, "Credit note found: ${creditNote.invoiceNumber}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        foundCreditNote = creditNote
                    )
                }
                .onFailure { e ->
                    Log.e(TAG, "Error searching credit note", e)
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
        _state.value = CreditNoteState()
    }

    // ═══════════════════════════════════════════════════
    // APLICACIÓN DE CRÉDITO
    // ═══════════════════════════════════════════════════

    private fun applyCreditToSale(creditNoteId: UUID, saleTotal: Double) {
        val creditNote = _state.value.foundCreditNote ?: return

        if (creditNote.saleId != creditNoteId) {
            setError(RefundError.Validation("ID de nota de crédito no coincide"))
            return
        }

        if (saleTotal <= 0) {
            setError(RefundError.Validation("Total de venta inválido"))
            return
        }

        // Calcular cuánto crédito aplicar
        val amountToApply = minOf(creditNote.creditRemaining, saleTotal)
        val remainingCredit = creditNote.creditRemaining - amountToApply

        val application = CreditApplication(
            creditNoteId = creditNoteId,
            amountToApply = amountToApply,
            remainingCredit = remainingCredit,
            saleTotal = saleTotal
        )

        _state.value = _state.value.copy(
            creditApplication = application,
            showConfirmationDialog = true,
            remainingPaymentRequired = application.additionalPaymentRequired
        )
    }


    private fun confirmCreditApplication() {
        val application = _state.value.creditApplication ?: return

        _state.value = _state.value.copy(
            isLoading = true,
            showConfirmationDialog = false
        )

        viewModelScope.launch {
            repository.applyCreditToSale(
                creditNoteId = application.creditNoteId,
                amountToApply = application.amountToApply
            )
                .onSuccess {
                    Log.d(TAG, "Credit applied successfully")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        showSuccessDialog = true,
                        appliedCreditNoteId = application.creditNoteId
                    )
                }
                .onFailure { e ->
                    Log.e(TAG, "Error applying credit", e)
                    setError(RefundError.Server("Error al aplicar crédito: ${e.message}"))
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