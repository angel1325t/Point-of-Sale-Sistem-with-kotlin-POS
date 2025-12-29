package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun ReferenceNumberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reference by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Número de referencia")
        },
        text = {
            Column {
                Text(
                    text = "Ingresa el número de referencia del comprobante de transferencia.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = reference,
                    onValueChange = {
                        reference = it
                        showError = false
                    },
                    singleLine = true,
                    label = { Text("Referencia") },
                    isError = showError,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
                )

                if (showError) {
                    Text(
                        text = "La referencia es obligatoria",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reference.isBlank()) {
                        showError = true
                    } else {
                        onConfirm(reference.trim())
                    }
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
