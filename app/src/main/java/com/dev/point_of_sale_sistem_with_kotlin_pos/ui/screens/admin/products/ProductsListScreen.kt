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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductDTO?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Función helper para obtener el mensaje de error (NO @Composable)
    fun getErrorMessage(error: ProductsError): String {
        return when (error) {
            // Errores de red
            is ProductsError.NetworkError -> context.getString(R.string.error_network)
            is ProductsError.TimeoutError -> context.getString(R.string.error_timeout)
            is ProductsError.NoInternetConnection -> context.getString(R.string.error_no_internet)

            // Errores de validación
            is ProductsError.ValidationError -> context.getString(R.string.error_validation_generic, error.field, error.message)
            is ProductsError.InvalidProductName -> context.getString(R.string.error_product_name_invalid)
            is ProductsError.ProductNameTooShort -> context.getString(R.string.error_product_name_too_short)
            is ProductsError.ProductNameTooLong -> context.getString(R.string.error_product_name_too_long)
            is ProductsError.InvalidPrice -> context.getString(R.string.error_price_invalid)
            is ProductsError.PriceZeroOrNegative -> context.getString(R.string.error_price_zero_or_negative)
            is ProductsError.InvalidStock -> context.getString(R.string.error_stock_invalid)
            is ProductsError.StockNegative -> context.getString(R.string.error_stock_negative)
            is ProductsError.InvalidCategory -> context.getString(R.string.error_category_invalid)
            is ProductsError.InvalidBarcode -> context.getString(R.string.error_barcode_invalid)
            is ProductsError.InvalidDiscount -> context.getString(R.string.error_discount_invalid)
            is ProductsError.DiscountValueInvalid -> context.getString(R.string.error_discount_value_invalid)
            is ProductsError.DiscountPercentageExceeded -> context.getString(R.string.error_discount_percentage_exceeded)

            // Errores de base de datos
            is ProductsError.ProductNotFound -> context.getString(R.string.error_product_not_found)
            is ProductsError.DuplicateBarcode -> context.getString(R.string.error_duplicate_barcode, error.barcode)
            is ProductsError.DatabaseError -> context.getString(R.string.error_database)
            is ProductsError.UnauthorizedAccess -> context.getString(R.string.error_unauthorized)

            // Errores de operaciones
            is ProductsError.CreateProductFailed -> context.getString(R.string.error_create_product_failed)
            is ProductsError.UpdateProductFailed -> context.getString(R.string.error_update_product_failed)
            is ProductsError.DeleteProductFailed -> context.getString(R.string.error_delete_product_failed)
            is ProductsError.LoadProductsFailed -> context.getString(R.string.error_load_products_failed)
            is ProductsError.SearchProductsFailed -> context.getString(R.string.error_search_products_failed)

            // Errores de storage
            is ProductsError.ImageUploadFailed -> context.getString(R.string.error_image_upload_failed)
            is ProductsError.BarcodeGenerationFailed -> context.getString(R.string.error_barcode_generation_failed)
            is ProductsError.StorageError -> context.getString(R.string.error_storage)

            // Errores de stock
            is ProductsError.InsufficientStock -> context.getString(R.string.error_insufficient_stock)
            is ProductsError.StockUpdateFailed -> context.getString(R.string.error_stock_update_failed)

            // Error genérico
            is ProductsError.UnknownError -> {
                if (error.message.isNullOrBlank()) {
                    context.getString(R.string.error_unknown_simple)
                } else {
                    context.getString(R.string.error_unknown, error.message)
                }
            }
        }
    }

    // Función para obtener mensaje de éxito
    fun getSuccessMessage(messageKey: String): String {
        return when (messageKey) {
            "product_created_success" -> context.getString(R.string.product_created_success)
            "product_updated_success" -> context.getString(R.string.product_updated_success)
            "product_deleted_success" -> context.getString(R.string.product_deleted_success)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.products_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.handleIntent(ProductsIntent.LoadProducts)
                    }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.action_refresh))
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
                Icon(Icons.Default.Add, stringResource(R.string.product_create))
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
                placeholder = stringResource(R.string.product_search_placeholder)
            )

            // Contenido principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    state.isLoading && state.products.isEmpty() -> {
                        LoadingIndicator(message = stringResource(R.string.product_loading))
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
                                Text(stringResource(R.string.processing))
                            }
                        }
                    }
                }
            }
        }
    }
}