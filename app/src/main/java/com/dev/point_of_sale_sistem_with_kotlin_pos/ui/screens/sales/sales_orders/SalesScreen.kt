package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders.SalesIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.PaymentFlowState
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetail
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleError
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.utils.InvoicePrinter
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.BarcodeScannerScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.CameraEvidenceScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.CashPaymentDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.PaymentMethodSelector
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.ReferenceNumberDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.StripePaymentDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.TransferPaymentDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.utils.toUserMessage
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
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
    viewModel: SalesViewModel,
    onNavigateBack: () -> Unit,
    authViewModel: AuthSessionViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsState()
    val context = LocalContext.current



    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val userId = authState.userId

    // 🔥 REGISTRAR PAYMENT SHEET LAUNCHER
    val paymentSheet = rememberPaymentSheet { paymentResult ->
        viewModel.onStripePaymentComplete(paymentResult)
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showReferenceDialog by remember { mutableStateOf(false) }
    var pendingImageFile by remember { mutableStateOf<java.io.File?>(null) }

    /* ---------------- INIT SALE ---------------- */
    LaunchedEffect(state.sale) {
        if (state.sale == null) {
            viewModel.handleIntent(
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
            viewModel.handleIntent(SalesIntent.SearchProductByName(searchQuery))
        } else if (searchQuery.isBlank()) {
            viewModel.handleIntent(SalesIntent.ClearSearchResults)
        }
    }

    /* ---------------- ERRORS ---------------- */
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error.toUserMessage())
            viewModel.handleIntent(SalesIntent.ClearError)
        }
    }

    /* ---------------- PAYMENT FLOW DIALOGS ---------------- */
    val lastInvoiceFile by viewModel.lastInvoiceFile.collectAsStateWithLifecycle()
    val activity = context as? Activity
 
    when (val flow = state.paymentFlowState) {
        is PaymentFlowState.CashPayment -> {
            CashPaymentDialog(
                total = flow.total,
                onDismiss = { viewModel.handleIntent(SalesIntent.ClearError) },
                onConfirm = {
                    viewModel.handleIntent(SalesIntent.ConfirmCashPayment(it))
                }
            )
        }

        is PaymentFlowState.CardPayment -> {
            // 🔥 LANZAR STRIPE PAYMENT SHEET
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
                total = flow.total,
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
                total = flow.total,
                onDismiss = { viewModel.handleIntent(SalesIntent.ClearError) },
                onCaptureEvidence = {
                    viewModel.handleIntent(SalesIntent.CaptureTransferEvidence)
                }
            )
        }

        // 🔥 NUEVO: Diálogo de éxito con opción de reimprimir
        is PaymentFlowState.Success -> {
            SaleSuccessDialog(
                onDismiss = {
                    viewModel.handleIntent(SalesIntent.ClearSale)
                    onNavigateBack()
                },
                onReprintInvoice = {
                    val file = lastInvoiceFile
                    if (file != null && activity != null) {
                        InvoicePrinter(activity).printInvoice(
                            pdfFile = file,
                            jobName = "Factura #${file.name.takeLast(8)}"
                        )
                    }
                }
            )
        }


        else -> Unit
    }

    LaunchedEffect(lastInvoiceFile) {
        val file = lastInvoiceFile
        if (file != null && activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            InvoicePrinter(activity).printInvoice(
                pdfFile = file,
                jobName = "Factura #${file.name.takeLast(8)}"
            )
        }
    }


    /* ---------------- LOADING DIALOG ---------------- */
    if (state.isLoading &&
        state.paymentFlowState !is PaymentFlowState.Idle &&
        state.paymentFlowState !is PaymentFlowState.CashPayment &&
        state.paymentFlowState !is PaymentFlowState.TransferPayment &&
        state.paymentFlowState !is PaymentFlowState.CapturingEvidence) {
        ProcessingPaymentDialog()
    }

    /* ---------------- BARCODE SCANNER ---------------- */
    if (showBarcodeScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = {
                viewModel.handleIntent(SalesIntent.SearchProductByBarcode(it))
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
                }
            )
        },
        bottomBar = {
            state.sale?.let { sale ->
                if (sale.saleDetails.isNotEmpty()) {
                    SalesBottomBar(
                        total = sale.total,
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
                    if (isSearchFocused && state.searchResults.isNotEmpty() && searchQuery.isNotBlank()) {
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
                                        items = state.searchResults,
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

                state.sale?.let { sale ->
                    if (sale.saleDetails.isEmpty()) {
                        EmptySaleState(Modifier.fillMaxSize())
                    } else {
                        LazyColumn {
                            items(sale.saleDetails) { detail ->
                                CompactSaleItemRow(
                                    saleDetail = detail,
                                    productName = state.productsCache[detail.productId]?.name ?: "",
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
    if (state.paymentFlowState is PaymentFlowState.CapturingEvidence) {
        val flow = state.paymentFlowState as PaymentFlowState.CapturingEvidence

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
                viewModel.handleIntent(
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
                viewModel.handleIntent(SalesIntent.ClearSale)
                showCancelDialog = false
                onNavigateBack()
            }
        )
    }
}

// 🔥 NUEVO: Diálogo de venta exitosa con reimpresión
@Composable
fun SaleSuccessDialog(
    onDismiss: () -> Unit,
    onReprintInvoice: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "¡Venta Completada!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "La venta se ha registrado exitosamente.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "La factura se ha enviado a imprimir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Finalizar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReprintInvoice,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Default.Print,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Reimprimir")
            }
        }
    )
}

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
private fun SalesBottomBar(
    total: Double,
    itemCount: Int,
    selectedPaymentMethod: String,
    onPaymentMethodSelected: (String) -> Unit,
    onCompleteClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Total Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$itemCount ${if (itemCount == 1) "producto" else "productos"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$${String.format("%.2f", total)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method Selector
            Text(
                text = "Método de pago",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodChip(
                    icon = Icons.Default.Payments,
                    label = "Efectivo",
                    value = "cash",
                    isSelected = selectedPaymentMethod == "cash",
                    onClick = { onPaymentMethodSelected("cash") },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodChip(
                    icon = Icons.Default.CreditCard,
                    label = "Tarjeta",
                    value = "card",
                    isSelected = selectedPaymentMethod == "card",
                    onClick = { onPaymentMethodSelected("card") },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodChip(
                    icon = Icons.Default.AccountBalance,
                    label = "Transfer.",
                    value = "transfer",
                    isSelected = selectedPaymentMethod == "transfer",
                    onClick = { onPaymentMethodSelected("transfer") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(0.4f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.sales_cancel))
                }
                Button(
                    onClick = onCompleteClick,
                    modifier = Modifier.weight(0.6f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.sales_complete))
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PaymentDialog(
    currentMethod: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(currentMethod) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sales_complete_confirmation_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.sales_complete_confirmation_message),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PaymentMethodSelector(
                    selectedMethod = selectedMethod,
                    onMethodSelected = { selectedMethod = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMethod) },
                enabled = selectedMethod.isNotBlank()
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