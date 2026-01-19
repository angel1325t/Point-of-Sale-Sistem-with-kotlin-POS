package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.refunds

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.refunds.RefundIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.QRScannerScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.refunds.RefundViewModel
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefundScreen(
    viewModel: RefundViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error.toUserMessage(),
                duration = SnackbarDuration.Long
            )
            viewModel.handleIntent(RefundIntent.ClearError)
        }
    }

    LaunchedEffect(state.navigateBack) {
        if (state.navigateBack) onNavigateBack()
    }

    if (state.showQRScanner) {
        QRScannerScreen(
            title = "Escanear Factura",
            onQRScanned = { viewModel.handleIntent(RefundIntent.SearchByQRCode(it)) },
            onNavigateBack = { viewModel.handleIntent(RefundIntent.HideQRScanner) }
        )
        return
    }

    if (state.showConfirmationDialog) {
        ConfirmDialog(
            total = state.totalRefundAmount,
            items = state.selectedItems.size,
            onDismiss = { viewModel.handleIntent(RefundIntent.ClearError) },
            onConfirm = { viewModel.handleIntent(RefundIntent.ConfirmRefund) }
        )
    }

    if (state.showSuccessDialog) {
        SuccessDialog(
            id = state.createdCreditNoteId?.toString()?.takeLast(8) ?: "",
            onDismiss = {
                viewModel.handleIntent(RefundIntent.ClearSearch)
                onNavigateBack()
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Devoluciones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.hasSelectedItems,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                BottomBar(
                    total = state.totalRefundAmount,
                    count = state.selectedItems.size,
                    onProcess = { viewModel.handleIntent(RefundIntent.ProcessRefund) },
                    onCancel = { viewModel.handleIntent(RefundIntent.ClearSearch) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { viewModel.handleIntent(RefundIntent.SearchByInvoiceNumber(searchQuery)) },
                onScan = { viewModel.handleIntent(RefundIntent.ShowQRScanner) },
                isLoading = state.isLoading
            )

            when {
                state.isLoading -> LoadingView()
                state.foundSale != null -> SaleView(
                    sale = state.foundSale!!,
                    selected = state.selectedItems,
                    onSelect = { detail, qty ->
                        viewModel.handleIntent(RefundIntent.SelectItemForRefund(detail.saleDetailId, qty))
                    },
                    onRemove = { detail ->
                        viewModel.handleIntent(RefundIntent.RemoveRefundItem(detail.saleDetailId))
                    }
                )
                else -> EmptyView()
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Número de factura") },
                leadingIcon = { Icon(Icons.Default.Receipt, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Limpiar")
                        }
                    }
                },
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSearch,
                    modifier = Modifier.weight(1f),
                    enabled = query.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Search, null, Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Buscar")
                }

                OutlinedButton(
                    onClick = onScan,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Escanear")
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SaleView(
    sale: RefundableSale,
    selected: List<RefundItem>,
    onSelect: (RefundableSaleDetail, Int) -> Unit,
    onRemove: (RefundableSaleDetail) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SaleCard(sale) }

        item {
            Text(
                text = "Productos (${sale.details.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(sale.details, key = { it.saleDetailId }) { detail ->
            val qty = selected.firstOrNull { it.saleDetailId == detail.saleDetailId }?.quantityToRefund ?: 0
            ProductCard(
                detail = detail,
                quantity = qty,
                onChange = { if (it > 0) onSelect(detail, it) else onRemove(detail) }
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SaleCard(sale: RefundableSale) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = sale.invoiceNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sale.saleDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$${String.format("%.2f", sale.total)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (sale.refundedTotal > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ya devuelto:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "$${String.format("%.2f", sale.refundedTotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    detail: RefundableSaleDetail,
    quantity: Int,
    onChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (quantity > 0) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$${String.format("%.2f", detail.unitPrice)} c/u",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (detail.refundedQuantity > 0) {
                        Text(
                            text = "${detail.refundedQuantity} devueltos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Disponible",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = detail.availableQuantity.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (detail.canBeRefunded)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            if (detail.canBeRefunded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Devolver:", style = MaterialTheme.typography.bodyMedium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (quantity > 0) onChange(quantity - 1) },
                            enabled = quantity > 0
                        ) {
                            Icon(Icons.Default.Remove, "Menos")
                        }

                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 40.dp),
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = { if (quantity < detail.availableQuantity) onChange(quantity + 1) },
                            enabled = quantity < detail.availableQuantity
                        ) {
                            Icon(Icons.Default.Add, "Más")
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Completamente devuelto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    total: Double,
    count: Int,
    onProcess: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$count ${if (count == 1) "producto" else "productos"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total (+ ITBIS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "$${String.format("%.2f", total)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = onProcess,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Procesar")
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text("Buscando factura...")
        }
    }
}

@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Busca una venta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Ingresa el número de factura o escanea el código QR",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ConfirmDialog(
    total: Double,
    items: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null) },
        title = { Text("Confirmar Devolución") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Confirmas la devolución de $items ${if (items == 1) "producto" else "productos"}?")

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", fontWeight = FontWeight.Bold)
                    Text(
                        "$${String.format("%.2f", total)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    "Se creará una nota de crédito.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun SuccessDialog(
    id: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("¡Devolución Exitosa!") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nota de crédito creada exitosamente.")

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            id,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Aceptar")
            }
        }
    )
}