package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.suppliers.SupplierIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.Supplier
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.SupplierError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.SupplierViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierListScreen(
    viewModel: SupplierViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var supplierToDelete by remember { mutableStateOf<Supplier?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(SupplierIntent.LoadSuppliers)
    }

    // Diálogo de eliminar
    if (showDeleteDialog && supplierToDelete != null) {
        DeleteSupplierDialog(
            supplier = supplierToDelete!!,
            onConfirm = {
                viewModel.handleIntent(SupplierIntent.DeleteSupplier(supplierToDelete!!.supplierId))
                showDeleteDialog = false
                supplierToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                supplierToDelete = null
            }
        )
    }

    // Diálogo de filtros
    if (showFilterDialog) {
        SupplierFilterDialog(
            onlyCompleteContact = state.isFiltered,
            onApplyFilter = { onlyComplete ->
                if (onlyComplete)
                    viewModel.handleIntent(SupplierIntent.SearchSupplier(""))
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Proveedores",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, "Filtrar")
                    }
                    IconButton(onClick = { viewModel.handleIntent(SupplierIntent.LoadSuppliers) }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate
            ) {
                Icon(Icons.Default.Add, "Agregar proveedor")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Barra búsqueda
            SupplierSearchBar(
                query = searchQuery,
                onQueryChange = { query ->
                    searchQuery = query
                    if (query.isNotEmpty()) {
                        viewModel.handleIntent(SupplierIntent.SearchSupplier(query))
                    } else {
                        viewModel.handleIntent(SupplierIntent.LoadSuppliers)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Lista
            Box(modifier = Modifier.fillMaxSize()) {

                when {
                    state.isLoading -> LoadingIndicator()

                    state.error != null -> ErrorMessage(
                        error = state.error as SupplierError,
                        onRetry = { viewModel.handleIntent(SupplierIntent.LoadSuppliers) },
                        onDismiss = { viewModel.clearError() }
                    )

                    state.displaySuppliers.isEmpty() -> EmptySuppliersState()

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.displaySuppliers, key = { it.supplierId }) { supplier ->
                            SupplierListItem(
                                supplier = supplier,
                                onClick = { onNavigateToEdit(supplier.supplierId) },
                                onDelete = {
                                    supplierToDelete = supplier
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Snackbar éxito
            if (state.operationSuccess && state.successMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(state.successMessage!!)
                }
            }
        }
    }
}
