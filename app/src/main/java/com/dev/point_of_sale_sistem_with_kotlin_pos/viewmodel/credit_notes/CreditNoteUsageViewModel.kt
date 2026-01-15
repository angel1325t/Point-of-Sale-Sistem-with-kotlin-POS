package com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.credit_notes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.credit_notes.CreditNoteIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditNoteUsageState
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.ScannedCreditNote
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.credit_notes.CreditNoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreditNoteUsageViewModel(
    private val repository: CreditNoteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreditNoteUsageState())
    val state: StateFlow<CreditNoteUsageState> = _state.asStateFlow()

    companion object {
        private const val TAG = "CreditNoteUsageVM"
    }

    /**
     * Punto de entrada único para todas las acciones (MVI)
     */
    fun handleIntent(intent: CreditNoteIntent) {
        when (intent) {
            is CreditNoteIntent.SearchByInvoiceNumber ->
                searchByInvoiceNumber(intent.invoiceNumber)

            is CreditNoteIntent.SearchByQRCode ->
                searchByQRCode(intent.qrContent)
            is CreditNoteIntent.SetSaleTotal -> {
                _state.value = state.value.copy(saleTotal = intent.saleTotal)
            }


            is CreditNoteIntent.AmountChanged -> {
                _state.value = _state.value.copy(
                    amountToApply = intent.amount
                )
            }

            is CreditNoteIntent.ApplyCreditToSale ->
                prepareApplyCredit(intent.creditNoteId, intent.saleTotal)

            CreditNoteIntent.ConfirmCreditApplication ->
                confirmCreditApplication()

            CreditNoteIntent.ShowQRScanner ->
                _state.value = _state.value.copy(showQRScanner = true)

            CreditNoteIntent.HideQRScanner ->
                _state.value = _state.value.copy(showQRScanner = false)

            CreditNoteIntent.ClearError ->
                clearError()

            CreditNoteIntent.NavigateBack ->
                _state.value = _state.value.copy(navigateBack = true)
        }
    }

    /**
     * Busca nota de crédito por número de factura (desde QR)
     */
    private fun searchByQRCode(qrContent: String) {
        val invoiceNumber = extractInvoiceFromQR(qrContent)
        if (invoiceNumber != null) {
            searchByInvoiceNumber(invoiceNumber)
        } else {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "QR inválido o no reconocido",
                showQRScanner = false
            )
        }
    }

    /**
     * Busca nota de crédito por invoice number
     */
    private fun searchByInvoiceNumber(invoiceNumber: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                showQRScanner = false
            )

            Log.d(TAG, "Searching credit note by invoice: $invoiceNumber")

            repository.getCreditNoteByInvoice(invoiceNumber)
                .onSuccess { creditNote ->
                    if (creditNote == null) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Nota de crédito no encontrada"
                        )
                        return@onSuccess
                    }

                    if (creditNote.creditRemaining <= 0) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Esta nota de crédito ya ha sido utilizada completamente"
                        )
                        return@onSuccess
                    }

                    _state.value = _state.value.copy(
                        isLoading = false,
                        scannedCreditNote = ScannedCreditNote(
                            saleId = creditNote.saleId,
                            invoiceNumber = creditNote.invoiceNumber,
                            creditRemaining = creditNote.creditRemaining
                        )
                    )

                    Log.d(TAG, "Credit note loaded - Invoice: ${creditNote.invoiceNumber}, Remaining: ${creditNote.creditRemaining}")
                }
                .onFailure { e ->
                    Log.e(TAG, "Error fetching credit note", e)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Error al buscar la nota de crédito: ${e.message}"
                    )
                }
        }
    }

    /**
     * Extrae el invoice number del QR
     * Formatos soportados:
     * - INV-CN-20260105-22
     * - {"invoiceNumber": "INV-CN-20260105-22"}
     * - URL con invoice al final
     */
    private fun extractInvoiceFromQR(qrContent: String): String? {
        val trimmed = qrContent.trim()

        return when {
            // Formato directo: INV-CN-YYYYMMDD-XX
            trimmed.matches(Regex("INV-CN-\\d{8}-\\d+", RegexOption.IGNORE_CASE)) -> {
                Log.d(TAG, "QR format: Direct invoice number")
                trimmed
            }

            // Formato JSON
            trimmed.startsWith("{") && trimmed.contains("invoiceNumber") -> {
                Log.d(TAG, "QR format: JSON")
                val match = Regex("\"invoiceNumber\"\\s*:\\s*\"([^\"]+)\"").find(trimmed)
                match?.groupValues?.get(1)
            }

            // Formato URL
            trimmed.contains("/") && trimmed.contains("INV-CN") -> {
                Log.d(TAG, "QR format: URL")
                val match = Regex("(INV-CN-\\d{8}-\\d+)").find(trimmed)
                match?.value
            }

            else -> {
                Log.e(TAG, "Invalid QR format: $trimmed")
                null
            }
        }
    }

    /**
     * Prepara la aplicación del crédito
     */
    private fun prepareApplyCredit(creditNoteId: String, saleTotal: Double) {
        val creditNote = _state.value.scannedCreditNote
        if (creditNote == null) {
            _state.value = _state.value.copy(
                error = "No hay nota de crédito seleccionada"
            )
            return
        }

        _state.value = _state.value.copy(
            saleTotal = saleTotal,
            showConfirmationDialog = true
        )
    }

    /**
     * Confirma y aplica el crédito
     */
    private fun confirmCreditApplication() {
        val creditNote = _state.value.scannedCreditNote ?: return
        val amount = _state.value.amountToApply

        if (amount <= 0 || amount > _state.value.maxApplicableAmount) {
            _state.value = _state.value.copy(
                error = "Monto inválido para aplicar"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                showConfirmationDialog = false
            )

            // La aplicación real del crédito se hace en SalesViewModel
            // Aquí solo marcamos como exitoso
            _state.value = _state.value.copy(
                isLoading = false,
                appliedSuccessfully = true,
                showSuccessDialog = true
            )
        }
    }

    private fun clearSearch() {
        _state.value = CreditNoteUsageState()
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}