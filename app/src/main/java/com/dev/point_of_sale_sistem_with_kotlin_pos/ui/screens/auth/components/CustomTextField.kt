package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    showPasswordStrength: Boolean = false,
    onPasswordValidationChange: ((Boolean) -> Unit)? = null
) {
    val passwordRequirements = listOf(
        "Debe tener al menos 8 caracteres" to (value.length >= 8),
        "Debe tener una letra mayúscula" to value.any { it.isUpperCase() },
        "Debe tener una letra minúscula" to value.any { it.isLowerCase() },
        "Debe tener un número" to value.any { it.isDigit() },
        "Debe tener un símbolo (!@#\$%^&*)" to value.any { it in "!@#\$%^&*()-_=+<>?/{}[]~" }
    )

    val missingRequirements = passwordRequirements.filter { !it.second }
    val isPasswordStrong = missingRequirements.isEmpty()

    if (isPassword && onPasswordValidationChange != null) {
        onPasswordValidationChange(isPasswordStrong)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = if (isPassword && onTogglePassword != null) {
                {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !showPassword)
                PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            enabled = enabled,
            isError = isError,
            supportingText = supportingText?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (showPasswordStrength && isPassword && value.isNotEmpty() && !isPasswordStrong) {
            Spacer(modifier = Modifier.height(8.dp))
            val rows = missingRequirements.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { (requirement, _) ->
                            Text(
                                text = "❌ $requirement",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (showPasswordStrength && isPassword && value.isNotEmpty() && isPasswordStrong) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Contraseña fuerte 💪",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
