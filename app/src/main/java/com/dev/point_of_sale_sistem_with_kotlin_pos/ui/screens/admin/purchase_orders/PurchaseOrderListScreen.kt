package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.purchase_orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.purchase_orders.PurchaseOrderIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.purchase_orders.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.PurchaseOrderViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.purchase_orders.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseOrderListScreen(
    viewModel: PurchaseOrderViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var orderToDelete by remember { mutableStateOf<PurchaseOrder?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Cargar pedidos al iniciar
    LaunchedEffect(Unit) {
        viewModel.handleIntent(PurchaseOrderIntent.LoadOrders)
    }

    // Mostrar mensajes de éxito
    LaunchedEffect(state.operationSuccess, state.successMessage) {
        if (state.operationSuccess && state.successMessage != null) {
            snackbarHostState.showSnackbar(state.successMessage!!)
            viewModel.clearError()
        }
    }

    // Diálogo de eliminar
    if (showDeleteDialog && orderToDelete != null) {
        DeleteOrderDialog(
            order = orderToDelete!!,
            onConfirm = {
                viewModel.handleIntent(PurchaseOrderIntent.DeleteOrder(orderToDelete!!.orderId))
                showDeleteDialog = false
                orderToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                orderToDelete = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pedidos de Reposición",
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
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, "Filtrar")
                    }

                    IconButton(onClick = {
                        viewModel.handleIntent(PurchaseOrderIntent.LoadOrders)
                    }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
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
                Icon(Icons.Default.Add, "Nuevo pedido")
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
            OrderSearchBar(
                query = searchQuery,
                onQueryChange = { q ->
                    searchQuery = q
                    if (q.isNotBlank()) {
                        viewModel.handleIntent(PurchaseOrderIntent.SearchOrders(q))
                    } else {
                        viewModel.handleIntent(PurchaseOrderIntent.LoadOrders)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Chips de filtro rápido
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filterStatus == OrderStatus.PENDING,
                    onClick = {
                        if (state.filterStatus == OrderStatus.PENDING) {
                            viewModel.handleIntent(PurchaseOrderIntent.FilterByStatus(null))
                        } else {
                            viewModel.handleIntent(PurchaseOrderIntent.FilterByStatus(OrderStatus.PENDING))
                        }
                    },
                    label = { Text("Pendientes (${state.pendingOrders.size})") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                FilterChip(
                    selected = state.filterStatus == OrderStatus.SENT,
                    onClick = {
                        if (state.filterStatus == OrderStatus.SENT) {
                            viewModel.handleIntent(PurchaseOrderIntent.FilterByStatus(null))
                        } else {
                            viewModel.handleIntent(PurchaseOrderIntent.FilterByStatus(OrderStatus.SENT))
                        }
                    },
                    label = { Text("Enviados (${state.sentOrders.size})") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                FilterChip(
                    selected = state.filterStatus == OrderStatus.RECEIVED,
                    onClick = {
                        if (state.filterStatus == OrderStatus.RECEIVED) {
                            viewModel.handleIntent(PurchaseOrderIntent.FilterByStatus(null))
                        } else {
                            viewModel.handleIntent(PurchaseOrderIntent.FilterByStatus(OrderStatus.RECEIVED))
                        }
                    },
                    label = { Text("Recibidos (${state.receivedOrders.size})") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        onRetry = { viewModel.handleIntent(PurchaseOrderIntent.LoadOrders) },
                        onDismiss = { viewModel.clearError() }
                    )

                    state.displayOrders.isEmpty() -> EmptyOrdersState()

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = state.displayOrders,
                                key = { it.orderId }
                            ) { order ->
                                PurchaseOrderListItem(
                                    order = order,
                                    onClick = { onNavigateToEdit(order.orderId) },
                                    onDelete = {
                                        orderToDelete = order
                                        showDeleteDialog = true
                                    },
                                    onMarkAsSent = {
                                        viewModel.handleIntent(
                                            PurchaseOrderIntent.MarkAsSent(order.orderId)
                                        )
                                    },
                                    onMarkAsReceived = {
                                        val today = LocalDate.now()
                                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        viewModel.handleIntent(
                                            PurchaseOrderIntent.MarkAsReceived(
                                                orderId = order.orderId,
                                                receivedDate = today
                                            )
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