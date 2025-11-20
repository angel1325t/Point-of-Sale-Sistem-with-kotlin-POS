//// CashRegisterMainScreen.kt
//package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.dev.point_of_sale_sistem_with_kotlin_pos.R
//import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.cash_register.CashRegisterIntent
//import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegister
//import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterError
//import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components.CreateEditCashRegisterDialog
//import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components.ErrorContent
//import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components.NoCashRegisterContent
//import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components.OpenCashRegisterContent
//import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.cash_register.CashRegisterViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CashRegisterMainScreen(
//    viewModel: CashRegisterViewModel,
//    onNavigateToManagement: () -> Unit
//) {
//    val state by viewModel.state.collectAsState()
//
//    var showOpenDialog by remember { mutableStateOf(false) }
//    var selectedCashRegister by remember { mutableStateOf<CashRegister?>(null) }
//    var initialBalanceText by remember { mutableStateOf("") }
//
//    LaunchedEffect(Unit) {
//        viewModel.processIntent(CashRegisterIntent.LoadCurrentCashRegister)
//        viewModel.processIntent(CashRegisterIntent.LoadAllCashRegisters)
//    }
//
//    LaunchedEffect(state.success) {
//        if (state.success) {
//            showOpenDialog = false
//            selectedCashRegister = null
//            initialBalanceText = ""
//            viewModel.processIntent(CashRegisterIntent.ClearSuccess)
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            CenterAlignedTopAppBar(
//                title = { Text(stringResource(R.string.cash_register)) },
//                actions = {
//                    IconButton(onClick = onNavigateToManagement) {
//                        Icon(Icons.Default.Settings, contentDescription = "Gestión")
//                    }
//                    if (state.currentCashRegister != null) {
//                        IconButton(onClick = { viewModel.processIntent(CashRegisterIntent.CloseCashRegister) }) {
//                            Icon(Icons.Default.Lock, contentDescription = "Cerrar caja", tint = MaterialTheme.colorScheme.error)
//                        }
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
//            when {
//                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
//                state.error != null -> ErrorContent(error = state.error!!, onRetry = {
//                    viewModel.processIntent(CashRegisterIntent.LoadCurrentCashRegister)
//                }, modifier = Modifier.align(Alignment.Center))
//
//                state.currentCashRegister != null -> OpenCashRegisterContent(
//                    cashRegisterHistory = state.currentCashRegister!!,
//                    onCloseCashRegister = { viewModel.processIntent(CashRegisterIntent.CloseCashRegister) }
//                )
//
//                else -> NoCashRegisterContent { showOpenDialog = true }
//            }
//        }
//    }
//
//    if (showOpenDialog) {
//        CreateEditCashRegisterDialog(
//            title = "Abrir caja registradora",
//            initialName = selectedCashRegister?.name ?: "",
//            onDismiss = {
//                showOpenDialog = false
//                selectedCashRegister = null
//                initialBalanceText = ""
//            },
//            onConfirm = { name ->
//                val balance = initialBalanceText.toDoubleOrNull() ?: 0.0
//                selectedCashRegister?.let {
//                    viewModel.processIntent(CashRegisterIntent.OpenCashRegister(it.cash_register_id, balance))
//                }
//            }
//        )
//    }
//
//}