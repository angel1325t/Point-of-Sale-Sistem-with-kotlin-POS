package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.cash_register.CashRegisterIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegister
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterError
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components.CashRegisterCard
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components.CreateEditCashRegisterDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components.EmptyCashRegistersContent
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components.OpenCashRegisterContent
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.cash_register.CashRegisterViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashRegisterManagementScreen(
    viewModel: CashRegisterViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var editingCashRegister by remember { mutableStateOf<CashRegister?>(null) }
    var deletingCashRegister by remember { mutableStateOf<CashRegister?>(null) }
    var showCloseDialog by remember { mutableStateOf(false) }

    val cashRegisters = state.allCashRegisters ?: emptyList()

    val availableCashRegisters: List<CashRegister> = remember(state.currentCashRegister, cashRegisters) {
        if (state.currentCashRegister == null) cashRegisters
        else cashRegisters.filter { it.cash_register_id != state.currentCashRegister!!.cash_register_id }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Carga inicial
    LaunchedEffect(Unit) {
        viewModel.processIntent(CashRegisterIntent.LoadAllCashRegisters)
        viewModel.processIntent(CashRegisterIntent.LoadCurrentCashRegister)
    }

    // ÉXITO → RECARGAR TODO
    LaunchedEffect(state.success) {
        if (state.success) {
            showCreateDialog = false
            showOpenDialog = false
            editingCashRegister = null
            deletingCashRegister = null
            showCloseDialog = false

            viewModel.processIntent(CashRegisterIntent.ClearSuccess)
            viewModel.processIntent(CashRegisterIntent.RefreshAllData)
        }
    }

    // ERRORES
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val message = when (error) {
                is CashRegisterError.NetworkError -> "Error de red"
                is CashRegisterError.ValidationError -> error.message
                is CashRegisterError.DatabaseError -> "Error en base de datos"
                is CashRegisterError.CashRegisterNotFound -> "Caja no encontrada"
                is CashRegisterError.UnauthorizedError -> "No autorizado"
                is CashRegisterError.UnknownError -> "Error desconocido"
                CashRegisterError.CashRegisterAlreadyOpen -> "Ya tienes una caja abierta"
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message)
            }
            viewModel.processIntent(CashRegisterIntent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cash_register_management)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (state.currentCashRegister == null) {
                        IconButton(
                            onClick = {
                                if (availableCashRegisters.isNotEmpty()) {
                                    showOpenDialog = true
                                } else if (!state.isLoading) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("No hay cajas disponibles")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Abrir caja")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.currentCashRegister == null) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Crear caja")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // ===== Prioridad al loader =====
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                when {
                    state.currentCashRegister != null -> {
                        OpenCashRegisterContent(
                            cashRegisterHistory = state.currentCashRegister!!,
                            onCloseCashRegister = { showCloseDialog = true }
                        )
                    }
                    cashRegisters.isEmpty() -> {
                        EmptyCashRegistersContent(Modifier.align(Alignment.Center))
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(cashRegisters, key = { it.cash_register_id }) { register ->
                                CashRegisterCard(
                                    cashRegister = register,
                                    onEdit = { editingCashRegister = register },
                                    onDelete = { deletingCashRegister = register }
                                )
                            }
                        }
                    }
                }
            }

            // ===== Bloqueo de diálogos si está cargando =====
            if (!state.isLoading) {
                if (showCreateDialog) {
                    CreateEditCashRegisterDialog(
                        title = "Nueva caja registradora",
                        initialName = "",
                        onDismiss = { showCreateDialog = false },
                        onConfirm = { name ->
                            viewModel.processIntent(CashRegisterIntent.CreateCashRegister(name))
                        }
                    )
                }

                editingCashRegister?.let { register ->
                    CreateEditCashRegisterDialog(
                        title = "Editar caja",
                        initialName = register.name,
                        onDismiss = { editingCashRegister = null },
                        onConfirm = { newName ->
                            viewModel.processIntent(CashRegisterIntent.UpdateCashRegister(register.cash_register_id, newName))
                        }
                    )
                }

                deletingCashRegister?.let { register ->
                    DeleteCashRegisterDialog(
                        cashRegisterName = register.name,
                        onDismiss = { deletingCashRegister = null },
                        onConfirm = {
                            viewModel.processIntent(CashRegisterIntent.DeleteCashRegister(register.cash_register_id))
                        }
                    )
                }

                if (showCloseDialog) {
                    CloseCashRegisterDialog(
                        currentBalance = state.currentCashRegister?.initial_balance ?: 0.0,
                        onDismiss = { showCloseDialog = false },
                        onConfirm = { finalBalance ->
                            showCloseDialog = false
                            viewModel.processIntent(CashRegisterIntent.CloseCashRegister(finalBalance))
                        }
                    )
                }

                if (showOpenDialog) {
                    OpenCashRegisterDialog(
                        cashRegisters = availableCashRegisters,
                        onDismiss = { showOpenDialog = false },
                        onConfirm = { id, balance ->
                            viewModel.processIntent(CashRegisterIntent.OpenCashRegister(id, balance))
                        }
                    )
                }
            }
        }
    }
}
