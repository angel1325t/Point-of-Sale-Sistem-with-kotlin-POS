//package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.credit_notes
//
//import android.os.Build
//import androidx.annotation.RequiresApi
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.credit_notes.CreditNoteIntent
//import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditApplication
//import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditNote
//import com.dev.point_of_sale_sistem_with_kotlin_pos.models.refunds.toUserMessage
//import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.QRScannerScreen
//import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.credit_notes.CreditNoteViewModel
//import java.time.format.DateTimeFormatter
//import java.util.UUID
//
//@RequiresApi(Build.VERSION_CODES.O)
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CreditNoteScreen(
//    viewModel: CreditNoteViewModel,
//    saleTotal: Double,
//    onCreditApplied: (creditNoteId: UUID, amountApplied: Double) -> Unit,
//    onNavigateBack: () -> Unit
//) {
//    val state by viewModel.state.collectAsStateWithLifecycle()
//    val snackbarHostState = remember { SnackbarHostState() }
//
//    var searchQuery by remember { mutableStateOf("") }
//
//    // Manejo de errores
//    LaunchedEffect(state.error) {
//        state.error?.let { error ->
//            snackbarHostState.showSnackbar(error.toUserMessage())
//            viewModel.handleIntent(CreditNoteIntent.ClearError)
//        }
//    }
//
//    // Aplicación exitosa
//    LaunchedEffect(state.showSuccessDialog) {
//        if (state.showSuccessDialog && state.appliedCreditNoteId != null) {
//            val application = state.creditApplication!!
//            onCreditApplied(state.appliedCreditNoteId!!, application.amountToApply)
//            // Limpiar y volver
//            viewModel.handleIntent(CreditNoteIntent.ClearSearch)
//            onNavigateBack()
//        }
//    }
//
//    // QR Scanner
//    if (state.showQRScanner) {
//        QRScannerScreen(
//            title = "Escanear nota de crédito",
//            onQRScanned = { qrContent ->
//                viewModel.handleIntent(CreditNoteIntent.SearchByQRCode(qrContent))
//            },
//            onNavigateBack = {
//                viewModel.handleIntent(CreditNoteIntent.HideQRScanner)
//            }
//        )
//        return
//    }
//
//    // Diálogo de confirmación
//    if (state.showConfirmationDialog && state.creditApplication != null) {
//        ConfirmCreditApplicationDialog(
//            application = state.creditApplication!!,
//            saleTotal = saleTotal,
//            onDismiss = {
//                viewModel.handleIntent(CreditNoteIntent.ClearError)
//                viewModel.handleIntent(CreditNoteIntent.ClearSearch)
//            },
//            onConfirm = {
//                viewModel.handleIntent(CreditNoteIntent.ConfirmCreditApplication)
//            }
//        )
//    }
//
//    Scaffold(
//        snackbarHost = { SnackbarHost(snackbarHostState) },
//        topBar = {
//            TopAppBar(
//                title = { Text("Aplicar Nota de Crédito") },
//                navigationIcon = {
//                    IconButton(onClick = onNavigateBack) {
//                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp)
//        ) {
//            // Total de la compra actual
//            SaleTotalCard(saleTotal)
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Búsqueda de nota de crédito
//            SearchCreditNoteSection(
//                searchQuery = searchQuery,
//                onSearchQueryChange = { searchQuery = it },
//                onSearchClick = {
//                    viewModel.handleIntent(CreditNoteIntent.SearchByInvoiceNumber(searchQuery))
//                },
//                onScanClick = {
//                    viewModel.handleIntent(CreditNoteIntent.ShowQRScanner)
//                },
//                isLoading = state.isLoading
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Contenido
//            when {
//                state.isLoading -> {
//                    Box(
//                        modifier = Modifier.fillMaxSize(),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        CircularProgressIndicator()
//                    }
//                }
//                state.foundCreditNote != null -> {
//                    CreditNoteDetailsSection(
//                        creditNote = state.foundCreditNote!!,
//                        saleTotal = saleTotal,
//                        onApplyCredit = {
//                            viewModel.handleIntent(
//                                CreditNoteIntent.ApplyCreditToSale(
//                                    creditNoteId = it.saleId,
//                                    saleTotal = saleTotal
//                                )
//                            )
//                        }
//                    )
//                }
//                else -> {
//                    EmptySearchState()
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun SaleTotalCard(saleTotal: Double) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.primaryContainer
//        )
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(20.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column {
//                Text(
//                    text = "Total de la compra",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold
//                )
//                Text(
//                    text = "Monto a pagar",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
//                )
//            }
//            Text(
//                text = "$${String.format("%.2f", saleTotal)}",
//                style = MaterialTheme.typography.headlineMedium,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.primary
//            )
//        }
//    }
//}
//
//@Composable
//private fun SearchCreditNoteSection(
//    searchQuery: String,
//    onSearchQueryChange: (String) -> Unit,
//    onSearchClick: () -> Unit,
//    onScanClick: () -> Unit,
//    isLoading: Boolean
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceVariant
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            Text(
//                text = "Buscar nota de crédito",
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold
//            )
//
//            OutlinedTextField(
//                value = searchQuery,
//                onValueChange = onSearchQueryChange,
//                modifier = Modifier.fillMaxWidth(),
//                placeholder = { Text("Número de factura") },
//                trailingIcon = {
//                    if (searchQuery.isNotEmpty()) {
//                        IconButton(onClick = { onSearchQueryChange("") }) {
//                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
//                        }
//                    }
//                },
//                singleLine = true
//            )
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Button(
//                    onClick = onSearchClick,
//                    modifier = Modifier.weight(1f),
//                    enabled = searchQuery.isNotBlank() && !isLoading
//                ) {
//                    Icon(Icons.Default.Search, contentDescription = null)
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text("Buscar")
//                }
//
//                OutlinedButton(
//                    onClick = onScanClick,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text("Escanear")
//                }
//            }
//        }
//    }
//}
//
//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//private fun CreditNoteDetailsSection(
//    creditNote: CreditNote,
//    saleTotal: Double,
//    onApplyCredit: (CreditNote) -> Unit
//) {
//    Column(
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        // Información de la nota de crédito
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors(
//                containerColor = MaterialTheme.colorScheme.secondaryContainer
//            )
//        ) {
//            Column(
//                modifier = Modifier.padding(20.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(
//                        Icons.Default.Receipt,
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier.size(32.dp)
//                    )
//                    Text(
//                        text = "Nota de Crédito Encontrada",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//
//                Divider()
//
//                InfoRow("Factura:", creditNote.invoiceNumber)
//                InfoRow(
//                    "Fecha:",
//                    creditNote.createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
//                )
//                InfoRow("Total original:", "$${String.format("%.2f", creditNote.originalTotal)}")
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Surface(
//                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
//                    shape = RoundedCornerShape(8.dp)
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(12.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = "Saldo disponible:",
//                            style = MaterialTheme.typography.titleSmall,
//                            fontWeight = FontWeight.Bold
//                        )
//                        Text(
//                            text = "$${String.format("%.2f", creditNote.creditRemaining)}",
//                            style = MaterialTheme.typography.headlineSmall,
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
//                }
//            }
//        }
//
//        // Cálculo de aplicación
//        val amountToApply = minOf(creditNote.creditRemaining, saleTotal)
//        val remainingPayment = maxOf(0.0, saleTotal - amountToApply)
//
//        Card(
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Column(
//                modifier = Modifier.padding(20.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Text(
//                    text = "Resumen de aplicación",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold
//                )
//
//                Divider()
//
//                InfoRow("Total compra:", "$${String.format("%.2f", saleTotal)}")
//                InfoRow(
//                    "Crédito a aplicar:",
//                    "$${String.format("%.2f", amountToApply)}",
//                    valueColor = MaterialTheme.colorScheme.primary
//                )
//
//                if (remainingPayment > 0) {
//                    InfoRow(
//                        "Saldo restante:",
//                        "$${String.format("%.2f", remainingPayment)}",
//                        valueColor = MaterialTheme.colorScheme.error
//                    )
//                } else {
//                    Surface(
//                        color = MaterialTheme.colorScheme.tertiaryContainer,
//                        shape = RoundedCornerShape(8.dp)
//                    ) {
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(12.dp),
//                            horizontalArrangement = Arrangement.Center,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Icon(
//                                Icons.Default.CheckCircle,
//                                contentDescription = null,
//                                tint = MaterialTheme.colorScheme.tertiary
//                            )
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text(
//                                text = "Compra cubierta completamente",
//                                fontWeight = FontWeight.Bold,
//                                color = MaterialTheme.colorScheme.tertiary
//                            )
//                        }
//                    }
//                }
//            }
//        }
//
//        // Botón para aplicar
//        Button(
//            onClick = { onApplyCredit(creditNote) },
//            modifier = Modifier.fillMaxWidth(),
//            shape = RoundedCornerShape(12.dp)
//        ) {
//            Icon(Icons.Default.Check, contentDescription = null)
//            Spacer(Modifier.width(8.dp))
//            Text(
//                text = "Aplicar Crédito",
//                style = MaterialTheme.typography.titleMedium
//            )
//        }
//    }
//}
//
//@Composable
//private fun InfoRow(
//    label: String,
//    value: String,
//    valueColor: Color = MaterialTheme.colorScheme.onSurface
//) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//        Text(
//            text = value,
//            style = MaterialTheme.typography.bodyMedium,
//            fontWeight = FontWeight.Bold,
//            color = valueColor
//        )
//    }
//}
//
//@Composable
//private fun EmptySearchState() {
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        Icon(
//            imageVector = Icons.Default.Receipt,
//            contentDescription = null,
//            modifier = Modifier.size(80.dp),
//            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//        Text(
//            text = "Busca una nota de crédito",
//            style = MaterialTheme.typography.titleMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//        Text(
//            text = "Escanea el QR o ingresa el número de factura",
//            style = MaterialTheme.typography.bodySmall,
//            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
//            textAlign = TextAlign.Center
//        )
//    }
//}
//
//@Composable
//private fun ConfirmCreditApplicationDialog(
//    application: CreditApplication,
//    saleTotal: Double,
//    onDismiss: () -> Unit,
//    onConfirm: () -> Unit
//) {
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        icon = {
//            Icon(
//                Icons.Default.Info,
//                contentDescription = null,
//                tint = MaterialTheme.colorScheme.primary
//            )
//        },
//        title = { Text("Confirmar Aplicación") },
//        text = {
//            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                Text("Se aplicarán $${String.format("%.2f", application.amountToApply)} de crédito.")
//
//                if (application.needsAdditionalPayment) {
//                    Divider()
//                    Text(
//                        "Saldo restante: $${String.format("%.2f", application.additionalPaymentRequired)}",
//                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.error
//                    )
//                    Text(
//                        "Deberás pagar el saldo con otro método.",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                } else {
//                    Text(
//                        "El crédito cubre el total. ✓",
//                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.tertiary
//                    )
//                }
//            }
//        },
//        confirmButton = {
//            Button(onClick = onConfirm) {
//                Text("Aplicar")
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("Cancelar")
//            }
//        }
//    )
//}