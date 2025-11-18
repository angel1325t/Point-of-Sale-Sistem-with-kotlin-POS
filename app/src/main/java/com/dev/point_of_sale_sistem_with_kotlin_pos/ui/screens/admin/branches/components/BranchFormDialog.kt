package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.branches.Branch

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
                    label = { Text(stringResource(R.string.branch_alias_label)) },
                    placeholder = { Text(stringResource(R.string.branch_alias_example)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = aliasError,
                    supportingText = {
                        if (aliasError) {
                            Text(stringResource(R.string.alias_requiered))
                        }
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.address_branch_label)) },
                    placeholder = { Text(stringResource(R.string.address_branch_example)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.phone_branch_label)) },
                    placeholder = { Text(stringResource(R.string.phone_branch_example)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(stringResource(R.string.city_branch_label)) },
                    placeholder = { Text(stringResource(R.string.city_branch_example)) },
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
                Text(if (branch == null) stringResource(R.string.create_text) else stringResource(R.string.save) )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel) )
            }
        }
    )
}
