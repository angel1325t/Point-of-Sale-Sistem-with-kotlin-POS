package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleFormScreen(
    navController: NavHostController,
    viewModel: RoleViewModel,
    roleId: Int? = null
) {
    val state = viewModel.state
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    val isEditMode = roleId != null

    // Cargar rol si estamos editando
    LaunchedEffect(roleId) {
        roleId?.let {
            viewModel.handleIntent(RoleIntent.LoadRole(it))
        }
    }

    // Rellenar campos cuando se carga el rol
    LaunchedEffect(state.selectedRole) {
        state.selectedRole?.let { role ->
            name = role.name
            description = role.description ?: ""
        }
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

    // Navegar atrás cuando se guarda exitosamente
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            navController.navigateUp()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditMode) "Editar Rol" else "Crear Rol")
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
            FloatingActionButton(
                onClick = {
                    // Validación
                    nameError = when {
                        name.isBlank() -> "El nombre es obligatorio"
                        name.length < 3 -> "Mínimo 3 caracteres"
                        name.length > 50 -> "Máximo 50 caracteres"
                        else -> null
                    }

                    if (nameError == null) {
                        val role = RoleRepository.RoleModel(
                            role_id = roleId,
                            name = name.trim(),
                            description = description.trim().takeIf { it.isNotEmpty() }
                        )

                        if (isEditMode) {
                            viewModel.handleIntent(RoleIntent.UpdateRole(role))
                        } else {
                            viewModel.handleIntent(RoleIntent.CreateRole(role))
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Guardar",
                    tint = MaterialTheme.colorScheme.surface
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card de información
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Información del Rol",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.inverseSurface
                        )

                        // Campo Nombre
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = null
                            },
                            label = { Text("Nombre del rol *") },
                            placeholder = { Text("Ej: Administrador, Cajero") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = nameError != null,
                            supportingText = {
                                nameError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            enabled = !state.isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )

                        // Campo Descripción
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descripción (opcional)") },
                            placeholder = { Text("Describe las funciones de este rol") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            enabled = !state.isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        // Indicador de carga
                        if (state.isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Card de ayuda
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                "Consejo",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Los roles te permiten organizar los permisos de tus usuarios. " +
                                        "Después de crear el rol, podrás asignarle permisos específicos.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}