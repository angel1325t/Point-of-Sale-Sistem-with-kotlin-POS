package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.products.ProductsIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductsError
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.components.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsListScreen(
    viewModel: ProductViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // ✅ SOLUCIÓN: Crear un mapa de strings al inicio del Composable
    val errorMessages = remember {
        mutableMapOf<String, String>()
    }

    val successMessages = remember {
        mutableMapOf<String, String>()
    }

    // ✅ Cargar todos los strings necesarios
    errorMessages["network"] = stringResource(R.string.error_network)
    errorMessages["timeout"] = stringResource(R.string.error_timeout)
    errorMessages["no_internet"] = stringResource(R.string.error_no_internet)
    errorMessages["product_name_invalid"] = stringResource(R.string.error_product_name_invalid)
    errorMessages["product_name_too_short"] = stringResource(R.string.error_product_name_too_short)
    errorMessages["product_name_too_long"] = stringResource(R.string.error_product_name_too_long)
    errorMessages["price_invalid"] = stringResource(R.string.error_price_invalid)
    errorMessages["price_zero_or_negative"] = stringResource(R.string.error_price_zero_or_negative)
    errorMessages["stock_invalid"] = stringResource(R.string.error_stock_invalid)
    errorMessages["stock_negative"] = stringResource(R.string.error_stock_negative)
    errorMessages["category_invalid"] = stringResource(R.string.error_category_invalid)
    errorMessages["barcode_invalid"] = stringResource(R.string.error_barcode_invalid)
    errorMessages["discount_invalid"] = stringResource(R.string.error_discount_invalid)
    errorMessages["discount_value_invalid"] = stringResource(R.string.error_discount_value_invalid)
    errorMessages["discount_percentage_exceeded"] = stringResource(R.string.error_discount_percentage_exceeded)
    errorMessages["product_not_found"] = stringResource(R.string.error_product_not_found)
    errorMessages["database"] = stringResource(R.string.error_database)
    errorMessages["unauthorized"] = stringResource(R.string.error_unauthorized)
    errorMessages["create_failed"] = stringResource(R.string.error_create_product_failed)
    errorMessages["update_failed"] = stringResource(R.string.error_update_product_failed)
    errorMessages["delete_failed"] = stringResource(R.string.error_delete_product_failed)
    errorMessages["load_failed"] = stringResource(R.string.error_load_products_failed)
    errorMessages["search_failed"] = stringResource(R.string.error_search_products_failed)
    errorMessages["image_upload"] = stringResource(R.string.error_image_upload_failed)
    errorMessages["barcode_generation"] = stringResource(R.string.error_barcode_generation_failed)
    errorMessages["storage"] = stringResource(R.string.error_storage)
    errorMessages["insufficient_stock"] = stringResource(R.string.error_insufficient_stock)
    errorMessages["stock_update"] = stringResource(R.string.error_stock_update_failed)
    errorMessages["unknown_simple"] = stringResource(R.string.error_unknown_simple)

    successMessages["created"] = stringResource(R.string.product_created_success)
    successMessages["updated"] = stringResource(R.string.product_updated_success)
    successMessages["deleted"] = stringResource(R.string.product_deleted_success)

    val validationErrorFormat = stringResource(R.string.error_validation_generic)
    val duplicateBarcodeFormat = stringResource(R.string.error_duplicate_barcode)
    val unknownErrorFormat = stringResource(R.string.error_unknown)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductDTO?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ Función helper que usa el mapa de strings
    fun getErrorMessage(error: ProductsError): String {
        return when (error) {
            is ProductsError.NetworkError -> errorMessages["network"]!!
            is ProductsError.TimeoutError -> errorMessages["timeout"]!!
            is ProductsError.NoInternetConnection -> errorMessages["no_internet"]!!
            is ProductsError.ValidationError -> String.format(validationErrorFormat, error.field, error.message)
            is ProductsError.InvalidProductName -> errorMessages["product_name_invalid"]!!
            is ProductsError.ProductNameTooShort -> errorMessages["product_name_too_short"]!!
            is ProductsError.ProductNameTooLong -> errorMessages["product_name_too_long"]!!
            is ProductsError.InvalidPrice -> errorMessages["price_invalid"]!!
            is ProductsError.PriceZeroOrNegative -> errorMessages["price_zero_or_negative"]!!
            is ProductsError.InvalidStock -> errorMessages["stock_invalid"]!!
            is ProductsError.StockNegative -> errorMessages["stock_negative"]!!
            is ProductsError.InvalidCategory -> errorMessages["category_invalid"]!!
            is ProductsError.InvalidBarcode -> errorMessages["barcode_invalid"]!!
            is ProductsError.InvalidDiscount -> errorMessages["discount_invalid"]!!
            is ProductsError.DiscountValueInvalid -> errorMessages["discount_value_invalid"]!!
            is ProductsError.DiscountPercentageExceeded -> errorMessages["discount_percentage_exceeded"]!!
            is ProductsError.ProductNotFound -> errorMessages["product_not_found"]!!
            is ProductsError.DuplicateBarcode -> String.format(duplicateBarcodeFormat, error.barcode)
            is ProductsError.DatabaseError -> errorMessages["database"]!!
            is ProductsError.UnauthorizedAccess -> errorMessages["unauthorized"]!!
            is ProductsError.CreateProductFailed -> errorMessages["create_failed"]!!
            is ProductsError.UpdateProductFailed -> errorMessages["update_failed"]!!
            is ProductsError.DeleteProductFailed -> errorMessages["delete_failed"]!!
            is ProductsError.LoadProductsFailed -> errorMessages["load_failed"]!!
            is ProductsError.SearchProductsFailed -> errorMessages["search_failed"]!!
            is ProductsError.ImageUploadFailed -> errorMessages["image_upload"]!!
            is ProductsError.BarcodeGenerationFailed -> errorMessages["barcode_generation"]!!
            is ProductsError.StorageError -> errorMessages["storage"]!!
            is ProductsError.InsufficientStock -> errorMessages["insufficient_stock"]!!
            is ProductsError.StockUpdateFailed -> errorMessages["stock_update"]!!
            is ProductsError.UnknownError -> {
                if (error.message.isNullOrBlank()) {
                    errorMessages["unknown_simple"]!!
                } else {
                    String.format(unknownErrorFormat, error.message)
                }
            }
        }
    }

    fun getSuccessMessage(messageKey: String): String {
        return when (messageKey) {
            "product_created_success" -> successMessages["created"]!!
            "product_updated_success" -> successMessages["updated"]!!
            "product_deleted_success" -> successMessages["deleted"]!!
            else -> messageKey
        }
    }

    // Cargar productos al iniciar
    LaunchedEffect(Unit) {
        viewModel.handleIntent(ProductsIntent.LoadProducts)
    }

    // Mostrar mensajes de éxito/error
    LaunchedEffect(state.successMessage, state.error) {
        state.successMessage?.let { messageKey ->
            snackbarHostState.showSnackbar(getSuccessMessage(messageKey))
        }
        state.error?.let { error ->
            snackbarHostState.showSnackbar(getErrorMessage(error))
        }
    }

    // Diálogo de eliminar
    if (showDeleteDialog && productToDelete != null) {
        DeleteProductDialog(
            product = productToDelete!!,
            onConfirm = {
                viewModel.handleIntent(ProductsIntent.DeleteProduct(productToDelete!!.productId))
                showDeleteDialog = false
                productToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                productToDelete = null
            }
        )
    }

    // Strings del UI
    val titleText = stringResource(R.string.products_title)
    val backText = stringResource(R.string.action_back)
    val refreshText = stringResource(R.string.action_refresh)
    val createText = stringResource(R.string.product_create)
    val searchPlaceholder = stringResource(R.string.product_search_placeholder)
    val loadingText = stringResource(R.string.product_loading)
    val processingText = stringResource(R.string.processing)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, backText)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.handleIntent(ProductsIntent.LoadProducts)
                    }) {
                        Icon(Icons.Default.Refresh, refreshText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, createText)
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Buscador (filtrado local)
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = searchPlaceholder
            )

            // Contenido principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    state.isLoading && state.products.isEmpty() -> {
                        LoadingIndicator(message = loadingText)
                    }

                    state.error != null && state.products.isEmpty() -> {
                        ErrorMessage(
                            message = getErrorMessage(state.error!!),
                            onRetry = {
                                viewModel.handleIntent(ProductsIntent.LoadProducts)
                            }
                        )
                    }

                    state.products.isEmpty() -> {
                        EmptyProductsState(isSearching = false)
                    }

                    else -> {
                        // Filtrar productos localmente por búsqueda
                        val filteredProducts = if (searchQuery.isBlank()) {
                            state.products
                        } else {
                            state.products.filter { product ->
                                product.name.contains(searchQuery, ignoreCase = true) ||
                                        product.description?.contains(searchQuery, ignoreCase = true) == true ||
                                        product.barcode?.contains(searchQuery, ignoreCase = true) == true
                            }
                        }

                        if (filteredProducts.isEmpty()) {
                            EmptyProductsState(isSearching = true)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = filteredProducts,
                                    key = { it.productId }
                                ) { product ->
                                    ProductListItem(
                                        product = product,
                                        isLowStock = product.currentStock <= product.minimumStock && product.currentStock > 0,
                                        isOutOfStock = product.currentStock == 0,
                                        onClick = { onNavigateToEdit(product.productId) },
                                        onDelete = {
                                            productToDelete = product
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Indicador de carga superpuesto (para acciones sin bloquear UI)
                if (state.isLoading && state.products.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(processingText)
                            }
                        }
                    }
                }
            }
        }
    }
}