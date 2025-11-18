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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
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
    val context = LocalContext.current

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
                is BranchError.UnauthorizedError -> context.getString(R.string.unauthorize_error)
                is BranchError.BranchNotFound -> context.getString(R.string.branch_not_found_error)
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
                            stringResource(R.string.branch_management_text),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(
                                R.string.branches_amount_mini_helper,
                                state.branches.size,
                                if (state.branches.size != 1) "es" else ""
                            ),
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
                text = { Text(stringResource(R.string.new_branch_text)) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text(stringResource(R.string.home_menu_text)) },
                    selected = false,
                    onClick = { onNavigateToTab(0) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Store, null) },
                    label = { Text(stringResource(R.string.branches_menu_text)) },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text(stringResource(R.string.settings_menu_text)) },
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
                    state.isLoading -> {
                        LoadingBranches()
                    }
                    sessionState.isLoading -> {
                        SwitchBranchesLoader()
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
            title = stringResource(R.string.create_branch_text),
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
            title = stringResource(R.string.edit_branch_text),
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
