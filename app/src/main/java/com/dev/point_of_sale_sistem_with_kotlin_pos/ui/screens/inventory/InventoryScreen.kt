package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.inventory.InventoryIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.inventory.InventoryViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.inventory.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var showSortDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // =====================================================
    // TOP BAR (CON TEMA)
    // =====================================================
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.inventory_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = stringResource(id = R.string.sort),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        viewModel.handleIntent(InventoryIntent.LoadInventory)
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.sort),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { padding ->

        // =====================================================
        // CONTENIDO PRINCIPAL
        // =====================================================
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // SEARCH BAR
            InventorySearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    viewModel.handleIntent(InventoryIntent.Search(it))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // LISTA / ERRORES / LOADING
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading ->
                        LoadingIndicator()

                    state.error != null ->
                        InventoryErrorView(
                            error = state.error!!,
                            onRetry = {
                                viewModel.handleIntent(InventoryIntent.LoadInventory)
                            },
                            onDismiss = {
                                viewModel.handleIntent(InventoryIntent.ClearError)
                            }
                        )

                    state.filteredItems.isEmpty() ->
                        InventoryEmptyState()

                    else ->
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.filteredItems, key = { it.id }) { item ->

                                InventoryCard(
                                    item = item,
                                    onIncrease = {
                                        viewModel.handleIntent(
                                            InventoryIntent.IncreaseStock(item.id, 1)
                                        )
                                    },
                                    onDecrease = {
                                        viewModel.handleIntent(
                                            InventoryIntent.DecreaseStock(item.id, 1)
                                        )
                                    }
                                )
                            }
                        }
                }
            }
        }
    }

    // =====================================================
    // SORT DIALOG
    // =====================================================
    if (showSortDialog) {
        InventorySortDialog(
            selected = state.sortMode,
            onSelect = {
                viewModel.handleIntent(InventoryIntent.SortBy(it))
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}
