package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.users.UserIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.UserError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.UserViewModel
import kotlinx.coroutines.delay
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormScreen(
    viewModel: UserViewModel,
    userId: String? = null,
    onNavigateBack: () -> Unit
) {
    val state = viewModel.state
    val isEditMode = userId != null

    var email by rememberSaveable { mutableStateOf("") }
    var selectedRoleId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedBranchId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedRole by rememberSaveable { mutableStateOf(false) }
    var expandedBranch by rememberSaveable { mutableStateOf(false) }
    var dataLoaded by rememberSaveable { mutableStateOf(false) }

    // 🔹 Cargar roles, sucursales y usuario si aplica
    LaunchedEffect(isEditMode, userId) {
        viewModel.handleIntent(UserIntent.LoadRoles)
        viewModel.handleIntent(UserIntent.LoadBranches)

        if (isEditMode && !dataLoaded) {
            viewModel.handleIntent(UserIntent.LoadUser(userId!!))
        } else if (!isEditMode) {
            viewModel.clearSelectedUser()
        }
    }

    // 🔹 Llenar los campos al cargar usuario en modo edición
    LaunchedEffect(state.selectedUser) {
        if (isEditMode && !dataLoaded) {
            state.selectedUser?.let { user ->
                email = user.email ?: ""
                selectedRoleId = user.role_id
                selectedBranchId = user.branch_id?.toString()
                dataLoaded = true
            }
        }
    }

    // 🔹 Navegar atrás al guardar con éxito
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            delay(1500)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Editar Usuario" else "Nuevo Usuario") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 📧 Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isLoading
                )

                // 👤 Rol (ahora visible tanto en crear como editar)
                ExposedDropdownMenuBox(
                    expanded = expandedRole,
                    onExpandedChange = { expandedRole = !expandedRole }
                ) {
                    OutlinedTextField(
                        value = state.roles.find { it.role_id == selectedRoleId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRole)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !state.isLoading
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRole,
                        onDismissRequest = { expandedRole = false }
                    ) {
                        state.roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.name) },
                                onClick = {
                                    selectedRoleId = role.role_id
                                    expandedRole = false
                                }
                            )
                        }
                    }
                }

                // 🏢 Sucursal (en crear y editar)
                ExposedDropdownMenuBox(
                    expanded = expandedBranch,
                    onExpandedChange = { expandedBranch = !expandedBranch }
                ) {
                    OutlinedTextField(
                        value = state.branches.find {
                            it.branch_id?.toString() == selectedBranchId
                        }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sucursal") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBranch)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !state.isLoading
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBranch,
                        onDismissRequest = { expandedBranch = false }
                    ) {
                        state.branches.forEach { branch ->
                            DropdownMenuItem(
                                text = { Text(branch.name) },
                                onClick = {
                                    selectedBranchId = branch.branch_id?.toString()
                                    expandedBranch = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 💾 Botón Guardar
                Button(
                    onClick = {
                        if (isEditMode) {
                            viewModel.handleIntent(
                                UserIntent.UpdateUser(
                                    userId = userId!!,
                                    email = if (email.isNotBlank()) email else null,
                                    branchId = selectedBranchId?.let { UUID.fromString(it) },
                                    roleId = selectedRoleId
                                )
                            )
                        } else {
                            if (email.isNotBlank() && selectedRoleId != null && selectedBranchId != null) {
                                viewModel.handleIntent(
                                    UserIntent.CreateUser(
                                        email = email,
                                        roleId = selectedRoleId!!,
                                        username = "",
                                        phone = null,
                                        branchId = UUID.fromString(selectedBranchId!!)
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && email.isNotBlank()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isEditMode) "Actualizar" else "Crear")
                    }
                }

                // ⚠️ Error
                state.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = when (error) {
                                UserError.ConnectionError -> "Error de conexión"
                                UserError.UserNotFound -> "Usuario no encontrado"
                                UserError.EmailAlreadyExists -> "El email ya existe"
                                UserError.AuthError -> "Error al crear usuario en autenticación"
                                is UserError.Other -> error.message ?: "Error desconocido"
                            },
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // ✅ Éxito
                state.successMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ⏳ Overlay de carga
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
