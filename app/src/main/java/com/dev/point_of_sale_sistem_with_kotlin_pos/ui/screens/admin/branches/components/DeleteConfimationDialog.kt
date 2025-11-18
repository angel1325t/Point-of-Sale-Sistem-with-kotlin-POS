package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmationDialog(
    branchName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var confirmationText by remember { mutableStateOf("") }
    val isValid = confirmationText.trim().equals(branchName.trim(), ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                stringResource(R.string.delete_branch_text),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.confirm_branch_deletion),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = branchName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.branch_deletion_helper),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    label = { Text(stringResource(R.string.branch_name_label)) },
                    placeholder = { Text(branchName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = confirmationText.isNotBlank() && !isValid,
                    supportingText = {
                        if (confirmationText.isNotBlank() && !isValid) {
                            Text(stringResource(R.string.branch_name_validation))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isValid)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.outline
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.delete_branch))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}