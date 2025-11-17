package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches.Branch

/**
 * Diálogo para crear/editar sucursal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchFormDialog(
    title: String,
    branch: Branch? = null,
    onDismiss: () -> Unit,
    onSave: (alias: String, address: String, phone: String, city: String) -> Unit
) {
    var alias by remember { mutableStateOf(branch?.name?.substringAfter(" - ") ?: "") }
    var address by remember { mutableStateOf(branch?.address ?: "") }
    var phone by remember { mutableStateOf(branch?.phone ?: "") }
    var city by remember { mutableStateOf(branch?.city ?: "") }

    var aliasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = alias,
                    onValueChange = {
                        alias = it
                        aliasError = it.isBlank()
                    },
                    label = { Text("Alias *") },
                    placeholder = { Text("Ej: Centro, Norte, Sur") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = aliasError,
                    supportingText = {
                        if (aliasError) {
                            Text("El alias es obligatorio")
                        }
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección") },
                    placeholder = { Text("Calle, número, colonia") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono") },
                    placeholder = { Text("(XXX) XXX-XXXX") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Ciudad") },
                    placeholder = { Text("Nombre de la ciudad") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (alias.isNotBlank()) {
                        onSave(alias.trim(), address.trim(), phone.trim(), city.trim())
                    } else {
                        aliasError = true
                    }
                },
                enabled = alias.isNotBlank()
            ) {
                Text(if (branch == null) "Crear" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
