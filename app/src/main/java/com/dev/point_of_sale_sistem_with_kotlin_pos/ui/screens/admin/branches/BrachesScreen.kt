package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.braches.BranchIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches.Branch
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches.BranchError
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.components.BranchFormDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.components.DeleteConfirmationDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.BranchViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchesScreen(
    viewModel: BranchViewModel,
    sessionViewModel: AuthSessionViewModel, // ✅ Agregado
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val sessionState by sessionViewModel.state.collectAsState() // ✅ Observar sesión

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChangeBranchDialog by remember { mutableStateOf(false) } // ✅ Nuevo
    var selectedBranch by remember { mutableStateOf<Branch?>(null) }

    // Manejar mensajes
    LaunchedEffect(state.error, state.successMessage) {
        if (state.error != null || state.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.handleIntent(BranchIntent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gestión de Sucursales")
                        // ✅ Mostrar sucursal actual
                        sessionState.branchId?.let { branchId ->
                            val currentBranch = state.branches.find { it.branchId == branchId }
                            currentBranch?.let {
                                Text(
                                    text = "Sucursal actual: ${it.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        } ?: Text(
                            text = "Sin sucursal seleccionada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    // ✅ Botón para cambiar sucursal
                    IconButton(onClick = { showChangeBranchDialog = true }) {
                        Icon(Icons.Default.SwapHoriz, "Cambiar Sucursal")
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Crear Sucursal")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ✅ Mostrar loading cuando está cambiando de sucursal
            if (state.isLoading || sessionState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp),
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = if (sessionState.isLoading) "Cambiando de sucursal..." else "Cargando...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.branches) { branch ->
                        BranchCard(
                            branch = branch,
                            isCurrentBranch = branch.branchId == sessionState.branchId, // ✅ Marcar actual
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
                }
            }

            // Mensajes de error y éxito
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                state.error?.let { error ->
                    MessageCard(
                        message = when (error) {
                            is BranchError.NetworkError -> error.message
                            is BranchError.ValidationError -> error.message
                            is BranchError.DatabaseError -> error.message
                            is BranchError.UnauthorizedError -> "No autorizado"
                            is BranchError.BranchNotFound -> "Sucursal no encontrada"
                            is BranchError.UnknownError -> error.message
                        },
                        isError = true
                    )
                }

                state.successMessage?.let { message ->
                    MessageCard(message = message, isError = false)
                }
            }
        }
    }

    // ✅ NUEVO: Diálogo para cambiar sucursal
    if (showChangeBranchDialog) {
        ChangeBranchDialog(
            branches = state.branches.filter { it.active }, // Solo sucursales activas
            currentBranchId = sessionState.branchId,
            onDismiss = { showChangeBranchDialog = false },
            onSelect = { branchId ->
                sessionViewModel.sendIntent(AuthIntent.ChangeBranch(branchId))
                showChangeBranchDialog = false
            }
        )
    }

    // Diálogos existentes
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
                viewModel.handleIntent(BranchIntent.DeleteBranch(selectedBranch!!.branchId))
                showDeleteDialog = false
                selectedBranch = null
            }
        )
    }
}

// ✅ NUEVO: Diálogo para seleccionar sucursal
@Composable
fun ChangeBranchDialog(
    branches: List<Branch>,
    currentBranchId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Sucursal") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(branches) { branch ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(branch.branchId) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (branch.branchId == currentBranchId)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = branch.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                branch.city?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (branch.branchId == currentBranchId) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Actual",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun BranchCard(
    branch: Branch,
    isCurrentBranch: Boolean = false, // ✅ Nueva propiedad
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentBranch)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = branch.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        // ✅ Badge de sucursal actual
                        if (isCurrentBranch) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "ACTUAL",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    branch.address?.let {
                        InfoRow(icon = Icons.Default.LocationOn, text = it)
                    }
                    branch.phone?.let {
                        InfoRow(icon = Icons.Default.Phone, text = it)
                    }
                    branch.city?.let {
                        InfoRow(icon = Icons.Default.Place, text = it)
                    }
                }

                Switch(
                    checked = branch.active,
                    onCheckedChange = { onToggleStatus() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MessageCard(message: String, isError: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message)
        }
    }
}