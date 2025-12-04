package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders.SalesIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleError
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.sales_orders.SalesViewModel
import java.util.UUID

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
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val userId = authState.userId
    val branchId = authState.branchId

    var searchQuery by remember { mutableStateOf("") }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<ProductDTO?>(null) }

    // String resources for error messages
    val errorNetworkMsg = stringResource(R.string.error_network)
    val errorValidationMsg = stringResource(R.string.sales_error_validation)
    val errorUnknownMsg = stringResource(R.string.error_unknown_simple)

    // Initialize sale if not exists
    LaunchedEffect(state.sale) {
        if (state.sale == null) {
            viewModel.handleIntent(
                SalesIntent.CreateSale(
                    paymentMethod = "cash",
                    globalDiscount = 0.0,
                    userId = UUID.fromString(userId),
                ),
            )
        }
    }

    // Handle errors
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val message = when (error) {
                is SaleError.Network -> errorNetworkMsg
                is SaleError.ValidationFailed -> errorValidationMsg
                is SaleError.Server -> error.message
                is SaleError.Unknown -> errorUnknownMsg
            }
            snackbarHostState.showSnackbar(message)
            viewModel.handleIntent(SalesIntent.ClearError)
        }
    }

    // Auto-select product when found by barcode
    LaunchedEffect(state.searchResults, state.lastScannedBarcode) {
        if (state.searchResults.size == 1 && state.lastScannedBarcode != null) {
            selectedProduct = state.searchResults.first()
            showAddProductDialog = true
            viewModel.handleIntent(SalesIntent.ClearSearchResults)
        }
    }

    // Show barcode scanner screen
    if (showBarcodeScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { barcode ->
                viewModel.handleIntent(SalesIntent.SearchProductByBarcode(barcode))
                showBarcodeScanner = false
            },
            onNavigateBack = { showBarcodeScanner = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sales_new)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.sale != null && state.sale!!.saleDetails.isNotEmpty()) {
                SalesBottomBar(
                    onCompleteClick = { showPaymentDialog = true },
                    onCancelClick = { showCancelDialog = true }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search bar for products
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Buscar producto por nombre...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    Row {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar"
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                showBarcodeScanner = true
                                keyboardController?.hide()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Escanear código"
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.handleIntent(
                                SalesIntent.SearchProductByName(searchQuery)
                            )
                            keyboardController?.hide()
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search results or add button
            if (state.searchResults.isNotEmpty() && searchQuery.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(
                            items = state.searchResults,
                            key = { it.productId }
                        ) { product ->
                            ProductSearchResultItem(
                                product = product,
                                onClick = {
                                    selectedProduct = product
                                    showAddProductDialog = true
                                    searchQuery = ""
                                    viewModel.handleIntent(SalesIntent.ClearSearchResults)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sale content
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                state.sale?.let { sale ->
                    if (sale.saleDetails.isEmpty()) {
                        EmptySaleState(
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = sale.saleDetails,
                                key = { it.saleDetailId }
                            ) { detail ->
                                SaleItemCard(
                                    saleDetail = detail,
                                    productName = "Producto ${detail.productId}",
                                    onQuantityChange = { newQuantity ->
                                        viewModel.handleIntent(
                                            SalesIntent.UpdateSaleDetail(
                                                productId = detail.productId,
                                                quantity = newQuantity,
                                                unitPrice = detail.unitPrice,
                                                discount = detail.discount
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

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                SalesSummaryCard(
                                    itemsCount = sale.saleDetails.size,
                                    subtotal = sale.saleDetails.sumOf {
                                        it.unitPrice * it.quantity
                                    },
                                    discount = sale.saleDetails.sumOf { it.discount },
                                    total = sale.total
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add product to sale dialog
    selectedProduct?.let { product ->
        if (showAddProductDialog) {
            AddProductToSaleDialog(
                product = product,
                onDismiss = {
                    showAddProductDialog = false
                    selectedProduct = null
                },
                onConfirm = { quantity, discount ->
                    viewModel.handleIntent(
                        SalesIntent.AddSaleDetail(
                            productId = product.productId,
                            quantity = quantity,
                            unitPrice = product.price,
                            discount = discount
                        )
                    )
                    showAddProductDialog = false
                    selectedProduct = null
                }
            )
        }
    }

    // Payment dialog
    if (showPaymentDialog) {
        PaymentDialog(
            currentMethod = state.sale?.paymentMethod ?: "cash",
            onDismiss = { showPaymentDialog = false },
            onConfirm = { paymentMethod ->
                viewModel.handleIntent(SalesIntent.CompleteSale)
                showPaymentDialog = false
            }
        )
    }

    // Cancel confirmation dialog
    if (showCancelDialog) {
        CancelSaleDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = {
                viewModel.handleIntent(SalesIntent.CancelSale)
                showCancelDialog = false
                onNavigateBack()
            }
        )
    }
}

// ... resto del código igual (ProductSearchResultItem, EmptySaleState, etc.)

@Composable
private fun ProductSearchResultItem(
    product: ProductDTO,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(product.name) },
        supportingContent = {
            Column {
                product.barcode?.let { barcode ->
                    Text("Código: $barcode", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "Stock: ${product.currentStock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.currentStock > product.minimumStock)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        },
        trailingContent = {
            Text(
                text = "$${product.price}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun EmptySaleState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingBag,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Venta vacía",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Busca productos para agregar a la venta",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AddProductToSaleDialog(
    product: ProductDTO,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, discount: Double) -> Unit
) {
    var quantity by remember { mutableStateOf("1") }
    var discount by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Agregar a la venta")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Precio: $${product.price}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it.filter { char -> char.isDigit() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = discount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            discount = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descuento ($)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true
                )

                val qty = quantity.toIntOrNull() ?: 0
                val disc = discount.toDoubleOrNull() ?: 0.0
                val subtotal = (product.price * qty) - disc

                if (qty > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subtotal",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$${"%.2f".format(subtotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    val disc = discount.toDoubleOrNull() ?: 0.0
                    if (qty > 0 && qty <= product.currentStock) {
                        onConfirm(qty, disc)
                    }
                },
                enabled = (quantity.toIntOrNull() ?: 0) > 0 &&
                        (quantity.toIntOrNull() ?: 0) <= product.currentStock
            ) {
                Text("Agregar")
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
private fun SalesBottomBar(
    onCompleteClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.sales_cancel))
            }

            Button(
                onClick = onCompleteClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.sales_complete))
            }
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
        title = {
            Text(stringResource(R.string.sales_complete_confirmation_title))
        },
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
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(stringResource(R.string.sales_cancel_confirmation_title))
        },
        text = {
            Text(stringResource(R.string.sales_cancel_confirmation_message))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
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