package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.credit_notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.credit_notes.CreditNoteIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.credit_notes.CreditNoteUsageState
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components.QRScannerScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.credit_notes.CreditNoteUsageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditNoteUsageScreen(
    viewModel: CreditNoteUsageViewModel,
    saleTotal: Double,
    onCreditApplied: (creditNoteId: String, amountApplied: Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 🔑 SETEAMOS EL TOTAL DE LA VENTA
    LaunchedEffect(Unit) {
        viewModel.handleIntent(
            CreditNoteIntent.SetSaleTotal(saleTotal)
        )
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.handleIntent(CreditNoteIntent.ClearError)
        }
    }

    LaunchedEffect(state.navigateBack) {
        if (state.navigateBack) onNavigateBack()
    }

    if (state.showQRScanner) {
        QRScannerScreen(
            title = "Escanear Nota de Crédito",
            onQRScanned = {
                viewModel.handleIntent(CreditNoteIntent.SearchByQRCode(it))
            },
            onNavigateBack = {
                viewModel.handleIntent(CreditNoteIntent.NavigateBack)
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Aplicar Nota de Crédito") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.handleIntent(CreditNoteIntent.NavigateBack)
                    }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {
                state.isLoading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.scannedCreditNote != null ->
                    CreditNoteDetail(
                        state = state,
                        onAmountChange = {
                            viewModel.handleIntent(
                                CreditNoteIntent.AmountChanged(it)
                            )
                        },
                        onConfirm = {
                            viewModel.handleIntent(
                                CreditNoteIntent.ConfirmCreditApplication
                            )
                        }
                    )

                else ->
                    EmptyState {
                        viewModel.handleIntent(CreditNoteIntent.ShowQRScanner)
                    }
            }
        }
    }

    val creditNote = state.scannedCreditNote

    if (state.showSuccessDialog && creditNote != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = {
                    onCreditApplied(
                        creditNote.saleId,
                        state.amountToApply
                    )
                }) {
                    Text("Aceptar")
                }
            },
            title = { Text("Crédito aplicado") },
            text = { Text("La nota de crédito fue aplicada correctamente") }
        )
    }

}

/* ===================================================================================== */
/* =================================== COMPONENTS ====================================== */
/* ===================================================================================== */

@Composable
private fun CreditNoteDetail(
    state: CreditNoteUsageState,
    onAmountChange: (Double) -> Unit,
    onConfirm: () -> Unit
) {
    val creditNote = state.scannedCreditNote!!

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Nota de Crédito",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Factura: ${creditNote.invoiceNumber}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Crédito disponible: $${"%.2f".format(creditNote.creditRemaining)}"
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Monto máximo aplicable: $${"%.2f".format(state.maxApplicableAmount)}",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        item {
            OutlinedTextField(
            value = if (state.amountToApply == 0.0) "" else state.amountToApply.toString(),
            onValueChange = {
                it.toDoubleOrNull()?.let { amount ->
                    onAmountChange(amount)
                }
            },
            label = { Text("Monto a aplicar") },
            prefix = { Text("$") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )


        }

        item {
            Button(
                onClick = onConfirm,
                enabled = state.canApplyCredit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(8.dp))
                Text("Aplicar $${"%.2f".format(state.amountToApply)}")
            }
        }

        item {
            TextButton(
                onClick = {
                    onAmountChange(state.maxApplicableAmount)
                    onConfirm()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aplicar monto completo ($${"%.2f".format(state.maxApplicableAmount)})")
            }
        }
    }
}

@Composable
private fun EmptyState(onScanClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Escanee una nota de crédito para continuar")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onScanClick) {
            Text("Escanear QR")
        }
    }
}
