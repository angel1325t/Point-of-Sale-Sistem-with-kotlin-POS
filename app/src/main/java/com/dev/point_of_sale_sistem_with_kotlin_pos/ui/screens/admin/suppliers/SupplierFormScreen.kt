package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.SupplierViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.suppliers.SupplierIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.suppliers.SupplierError
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.components.ErrorMessage


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierFormScreen(
    viewModel: SupplierViewModel,
    supplierId: Int?,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isEditMode = supplierId != null

    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Cargar datos
    LaunchedEffect(supplierId) {
        if (supplierId != null)
            viewModel.handleIntent(SupplierIntent.LoadSupplierById(supplierId))
    }

    // Actualizar UI
    LaunchedEffect(state.selectedSupplier) {
        state.selectedSupplier?.let {
            name = it.name
            contact = it.contact ?: ""
            phone = it.phone ?: ""
            email = it.email ?: ""
            address = it.address ?: ""
        }
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
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // NOMBRE
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    viewModel.validateSupplierName(it)
                },
                label = { Text("Nombre *") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                isError = state.nameError != null,
                supportingText = {
                    state.nameError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // CONTACTO
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Persona de contacto") },
                leadingIcon = { Icon(Icons.Default.Badge, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // PHONE
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    viewModel.validatePhone(it)
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
                singleLine = true
            )

            // EMAIL
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.validateEmail(it)
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
                singleLine = true
            )

            // ADDRESS
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Dirección") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // BOTONES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
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

                            // 🔥 Retrocede manualmente
                            onNavigateBack()

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

                            // 🔥 Retrocede manualmente
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && state.nameError == null
                ) {
                    if (state.isLoading)
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    else
                        Text(if (isEditMode) "Actualizar" else "Crear")
                }

            }

            // ERROR UI
            state.error?.let { error ->
                ErrorMessage(
                    error = error,
                    onRetry = {},
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }
}
