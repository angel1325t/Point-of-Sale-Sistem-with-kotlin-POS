package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register

import android.R.attr.enabled
import android.R.attr.type
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegister

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenCashRegisterDialog(
    cashRegisters: List<CashRegister>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var balanceText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abrir caja registradora") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = cashRegisters.find { it.cash_register_id == selectedId }?.name ?: "Seleccionar caja",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Caja") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        cashRegisters.forEach { register ->
                            DropdownMenuItem(
                                text = { Text(register.name) },
                                onClick = {
                                    selectedId = register.cash_register_id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Saldo inicial") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = balanceText.toDoubleOrNull() ?: 0.0
                if (selectedId != null && amount >= 0) {
                    onConfirm(selectedId!!, amount)
                }
            }) { Text("Abrir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
@Composable
fun CloseCashRegisterDialog(
    currentBalance: Double, // ← puedes pasar el saldo actual calculado o initial_balance
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var finalBalanceText by remember { mutableStateOf(currentBalance.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cerrar caja registradora") },
        text = {
            Column {
                Text("Ingresa el saldo final en efectivo:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = finalBalanceText,
                    onValueChange = {
                        finalBalanceText = it
                        error = null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val balance = finalBalanceText.toDoubleOrNull()
                    if (balance == null || balance < 0) {
                        error = "Ingresa un monto válido"
                    } else {
                        onConfirm(balance)
                    }
                }
            ) {
                Text("Cerrar caja")
            }
        }
    )
}
@Composable
fun CreateCashRegisterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
        },
        title = {
            Text(text = stringResource(R.string.cash_register_create_title))
        },
        text = {
            OutlinedTextField(
                value = nameText,
                onValueChange = {
                    nameText = it
                    isError = false
                },
                label = { Text(stringResource(R.string.cash_register_name)) },
                placeholder = { Text(stringResource(R.string.cash_register_enter_name)) },
                isError = isError,
                supportingText = if (isError) {
                    { Text(stringResource(R.string.cash_register_error_empty_name)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isBlank()) {
                        isError = true
                    } else {
                        onConfirm(nameText)
                    }
                }
            ) {
                Text(stringResource(R.string.cash_register_confirm_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cash_register_cancel))
            }
        }
    )
}

@Composable
fun EditCashRegisterDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null
            )
        },
        title = {
            Text(text = stringResource(R.string.cash_register_edit_title))
        },
        text = {
            OutlinedTextField(
                value = nameText,
                onValueChange = {
                    nameText = it
                    isError = false
                },
                label = { Text(stringResource(R.string.cash_register_name)) },
                isError = isError,
                supportingText = if (isError) {
                    { Text(stringResource(R.string.cash_register_error_empty_name)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isBlank()) {
                        isError = true
                    } else {
                        onConfirm(nameText)
                    }
                }
            ) {
                Text(stringResource(R.string.cash_register_confirm_edit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cash_register_cancel))
            }
        }
    )
}

@Composable
fun DeleteCashRegisterDialog(
    cashRegisterName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = stringResource(R.string.cash_register_delete_title))
        },
        text = {
            Text(
                text = stringResource(R.string.cash_register_delete_confirmation, cashRegisterName)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.cash_register_confirm_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cash_register_cancel))
            }
        }
    )
}
