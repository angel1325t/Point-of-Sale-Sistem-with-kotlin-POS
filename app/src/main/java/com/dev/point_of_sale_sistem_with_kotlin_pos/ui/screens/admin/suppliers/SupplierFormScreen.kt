package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.permissions.PermissionChecker
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.suppliers.SupplierIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.SupplierError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.SupplierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierFormScreen(
    viewModel: SupplierViewModel,
    supplierId: Int?,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditMode = supplierId != null

    // Verificar permisos
    val canCreate = PermissionChecker.canCreateSuppliers()
    val canUpdate = PermissionChecker.canUpdateSuppliers()
    val hasPermission = if (isEditMode) canUpdate else canCreate

    // Si no tiene permiso, navegar atrás
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            onNavigateBack()
        }
    }

    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    LaunchedEffect(supplierId) {
        if (supplierId != null && hasPermission) {
            viewModel.handleIntent(SupplierIntent.LoadSupplierById(supplierId))
        } else {
            viewModel.handleIntent(SupplierIntent.ClearSelectedSupplier)
        }
    }

    LaunchedEffect(state.selectedSupplier) {
        state.selectedSupplier?.let { supplier ->
            name = supplier.name
            contact = supplier.contact ?: ""
            phone = supplier.phone ?: ""
            email = supplier.email ?: ""
            address = supplier.address ?: ""
        }
    }

    LaunchedEffect(state.operationSuccess) {
        if (state.operationSuccess) {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val message = when (error) {
                is SupplierError.NetworkError -> error.message
                is SupplierError.ValidationError -> error.message
                is SupplierError.DatabaseError -> error.message
                is SupplierError.DuplicateNameError -> error.message
                is SupplierError.InvalidEmailError -> error.message
                is SupplierError.InvalidPhoneError -> error.message
                is SupplierError.RecordNotFound -> error.message
                is SupplierError.TimeoutError -> error.message
                is SupplierError.UnauthorizedError -> error.message
                is SupplierError.UnknownError -> error.message
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    // Si no tiene permiso, no renderizar nada
    if (!hasPermission) {
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Editar Proveedor" else "Nuevo Proveedor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    viewModel.handleIntent(SupplierIntent.ValidateName(it))
                },
                label = { Text("Nombre del proveedor *") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                isError = state.nameError != null,
                supportingText = {
                    state.nameError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isLoading
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Información de contacto (opcional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Persona de contacto") },
                leadingIcon = { Icon(Icons.Default.Badge, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isLoading
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    viewModel.handleIntent(SupplierIntent.ValidatePhone(it))
                },
                label = { Text("Teléfono") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                isError = state.phoneError != null,
                supportingText = {
                    state.phoneError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isLoading,
                placeholder = { Text("Ej: 809-555-1234") }
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.handleIntent(SupplierIntent.ValidateEmail(it))
                },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                isError = state.emailError != null,
                supportingText = {
                    state.emailError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isLoading,
                placeholder = { Text("correo@ejemplo.com") }
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Dirección") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        if (isEditMode && supplierId != null) {
                            viewModel.handleIntent(
                                SupplierIntent.UpdateSupplier(
                                    supplierId = supplierId,
                                    name = name,
                                    contact = contact.ifBlank { null },
                                    phone = phone.ifBlank { null },
                                    email = email.ifBlank { null },
                                    address = address.ifBlank { null }
                                )
                            )
                        } else {
                            viewModel.handleIntent(
                                SupplierIntent.CreateSupplier(
                                    name = name,
                                    contact = contact.ifBlank { null },
                                    phone = phone.ifBlank { null },
                                    email = email.ifBlank { null },
                                    address = address.ifBlank { null }
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() &&
                            state.nameError == null &&
                            state.phoneError == null &&
                            state.emailError == null &&
                            !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isEditMode) "Actualizar" else "Crear")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "* Campos obligatorios. La información de contacto es opcional pero recomendada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}