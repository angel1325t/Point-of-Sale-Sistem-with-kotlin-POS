package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dev.point_of_sale_sistem_with_kotlin_pos.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteCashRegisterDialog(
    cashRegisterName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cash_register_delete_title)) },
        text = {
            Text(stringResource(R.string.cash_register_dialog_delete_message, cashRegisterName))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
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