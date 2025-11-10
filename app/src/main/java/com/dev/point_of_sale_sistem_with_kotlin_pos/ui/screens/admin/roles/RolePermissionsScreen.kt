package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles

import androidx.compose.animation.AnimatedVisibility
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
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.RoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePermissionsScreen(
    navController: NavHostController,
    viewModel: RoleViewModel,
    roleId: Int
) {
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPermissions by remember(state.assignedPermissionIds) {
        mutableStateOf(state.assignedPermissionIds)
    }
    var hasChanges by remember { mutableStateOf(false) }

    // Cargar datos iniciales
    LaunchedEffect(roleId) {
        viewModel.clearMessages() // Limpia mensajes viejos
        viewModel.handleIntent(RoleIntent.LoadRole(roleId))
        viewModel.handleIntent(RoleIntent.LoadPermissions)
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

    // Navegar atrás cuando se guardan los permisos
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            // Limpiar el mensaje para que no se repita
            viewModel.clearMessages()
            navController.navigateUp()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Permisos")
                        state.selectedRole?.let { role ->
                            Text(
                                text = role.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
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
            AnimatedVisibility(visible = hasChanges) {
                FloatingActionButton(
                    onClick = {
                        viewModel.handleIntent(
                            RoleIntent.AssignPermissions(
                                roleId = roleId,
                                permissions = selectedPermissions.toList()
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Guardar permisos",
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
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
                state.permissions.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            "No hay permisos disponibles",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Contacta al administrador del sistema",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        // Contador de permisos seleccionados
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Permisos seleccionados:",
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${selectedPermissions.size} de ${state.permissions.size}",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Lista de permisos
                        items(
                            items = state.permissions,
                            key = { it.permission_id }
                        ) { permission ->
                            PermissionCard(
                                permission = permission,
                                isSelected = selectedPermissions.contains(permission.permission_id),
                                onToggle = { selected ->
                                    selectedPermissions = if (selected) {
                                        selectedPermissions + permission.permission_id
                                    } else {
                                        selectedPermissions - permission.permission_id
                                    }
                                    hasChanges = true
                                }
                            )
                        }

                        // Espaciado al final para el FAB
                        item {
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    permission: com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository.PermissionModel,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Gray
                    },
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = permission.name,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.inverseSurface
                        }
                    )
                    Text(
                        text = formatPermissionName(permission.name),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Switch(
                checked = isSelected,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.secondary
                )
            )
        }
    }
}

// Helper para formatear nombres de permisos
private fun formatPermissionName(name: String): String {
    return name
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
}