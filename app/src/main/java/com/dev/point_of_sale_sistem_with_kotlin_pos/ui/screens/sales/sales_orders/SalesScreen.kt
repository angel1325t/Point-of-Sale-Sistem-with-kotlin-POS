    package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders
    
    import android.os.Build
    import androidx.annotation.RequiresApi
    import androidx.compose.animation.*
    import androidx.compose.animation.core.*
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
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.compose.ui.window.Popup
    import androidx.compose.ui.window.PopupProperties
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import com.dev.point_of_sale_sistem_with_kotlin_pos.R
    import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.sales_orders.SalesIntent
    import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
    import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleDetail
    import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.sales_orders.SaleError
    import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.BarcodeScannerScreen
    import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.PaymentMethodSelector
    import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
    import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.sales_orders.SalesViewModel
    import kotlinx.coroutines.delay
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
        val focusManager = LocalFocusManager.current
    
        val userId = authState.userId
    
        var searchQuery by remember { mutableStateOf("") }
        var isSearchFocused by remember { mutableStateOf(false) }
        var showPaymentDialog by remember { mutableStateOf(false) }
        var showCancelDialog by remember { mutableStateOf(false) }
        var showBarcodeScanner by remember { mutableStateOf(false) }
    
        // String resources
        val errorNetworkMsg = stringResource(R.string.error_network)
        val errorValidationMsg = stringResource(R.string.sales_error_validation)
        val errorUnknownMsg = stringResource(R.string.error_unknown_simple)
    
        // Initialize sale
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
    
        // Debounced search - searches as user types
        LaunchedEffect(searchQuery) {
            if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
                delay(300) // Debounce 300ms
                viewModel.handleIntent(SalesIntent.SearchProductByName(searchQuery))
            } else if (searchQuery.isBlank()) {
                viewModel.handleIntent(SalesIntent.ClearSearchResults)
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
    
        // Auto-add product when scanned by barcode
        LaunchedEffect(state.searchResults, state.lastScannedBarcode) {
            if (state.searchResults.size == 1 && state.lastScannedBarcode != null) {
                val product = state.searchResults.first()
                viewModel.handleIntent(
                    SalesIntent.AddSaleDetail(
                        productId = product.productId,
                        quantity = 1,
                        unitPrice = product.price,
                        discount = 0.0
                    )
                )
                viewModel.handleIntent(SalesIntent.ClearSearchResults)
            }
        }
    
        // Barcode scanner screen
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
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                state.sale?.let { sale ->
                    if (sale.saleDetails.isNotEmpty()) {
                        SalesBottomBar(
                            total = sale.total,
                            itemCount = sale.saleDetails.sumOf { it.quantity },
                            onCompleteClick = { showPaymentDialog = true },
                            onCancelClick = { showCancelDialog = true }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                        isSearchFocused = false
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
    
                    // Search bar
                    Box {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isSearchFocused = it.isFocused },
                            placeholder = { Text("Buscar producto...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            viewModel.handleIntent(SalesIntent.ClearSearchResults)
                                        }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Limpiar",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        showBarcodeScanner = true
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "Escanear",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
    
                        // Floating search results dropdown
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
    
                    Spacer(modifier = Modifier.height(12.dp))
    
                    // Sale content
                    if (state.isLoading && state.sale?.saleDetails.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        }
                    } else {
                        state.sale?.let { sale ->
                            if (sale.saleDetails.isEmpty()) {
                                EmptySaleState(modifier = Modifier.weight(1f))
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(
                                        items = sale.saleDetails,
                                        key = { it.saleDetailId }
                                    ) { detail ->
                                        val product = state.searchResults.find { it.productId == detail.productId }
                                        CompactSaleItemRow(
                                            saleDetail = detail,
                                            productName = product?.name ?: "Producto",
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
                                }
                            }
                        }
                    }
                }
            }
        }
    
        // Payment dialog
        if (showPaymentDialog) {
            PaymentDialog(
                currentMethod = state.sale?.paymentMethod ?: "cash",
                onDismiss = { showPaymentDialog = false },
                onConfirm = {
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
    
                Spacer(modifier = Modifier.height(12.dp))
    
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
                        Text(stringResource(R.string.sales_complete))
                    }
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