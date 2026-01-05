package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders.SalesIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.AppliedCreditNote
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
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.credit_notes.CreditNoteViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.sales_orders.SalesViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet
import kotlinx.coroutines.delay
import java.util.UUID

@SuppressLint("ContextCastToActivity")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: SalesViewModel?,
    onNavigateBack: () -> Unit,
    authViewModel: AuthSessionViewModel,
    creditNoteViewModel: CreditNoteViewModel // Pasar desde navegación
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

    // 🎫 ESTADO LOCAL PARA NOTAS DE CRÉDITO (sin modificar SalesViewModel)
    var showCreditNoteScanner by remember { mutableStateOf(false) }
    var appliedCreditNotes by remember { mutableStateOf<List<AppliedCreditNote>>(emptyList()) }

    // Cálculos derivados de créditos
    val totalCreditApplied = appliedCreditNotes.sumOf { it.amountApplied }
    val saleTotal = state?.value?.sale?.total ?: 0.0
    val remainingToPay = maxOf(0.0, saleTotal - totalCreditApplied)
    val isCoveredByCredit = remainingToPay <= 0.0

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

    /* ---------------- SEARCH ---------------- */
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
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
            viewModel = creditNoteViewModel,
            saleTotal = remainingToPay, // Pasar el saldo restante
            onCreditApplied = { creditNoteId, amountApplied ->
                // Agregar crédito aplicado a la lista local
                appliedCreditNotes = appliedCreditNotes + AppliedCreditNote(
                    creditNoteId = creditNoteId,
                    invoiceNumber = "", // Opcional: puedes guardarlo si lo necesitas
                    amountApplied = amountApplied
                )
                showCreditNoteScanner = false
            },
            onNavigateBack = {
                showCreditNoteScanner = false
            }
        )
        return // Salir del composable para mostrar solo el scanner
    }

    /* ---------------- PAYMENT FLOWS ---------------- */
    when (val flow = state?.value?.paymentFlowState) {
        is PaymentFlowState.CashPayment -> {
            CashPaymentDialog(
                total = remainingToPay, // Usar saldo restante
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
                total = remainingToPay, // Usar saldo restante
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
                total = remainingToPay, // Usar saldo restante
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
                    appliedCreditNotes = emptyList() // Limpiar créditos
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

    /* ---------------- BARCODE SCANNER ---------------- */
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
                    // Botón para aplicar notas de crédito
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
                    focusManager.clearFocus()
                    isSearchFocused = false
                }
        ) {
            Column(Modifier.padding(16.dp)) {
                // Mostrar créditos aplicados
                if (appliedCreditNotes.isNotEmpty()) {
                    AppliedCreditNotesCard(
                        appliedCredits = appliedCreditNotes,
                        onRemoveCredit = { creditNote ->
                            appliedCreditNotes = appliedCreditNotes.filter {
                                it.creditNoteId != creditNote.creditNoteId
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Search bar con Popup
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
                appliedCreditNotes = emptyList() // Limpiar créditos
                showCancelDialog = false
                onNavigateBack()
            }
        )
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
    creditApplied: Double = 0.0, // NUEVO
    remainingToPay: Double = total, // NUEVO
    isCoveredByCredit: Boolean = false, // NUEVO
    itemCount: Int,
    selectedPaymentMethod: String,
    onPaymentMethodSelected: (String) -> Unit,
    onCompleteClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Resumen de montos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                // Cantidad de items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Items:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$itemCount",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Subtotal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subtotal:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${"%.2f".format(subtotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ITBIS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ITBIS (18%):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${"%.2f".format(itbis)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Divider()

                Spacer(modifier = Modifier.height(8.dp))

                // Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL:",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$${"%.2f".format(total)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // NUEVO: Mostrar crédito aplicado
                if (creditApplied > 0) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Crédito aplicado:",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "-$${"%.2f".format(creditApplied)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Divider()

                    Spacer(modifier = Modifier.height(8.dp))

                    // Saldo a pagar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCoveredByCredit) "Cubierto:" else "Saldo a pagar:",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isCoveredByCredit)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "$${"%.2f".format(remainingToPay)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isCoveredByCredit)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Métodos de pago (solo si no está cubierto por crédito)
            if (!isCoveredByCredit) {
                Text(
                    text = "Método de pago ${if (creditApplied > 0) "(saldo restante)" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodButton(
                        icon = Icons.Default.Payments,
                        label = "Efectivo",
                        isSelected = selectedPaymentMethod == "cash",
                        onClick = { onPaymentMethodSelected("cash") },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentMethodButton(
                        icon = Icons.Default.CreditCard,
                        label = "Tarjeta",
                        isSelected = selectedPaymentMethod == "card",
                        onClick = { onPaymentMethodSelected("card") },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentMethodButton(
                        icon = Icons.Default.AccountBalance,
                        label = "Transfer.",
                        isSelected = selectedPaymentMethod == "transfer",
                        onClick = { onPaymentMethodSelected("transfer") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Mensaje de cubierto por crédito
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Venta cubierta con nota de crédito",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Cancelar")
                }

                Button(
                    onClick = onCompleteClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCoveredByCredit)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (isCoveredByCredit) "Finalizar" else "Completar")
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        },
        modifier = modifier.height(70.dp)
    )
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