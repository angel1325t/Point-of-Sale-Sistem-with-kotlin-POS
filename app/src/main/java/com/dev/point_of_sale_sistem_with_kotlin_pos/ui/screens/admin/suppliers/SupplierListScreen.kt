package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.suppliers.SupplierIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.Supplier
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.SupplierError
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.components.DeleteSupplierDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.components.EmptySuppliersState
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.components.ErrorMessage
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.components.LoadingIndicator
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.components.SupplierFilterDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.components.SupplierListItem
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.components.SupplierSearchBar
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.SupplierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierListScreen(
    viewModel: SupplierViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var supplierToDelete by remember { mutableStateOf<Supplier?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Cargar proveedores al inicio
    LaunchedEffect(Unit) {
        viewModel.handleIntent(SupplierIntent.LoadSuppliers)
    }

    // Mostrar mensaje de éxito
    LaunchedEffect(state.operationSuccess, state.successMessage) {
        if (state.operationSuccess && state.successMessage != null) {
            snackbarHostState.showSnackbar(
                message = state.successMessage!!,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
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
                viewModel.handleIntent(SupplierIntent.FilterSuppliers(onlyComplete))
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
                        Icon(
                            Icons.Default.FilterList,
                            "Filtrar",
                            tint = if (state.isFiltered)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        viewModel.handleIntent(SupplierIntent.LoadSuppliers)
                        searchQuery = ""
                    }) {
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barra de búsqueda
            SupplierSearchBar(
                query = searchQuery,
                onQueryChange = { query ->
                    searchQuery = query
                    viewModel.handleIntent(SupplierIntent.SearchSupplier(query))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Lista de proveedores
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> LoadingIndicator()

                    state.error != null -> ErrorMessage(
                        error = state.error as SupplierError,
                        onRetry = { viewModel.handleIntent(SupplierIntent.LoadSuppliers) },
                        onDismiss = { viewModel.clearError() }
                    )

                    state.displaySuppliers.isEmpty() -> EmptySuppliersState(
                        isSearching = searchQuery.isNotEmpty(),
                        isFiltering = state.isFiltered
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.displaySuppliers,
                            key = { it.supplierId }
                        ) { supplier ->
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
        }
    }
}