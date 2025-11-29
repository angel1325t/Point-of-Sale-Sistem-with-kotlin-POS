package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.sales.cash_register.CashRegisterIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterHistory
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components.ErrorContent
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components.formatDate
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.cash_register.CashRegisterViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashRegisterHistoryScreen(
    viewModel: CashRegisterViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        Log.d("HistoryScreen", "Nuevo estado recibido: isLoadingMore=${state.isLoadingMore}, allPagesLoaded=${state.allPagesLoaded}")
    }

    val history = state.cashRegisterHistory

    LaunchedEffect(Unit) {
        if (!viewModel.state.value.historyLoaded) {
            Log.d("HistoryScreen", "Cargando historial por primera vez...")
            viewModel.processIntent(CashRegisterIntent.LoadCashRegisterHistory)
        }
    }

    val listState = rememberLazyListState()

    val canLoadMore by remember(
        state.isLoadingMore,
        state.allPagesLoaded,
        history
    ) {
        derivedStateOf {
            !state.isLoadingMore && !state.allPagesLoaded && history.isNotEmpty()
        }
    }

    LaunchedEffect(listState, canLoadMore) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }
            .distinctUntilChanged()
            .collect { lastVisible ->
                val total = listState.layoutInfo.totalItemsCount

                if (lastVisible == total - 1 && total > 0 && canLoadMore) {
                    Log.d("HistoryScreen", "Trigger loadMore() - llegó al final del scroll")
                    viewModel.processIntent(
                        CashRegisterIntent.LoadMoreHistory(
                            nextOffset = history.size
                        )
                    )
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cash_register_history)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                state.error != null -> ErrorContent(
                    error = state.error!!,
                    onRetry = { viewModel.processIntent(CashRegisterIntent.LoadCashRegisterHistory) },
                    modifier = Modifier.align(Alignment.Center)
                )

                history.isEmpty() -> EmptyHistoryContent(
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history) { cashRegisterHistory ->
                        CashRegisterHistoryCard(cashRegisterHistory = cashRegisterHistory)
                    }

                    item {
                        if (state.isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashRegisterHistoryCard(
    cashRegisterHistory: CashRegisterHistory
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (cashRegisterHistory.closing_date != null)
                        stringResource(R.string.cash_register_status_closed)
                    else
                        stringResource(R.string.cash_register_status_open),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = if (cashRegisterHistory.closing_date != null)
                        Icons.Default.Lock
                    else
                        Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (cashRegisterHistory.closing_date != null)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            // Fecha de apertura
            InfoRowHistory(
                icon = Icons.Default.Schedule,
                label = stringResource(R.string.cash_register_opening_date),
                value = formatDate(cashRegisterHistory.opening_date ?: "")
            )

            // Fecha de cierre
            if (cashRegisterHistory.closing_date != null) {
                InfoRowHistory(
                    icon = Icons.Default.EventAvailable,
                    label = stringResource(R.string.cash_register_closing_date),
                    value = formatDate(cashRegisterHistory.closing_date)
                )
            }

            HorizontalDivider()

            // Saldo inicial
            InfoRowHistory(
                icon = Icons.Default.AttachMoney,
                label = stringResource(R.string.cash_register_initial_balance),
                value = stringResource(R.string.currency_format, cashRegisterHistory.initial_balance),
                valueColor = MaterialTheme.colorScheme.primary
            )

            // Saldo final
            if (cashRegisterHistory.final_balance != null) {
                InfoRowHistory(
                    icon = Icons.Default.AccountBalance,
                    label = stringResource(R.string.cash_register_final_balance),
                    value = stringResource(R.string.currency_format, cashRegisterHistory.final_balance),
                    valueColor = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun InfoRowHistory(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun EmptyHistoryContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.HistoryToggleOff,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.cash_register_history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}