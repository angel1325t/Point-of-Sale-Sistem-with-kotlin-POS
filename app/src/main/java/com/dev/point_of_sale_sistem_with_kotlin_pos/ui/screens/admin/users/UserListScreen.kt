package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.users.UserIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.UserError
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListScreen(
    navController: NavHostController,
    viewModel: UserViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val state = viewModel.state
    var showDeleteDialog by remember { mutableStateOf<UserRepository.UserModel?>(null) }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(UserIntent.LoadUsers)
    }

    // Mostrar mensajes
    LaunchedEffect(state.error, state.successMessage) {
        if (state.error != null || state.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios") },
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
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Usuario")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (state.error) {
                                UserError.ConnectionError -> "Error de conexión"
                                UserError.UserNotFound -> "Usuario no encontrado"
                                UserError.EmailAlreadyExists -> "El email ya existe"
                                UserError.AuthError -> "Error de autenticación"
                                is UserError.Other -> state.error.message ?: "Error desconocido"
                            },
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                state.users.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No hay usuarios registrados")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.users) { user ->
                            UserCard(
                                user = user,
                                onEdit = { onNavigateToEdit(user.user_id.toString()) },
                                onDelete = { showDeleteDialog = user }
                            )
                        }
                    }
                }
            }

            // Snackbar para mensajes de éxito
            state.successMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(message)
                }
            }
        }
    }

    // Diálogo de confirmación de eliminación
    showDeleteDialog?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Confirmar eliminación") },
            text = { Text("¿Está seguro que desea eliminar al usuario ${user.username}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleIntent(UserIntent.DeleteUser(user.user_id.toString()))
                        showDeleteDialog = null
                    }
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
fun UserCard(
    user: UserRepository.UserModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                Text(text = user.username, style = MaterialTheme.typography.titleMedium)
                user.phone?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Estado: ${if (user.active == true) "Activo" else "Inactivo"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.active == true)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar",tint = Color(0xFF4CAF50))
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
