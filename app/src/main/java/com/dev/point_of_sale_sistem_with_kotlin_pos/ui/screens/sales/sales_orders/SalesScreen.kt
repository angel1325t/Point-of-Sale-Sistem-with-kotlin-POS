package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders.SalesIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.PaymentFlowState
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetail
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.credit_notes.AppliedCreditNotesCard
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.credit_notes.CreditNoteUsageScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.BarcodeScannerScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.CameraEvidenceScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.CashPaymentDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.ReferenceNumberDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.SaleSuccessDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.StripePaymentDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.TransferPaymentDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.utils.toUserMessage
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.credit_notes.CreditNoteUsageViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.sales_orders.SalesViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("ContextCastToActivity")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: SalesViewModel?,
    onNavigateBack: () -> Unit,
    authViewModel: AuthSessionViewModel,
    creditNoteUsageViewModel: CreditNoteUsageViewModel
) {
    val state = viewModel?.state?.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val userId = authState.userId

    // 🔥 REGISTRAR PAYMENT SHEET LAUNCHER
    val paymentSheet = rememberPaymentSheet { paymentResult ->
        viewModel?.onStripePaymentComplete(paymentResult)
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showReferenceDialog by remember { mutableStateOf(false) }
    var pendingImageFile by remember { mutableStateOf<java.io.File?>(null) }

    // 🎫 ESTADO PARA NOTAS DE CRÉDITO
    var showCreditNoteScanner by remember { mutableStateOf(false) }

    // 📟 ESTADO PARA LECTOR DE CÓDIGOS
    val isBarcodeReaderActive = state?.value?.isBarcodeReaderActive ?: false

    // Obtener valores del ViewModel state
    val appliedCreditNotes = state?.value?.appliedCreditNotes ?: emptyList()
    val totalCreditApplied = state?.value?.totalCreditApplied ?: 0.0
    val saleTotal = state?.value?.sale?.total ?: 0.0
    val remainingToPay = state?.value?.remainingToPay ?: saleTotal
    val isCoveredByCredit = state?.value?.isCoveredByCredit ?: false

    /* ---------------- INIT SALE ---------------- */
    LaunchedEffect(state?.value?.sale) {
        if (state?.value?.sale == null) {
            viewModel?.handleIntent(
                SalesIntent.CreateSale(
                    paymentMethod = "cash",
                    globalDiscount = 0.0,
                    userId = UUID.fromString(userId)
                )
            )
        }
    }

    /* ---------------- SYNC PRODUCTS FOR OFFLINE ---------------- */
    LaunchedEffect(Unit) {
        viewModel?.handleIntent(SalesIntent.SyncProducts)
    }

    /* ---------------- SEARCH ---------------- */
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2 && !isBarcodeReaderActive) {
            delay(300)
            viewModel?.handleIntent(SalesIntent.SearchProductByName(searchQuery))
        } else if (searchQuery.isBlank()) {
            viewModel?.handleIntent(SalesIntent.ClearSearchResults)
        }
    }

    /* ---------------- ERRORS ---------------- */
    LaunchedEffect(state?.value?.error) {
        state?.value?.error?.let { error ->
            snackbarHostState.showSnackbar(error.toUserMessage())
            viewModel.handleIntent(SalesIntent.ClearError)
        }
    }

    /* ---------------- CREDIT NOTE SCANNER ---------------- */
    if (showCreditNoteScanner) {
        CreditNoteUsageScreen(
            viewModel = creditNoteUsageViewModel,
            saleTotal = remainingToPay,
            onCreditApplied = { creditNoteId, amountApplied ->
                viewModel?.handleIntent(
                    SalesIntent.ApplyCreditNote(
                        creditNoteId = creditNoteId,
                        amountApplied = amountApplied
                    )
                )
                showCreditNoteScanner = false
            },
            onNavigateBack = {
                showCreditNoteScanner = false
            }
        )
        return
    }

    /* ---------------- PAYMENT FLOWS ---------------- */
    when (val flow = state?.value?.paymentFlowState) {
        is PaymentFlowState.CashPayment -> {
            CashPaymentDialog(
                total = remainingToPay,
                onDismiss = { viewModel.handleIntent(SalesIntent.ClearError) },
                onConfirm = {
                    viewModel.handleIntent(SalesIntent.ConfirmCashPayment(it))
                }
            )
        }

        is PaymentFlowState.CardPayment -> {
            LaunchedEffect(flow.clientSecret, flow.paymentIntentId) {
                if (flow.clientSecret != null && flow.paymentIntentId != null) {
                    try {
                        paymentSheet.presentWithPaymentIntent(
                            paymentIntentClientSecret = flow.clientSecret,
                            configuration = PaymentSheet.Configuration(
                                merchantDisplayName = "Mi POS",
                                allowsDelayedPaymentMethods = false
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("SalesScreen", "Error launching payment sheet", e)
                    }
                }
            }

            StripePaymentDialog(
                total = remainingToPay,
                isProcessing = flow.clientSecret != null,
                errorMessage = flow.errorMessage,
                onDismiss = {
                    viewModel.handleIntent(SalesIntent.ClearError)
                },
                onRetry = {
                    viewModel.handleIntent(SalesIntent.InitiateCardPayment)
                }
            )
        }

        is PaymentFlowState.TransferPayment -> {
            TransferPaymentDialog(
                total = remainingToPay,
                onDismiss = { viewModel.handleIntent(SalesIntent.ClearError) },
                onCaptureEvidence = {
                    viewModel.handleIntent(SalesIntent.CaptureTransferEvidence)
                }
            )
        }

        is PaymentFlowState.Success -> {
            SaleSuccessDialog(
                onConfirm = {
                    viewModel.handleIntent(SalesIntent.ClearSale)
                    onNavigateBack()
                }
            )
        }

        else -> Unit
    }

    /* ---------------- LOADING DIALOG ---------------- */
    if (state?.value?.isLoading == true &&
        state.value.paymentFlowState !is PaymentFlowState.Idle &&
        state.value.paymentFlowState !is PaymentFlowState.CashPayment &&
        state.value.paymentFlowState !is PaymentFlowState.TransferPayment &&
        state.value.paymentFlowState !is PaymentFlowState.CapturingEvidence) {
        ProcessingPaymentDialog()
    }

    /* ---------------- BARCODE SCANNER (CÁMARA) ---------------- */
    if (showBarcodeScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = {
                viewModel?.handleIntent(SalesIntent.SearchProductByBarcode(it))
                showBarcodeScanner = false
            },
            onNavigateBack = { showBarcodeScanner = false }
        )
    }

    /* ---------------- MAIN UI ---------------- */
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nueva venta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    state?.value?.sale?.let { sale ->
                        if (sale.saleDetails.isNotEmpty()) {
                            IconButton(
                                onClick = { showCreditNoteScanner = true }
                            ) {
                                if (appliedCreditNotes.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text("${appliedCreditNotes.size}")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Receipt,
                                            contentDescription = "Aplicar nota de crédito"
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = "Aplicar nota de crédito"
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            state?.value?.sale?.let { sale ->
                if (sale.saleDetails.isNotEmpty()) {
                    SalesBottomBar(
                        subtotal = sale.subtotal,
                        itbis = sale.itbis,
                        total = sale.total,
                        creditApplied = totalCreditApplied,
                        remainingToPay = remainingToPay,
                        isCoveredByCredit = isCoveredByCredit,
                        itemCount = sale.saleDetails.sumOf { it.quantity },
                        selectedPaymentMethod = sale.paymentMethod,
                        onPaymentMethodSelected = {
                            viewModel.handleIntent(SalesIntent.UpdatePaymentMethod(it))
                        },
                        onCompleteClick = {
                            viewModel.handleIntent(SalesIntent.CompleteSale)
                        },
                        onCancelClick = { showCancelDialog = true }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (!isBarcodeReaderActive) {
                        focusManager.clearFocus()
                        isSearchFocused = false
                    }
                }
        ) {
            Column(Modifier.padding(16.dp)) {
                // Mostrar créditos aplicados
                if (appliedCreditNotes.isNotEmpty()) {
                    AppliedCreditNotesCard(
                        appliedCredits = appliedCreditNotes,
                        onRemoveCredit = { creditNote ->
                            viewModel?.handleIntent(
                                SalesIntent.RemoveCreditNote(creditNote.creditNoteId)
                            )
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Search bar con Popup (solo visible cuando el lector está INACTIVO)
                if (!isBarcodeReaderActive) {
                    Box {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isSearchFocused = it.isFocused },
                            placeholder = { Text("Buscar producto") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    showBarcodeScanner = true
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }) {
                                    Icon(Icons.Default.QrCodeScanner, null)
                                }
                            }
                        )

                        // Popup con resultados de búsqueda
                        if (isSearchFocused && state?.value?.searchResults?.isNotEmpty() == true && searchQuery.isNotBlank()) {
                            Popup(
                                alignment = Alignment.TopStart,
                                properties = PopupProperties(focusable = false)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .padding(top = 60.dp)
                                        .fillMaxWidth(0.92f)
                                        .heightIn(max = 280.dp)
                                        .shadow(8.dp, RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 4.dp
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        items(
                                            items = state.value.searchResults,
                                            key = { it.productId }
                                        ) { product ->
                                            SearchResultItem(
                                                product = product,
                                                onClick = {
                                                    viewModel.handleIntent(
                                                        SalesIntent.AddSaleDetail(
                                                            productId = product.productId,
                                                            quantity = 1,
                                                            unitPrice = product.price,
                                                            discount = 0.0
                                                        )
                                                    )
                                                    searchQuery = ""
                                                    viewModel.handleIntent(SalesIntent.ClearSearchResults)
                                                    focusManager.clearFocus()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // 📟 LECTOR DE CÓDIGOS DE BARRA
                BarcodeReaderSection(
                    isActive = isBarcodeReaderActive,
                    onToggle = {
                        viewModel?.handleIntent(SalesIntent.ToggleBarcodeReader)
                        if (!isBarcodeReaderActive) {
                            // Al activar, limpiar búsqueda y focus
                            searchQuery = ""
                            focusManager.clearFocus()
                            viewModel?.handleIntent(SalesIntent.ClearSearchResults)
                        }
                    },
                    onBarcodeSubmit = { barcode ->
                        viewModel?.handleIntent(SalesIntent.ProcessBarcodeFromReader(barcode))
                    }
                )

                Spacer(Modifier.height(12.dp))

                state?.value?.sale?.let { sale ->
                    if (sale.saleDetails.isEmpty()) {
                        EmptySaleState(Modifier.fillMaxSize())
                    } else {
                        LazyColumn {
                            items(sale.saleDetails) { detail ->
                                CompactSaleItemRow(
                                    saleDetail = detail,
                                    productName = state.value.productsCache[detail.productId]?.name ?: "",
                                    onQuantityChange = {
                                        viewModel.handleIntent(
                                            SalesIntent.UpdateSaleDetail(
                                                detail.productId,
                                                it,
                                                detail.unitPrice,
                                                detail.discount
                                            )
                                        )
                                    },
                                    onRemove = {
                                        viewModel.handleIntent(
                                            SalesIntent.RemoveSaleDetail(detail.productId)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /* ---------------- CAMERA OVERLAY ---------------- */
    if (state?.value?.paymentFlowState is PaymentFlowState.CapturingEvidence) {
        val flow = state.value.paymentFlowState as PaymentFlowState.CapturingEvidence

        CameraEvidenceScreen(
            saleId = flow.saleId,
            totalAmount = flow.total,
            onPhotoCaptured = {
                pendingImageFile = it
                showReferenceDialog = true
            },
            onNavigateBack = {
                pendingImageFile = null
                showReferenceDialog = false
                viewModel.handleIntent(SalesIntent.ClearError)
            }
        )
    }

    /* ---------------- REFERENCE DIALOG ---------------- */
    if (showReferenceDialog && pendingImageFile != null) {
        ReferenceNumberDialog(
            onDismiss = {
                showReferenceDialog = false
                pendingImageFile = null
            },
            onConfirm = {
                viewModel?.handleIntent(
                    SalesIntent.SubmitTransferEvidence(
                        pendingImageFile!!,
                        it.trim()
                    )
                )
                showReferenceDialog = false
                pendingImageFile = null
            }
        )
    }

    /* ---------------- CANCEL SALE ---------------- */
    if (showCancelDialog) {
        CancelSaleDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = {
                viewModel?.handleIntent(SalesIntent.ClearSale)
                showCancelDialog = false
                onNavigateBack()
            }
        )
    }
}

// ═══════════════════════════════════════════════════
// 📟 COMPONENTE DEL LECTOR DE CÓDIGOS DE BARRA
// ═══════════════════════════════════════════════════

@Composable
private fun BarcodeReaderSection(
    isActive: Boolean,
    onToggle: () -> Unit,
    onBarcodeSubmit: (String) -> Unit
) {
    var barcodeInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 🎯 DEBOUNCE PARA DETECTAR CUANDO EL LECTOR TERMINA
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Auto-focus cuando se activa
    LaunchedEffect(isActive) {
        if (isActive) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
            barcodeInput = "" // Limpiar al activar
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Scanner,
                        contentDescription = null,
                        tint = if (isActive)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = "Lector de códigos de barra",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isActive)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isActive) {
                            Text(
                                text = "Activo - Esperando escaneo...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggle() }
                )
            }

            if (isActive) {
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { newValue ->
                        barcodeInput = newValue

                        // 🔥 CANCELAR DEBOUNCE ANTERIOR
                        debounceJob?.cancel()

                        // 🔥 CREAR NUEVO DEBOUNCE (150ms para detectar cuando el lector termina)
                        if (newValue.isNotBlank()) {
                            debounceJob = coroutineScope.launch {
                                delay(150) // Esperar a que el lector termine de escribir

                                // Si el código tiene longitud razonable (6-15 dígitos)
                                if (newValue.length in 6..15) {
                                    Log.d("BarcodeReader", "Auto-submitting: $newValue")
                                    onBarcodeSubmit(newValue.trim())
                                    barcodeInput = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            if (barcodeInput.isEmpty())
                                "Escanee el código de barra..."
                            else
                                "Procesando..."
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (barcodeInput.isNotBlank()) {
                                debounceJob?.cancel()
                                onBarcodeSubmit(barcodeInput.trim())
                                barcodeInput = ""
                            }
                        }
                    ),
                    trailingIcon = {
                        if (barcodeInput.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${barcodeInput.length}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        debounceJob?.cancel()
                                        onBarcodeSubmit(barcodeInput.trim())
                                        barcodeInput = ""
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Send,
                                        contentDescription = "Buscar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💡 El producto se agregará automáticamente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (barcodeInput.isNotEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
@SuppressLint("DefaultLocale")
@Composable
private fun SearchResultItem(
    product: ProductDTO,
    onClick: () -> Unit
) {
    val isLowStock = product.currentStock <= product.minimumStock

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                product.barcode?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "Stock: ${product.currentStock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Text(
            text = "$${String.format("%.2f", product.price)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSaleItemRow(
    saleDetail: SaleDetail,
    productName: String,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "swipe_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.White
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$${String.format("%.2f", saleDetail.unitPrice)} c/u",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quantity controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (saleDetail.quantity > 1) {
                                onQuantityChange(saleDetail.quantity - 1)
                            }
                        },
                        modifier = Modifier.size(28.dp),
                        enabled = saleDetail.quantity > 1
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Disminuir",
                            modifier = Modifier.size(16.dp),
                            tint = if (saleDetail.quantity > 1)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }

                    Text(
                        text = saleDetail.quantity.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    IconButton(
                        onClick = { onQuantityChange(saleDetail.quantity + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Aumentar",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Subtotal
                Text(
                    text = "$${String.format("%.2f", saleDetail.finalPrice)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.widthIn(min = 70.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun EmptySaleState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Sin productos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Busca o escanea para agregar",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
@Composable
fun SalesBottomBar(
    subtotal: Double,
    itbis: Double,
    total: Double,
    creditApplied: Double = 0.0,
    remainingToPay: Double = total,
    isCoveredByCredit: Boolean = false,
    itemCount: Int,
    selectedPaymentMethod: String,
    onPaymentMethodSelected: (String) -> Unit,
    onCompleteClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    var showPaymentMethods by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Resumen de totales
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subtotal:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$${String.format("%.2f", subtotal)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ITBIS (18%):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$${String.format("%.2f", itbis)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${String.format("%.2f", total)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Mostrar crédito aplicado si existe
                if (creditApplied > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Crédito aplicado:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "-$${String.format("%.2f", creditApplied)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Restante a pagar:",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isCoveredByCredit)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$${String.format("%.2f", remainingToPay)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isCoveredByCredit)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "$itemCount ${if (itemCount == 1) "artículo" else "artículos"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de método de pago (solo si hay saldo pendiente)
            if (!isCoveredByCredit && remainingToPay > 0) {
                OutlinedButton(
                    onClick = { showPaymentMethods = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = when (selectedPaymentMethod) {
                            "cash" -> Icons.Default.Money
                            "card" -> Icons.Default.CreditCard
                            "transfer" -> Icons.Default.AccountBalance
                            else -> Icons.Default.Payment
                        },
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when (selectedPaymentMethod) {
                            "cash" -> "Efectivo"
                            "card" -> "Tarjeta"
                            "transfer" -> "Transferencia"
                            else -> "Seleccionar método"
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancelar")
                }

                Button(
                    onClick = onCompleteClick,
                    modifier = Modifier.weight(1f),
                    enabled = itemCount > 0
                ) {
                    Icon(
                        imageVector = if (isCoveredByCredit)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.Payment,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isCoveredByCredit)
                            "Completar"
                        else
                            "Pagar"
                    )
                }
            }
        }
    }

    // Diálogo de selección de método de pago
    if (showPaymentMethods) {
        AlertDialog(
            onDismissRequest = { showPaymentMethods = false },
            title = { Text("Método de pago") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodOption(
                        icon = Icons.Default.Money,
                        label = "Efectivo",
                        isSelected = selectedPaymentMethod == "cash",
                        onClick = {
                            onPaymentMethodSelected("cash")
                            showPaymentMethods = false
                        }
                    )
                    PaymentMethodOption(
                        icon = Icons.Default.CreditCard,
                        label = "Tarjeta",
                        isSelected = selectedPaymentMethod == "card",
                        onClick = {
                            onPaymentMethodSelected("card")
                            showPaymentMethods = false
                        }
                    )
                    PaymentMethodOption(
                        icon = Icons.Default.AccountBalance,
                        label = "Transferencia",
                        isSelected = selectedPaymentMethod == "transfer",
                        onClick = {
                            onPaymentMethodSelected("transfer")
                            showPaymentMethods = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaymentMethods = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun PaymentMethodOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surface,
        border = if (isSelected)
            null
        else
            ButtonDefaults.outlinedButtonBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
@Composable
private fun CancelSaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.sales_cancel_confirmation_title)) },
        text = { Text(stringResource(R.string.sales_cancel_confirmation_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ProcessingPaymentDialog() {
    AlertDialog(
        onDismissRequest = { /* No dismissible */ },
        confirmButton = { },
        title = {
            Text(
                "Procesando pago",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Text(
                    text = "Guardando venta y comprobante...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    )
}