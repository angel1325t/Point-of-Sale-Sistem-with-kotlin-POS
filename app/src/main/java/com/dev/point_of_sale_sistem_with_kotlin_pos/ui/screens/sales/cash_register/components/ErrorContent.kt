// ui/screens/sales/cash_register/components/ErrorContent.kt
package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.sales.cash_register.CashRegisterError

@Composable
fun ErrorContent(
    error: CashRegisterError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val errorMessage = when (error) {
        is CashRegisterError.NetworkError -> error.message
        is CashRegisterError.ValidationError -> error.message
        is CashRegisterError.DatabaseError -> error.message
        is CashRegisterError.UnauthorizedError -> context.getString(R.string.unauthorize_error)
        is CashRegisterError.CashRegisterNotFound -> context.getString(R.string.cash_register_not_found_error)
        is CashRegisterError.UnknownError -> error.message
        CashRegisterError.CashRegisterAlreadyOpen -> stringResource(R.string.cash_register_already_open)
    }

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorMessage, // Aquí usamos el String extraído
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
