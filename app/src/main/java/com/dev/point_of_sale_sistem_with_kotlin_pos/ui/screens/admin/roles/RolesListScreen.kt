package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.roles.RoleIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.roles.RoleError
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.RoleViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolesListScreen(
    navController: NavHostController,
    viewModel: RoleViewModel
) {
    val state = viewModel.state
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<Int?>(null) }

    // Cargar roles al iniciar
    LaunchedEffect(Unit) {
        viewModel.handleIntent(RoleIntent.LoadRoles)
    }

    // Mostrar errores
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val message = when (error) {
                is RoleError.ConnectionError -> "Error de conexión"
                is RoleError.RoleNotFound -> "Rol no encontrado"
                is RoleError.Other -> error.customMessage ?: "Error desconocido"
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Mostrar mensajes de éxito
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Roles") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("roles/create") },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Crear rol",
                    tint = MaterialTheme.colorScheme.surface
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                state.roles.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            "No hay roles registrados",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Button(
                            onClick = { navController.navigate("roles/create") }
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Crear primer rol")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.roles,
                            key = { it.role_id ?: 0 }
                        ) { role ->
                            RoleCard(
                                role = role,
                                onEdit = {
                                    navController.navigate("roles/edit/${role.role_id}")
                                },
                                onDelete = {
                                    showDeleteDialog = role.role_id
                                },
                                onAssignPermissions = {
                                    navController.navigate("roles/${role.role_id}/permissions")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    showDeleteDialog?.let { roleId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Confirmar eliminación",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Estás seguro de eliminar este rol? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleIntent(RoleIntent.DeleteRole(roleId))
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onError, contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
@Composable
fun RoleCard(
    role: RoleRepository.RoleModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAssignPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = role.name,
                    style = MaterialTheme.typography.titleMedium
                )

                role.description?.let { desc ->
                    if (desc.isNotEmpty()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onAssignPermissions) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Gestionar permisos",
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color(0xFF4CAF50)
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
