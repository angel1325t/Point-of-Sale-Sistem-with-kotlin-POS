package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.products.ProductsIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.Product
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.ProductsError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.ProductsViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.components.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsListScreen(
    viewModel: ProductsViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(ProductsIntent.LoadProducts)
    }

    LaunchedEffect(state.operationSuccess, state.successMessage) {
        if (state.operationSuccess && state.successMessage != null) {
            snackbarHostState.showSnackbar(state.successMessage!!)
            viewModel.handleIntent(ProductsIntent.ClearError)
        }
    }

    // --- Diálogo de eliminar ---
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
                        "Productos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.handleIntent(ProductsIntent.LoadProducts)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
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
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
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

            // Buscador
            SearchBar(
                query = searchQuery,
                onQueryChange = { q ->
                    searchQuery = q
                    if (q.isNotBlank()) {
                        viewModel.handleIntent(ProductsIntent.SearchProducts(q))
                    } else {
                        viewModel.handleIntent(ProductsIntent.LoadProducts)
                    }
                },
                placeholder = "Buscar productos..."
            )

            // Contenido principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {

                when {
                    state.isLoading -> LoadingIndicator()

                    state.error != null -> ErrorMessage(
                        error = state.error!!,
                        onRetry = { viewModel.handleIntent(ProductsIntent.LoadProducts) },
                        onDismiss = { viewModel.handleIntent(ProductsIntent.ClearError) }
                    )

                    state.displayProducts.isEmpty() -> EmptyProductsState()

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.displayProducts, key = { it.productId }) { product ->
                                ProductListItem(
                                    product = product,
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
        }
    }
}
