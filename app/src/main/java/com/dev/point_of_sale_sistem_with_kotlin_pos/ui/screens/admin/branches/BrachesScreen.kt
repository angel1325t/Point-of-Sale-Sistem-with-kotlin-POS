package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches

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
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.braches.BranchIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches.Branch
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches.BranchError
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.components.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.BranchViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchesScreen(
    viewModel: BranchViewModel,
    sessionViewModel: AuthSessionViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val sessionState by sessionViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangeBranchDialog by remember { mutableStateOf(false) }
    var selectedBranch by remember { mutableStateOf<Branch?>(null) }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(BranchIntent.LoadBranches)
    }

    // Error handler
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val message = when (error) {
                is BranchError.NetworkError -> error.message
                is BranchError.ValidationError -> error.message
                is BranchError.DatabaseError -> error.message
                is BranchError.UnauthorizedError -> "No autorizado"
                is BranchError.BranchNotFound -> "Sucursal no encontrada"
                is BranchError.UnknownError -> error.message
            }
            snackbarHostState.showSnackbar(message)
            viewModel.handleIntent(BranchIntent.ClearError)
        }
    }

    // Success handler
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.handleIntent(BranchIntent.ClearError)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Gestión de Sucursales",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${state.branches.size} sucursal${if (state.branches.size != 1) "es" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nueva Sucursal") }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Inicio") },
                    selected = false,
                    onClick = { onNavigateToTab(0) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Store, null) },
                    label = { Text("Sucursales") },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Ajustes") },
                    selected = false,
                    onClick = { onNavigateToTab(2) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Column(modifier = Modifier.fillMaxSize()) {

                // Barra de información sucursal actual
                sessionState.branchId?.let { branchId ->
                    state.branches.find { it.branchId == branchId }?.let { currentBranch ->
                            CurrentBranchCard(
                            branch = currentBranch,
                            canChange = state.branches.any { it.active && it.branchId != branchId },
                            onChange = { showChangeBranchDialog = true }
                        )
                    }
                }

                when {
                    state.isLoading || sessionState.isLoading -> {
                        LoadingBranches()
                    }

                    state.branches.isEmpty() -> {
                        EmptyBranchesScreen()
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.branches) { branch ->
                                BranchCard(
                                    branch = branch,
                                    isCurrentBranch = branch.branchId == sessionState.branchId,
                                    onEdit = {
                                        selectedBranch = branch
                                        showEditDialog = true
                                    },
                                    onDelete = {
                                        selectedBranch = branch
                                        showDeleteDialog = true
                                    },
                                    onToggleStatus = {
                                        viewModel.handleIntent(
                                            BranchIntent.ToggleBranchStatus(
                                                branch.branchId,
                                                !branch.active
                                            )
                                        )
                                    }
                                )
                            }

                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    // ----- DIALOGS -----

    if (showChangeBranchDialog) {
        ChangeBranchDialog(
            branches = state.branches.filter { it.active },
            currentBranchId = sessionState.branchId,
            onDismiss = { showChangeBranchDialog = false },
            onSelect = { branchId ->
                sessionViewModel.sendIntent(AuthIntent.ChangeBranch(branchId))
                showChangeBranchDialog = false
            }
        )
    }

    if (showCreateDialog) {
        BranchFormDialog(
            title = "Crear Sucursal",
            onDismiss = { showCreateDialog = false },
            onSave = { alias, address, phone, city ->
                viewModel.handleIntent(
                    BranchIntent.CreateBranch(alias, address, phone, city)
                )
                showCreateDialog = false
            }
        )
    }

    if (showEditDialog && selectedBranch != null) {
        BranchFormDialog(
            title = "Editar Sucursal",
            branch = selectedBranch,
            onDismiss = {
                showEditDialog = false
                selectedBranch = null
            },
            onSave = { alias, address, phone, city ->
                viewModel.handleIntent(
                    BranchIntent.UpdateBranch(
                        selectedBranch!!.branchId,
                        alias,
                        address,
                        phone,
                        city
                    )
                )
                showEditDialog = false
                selectedBranch = null
            }
        )
    }

    if (showDeleteDialog && selectedBranch != null) {
        DeleteConfirmationDialog(
            branchName = selectedBranch!!.name,
            onDismiss = {
                showDeleteDialog = false
                selectedBranch = null
            },
            onConfirm = {
                viewModel.handleIntent(
                    BranchIntent.DeleteBranch(selectedBranch!!.branchId)
                )
                showDeleteDialog = false
                selectedBranch = null
            }
        )
    }
}
