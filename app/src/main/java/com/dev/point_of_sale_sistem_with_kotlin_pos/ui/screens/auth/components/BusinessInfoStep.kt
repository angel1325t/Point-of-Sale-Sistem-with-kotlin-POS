package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModel

@Composable
fun BusinessInfoStep(viewModel: RegisterViewModel) {
    val state by viewModel.state.collectAsState()
    val canRegister = state.businessName.isNotBlank() && state.businessEmail.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Configura la información de tu negocio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        CustomTextField(
            value = state.businessName,
            onValueChange = viewModel::onBusinessNameChange,
            label = "Nombre del negocio",
            icon = Icons.Default.Store,
            enabled = !state.isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = state.businessEmail,
            onValueChange = viewModel::onBusinessEmailChange,
            label = "Email del negocio",
            icon = Icons.Default.Email,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
            enabled = !state.isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = state.businessAddress,
            onValueChange = viewModel::onBusinessAddressChange,
            label = "Dirección (opcional)",
            icon = Icons.Default.LocationOn,
            enabled = !state.isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = state.businessPhone,
            onValueChange = viewModel::onBusinessPhoneChange,
            label = "Teléfono (opcional)",
            icon = Icons.Default.Phone,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        state.error?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Error al crear la cuenta. Intenta nuevamente.", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        Button(
            onClick = {
                viewModel.sendIntent(
                    AuthIntent.Register(
                        userEmail = state.userEmail,
                        userPassword = state.userPassword,
                        userName = state.userName,
                        businessName = state.businessName,
                        businessEmail = state.businessEmail,
                        businessPhone = state.businessPhone.takeIf { it.isNotBlank() } ?: null,
                        businessAddress = state.businessAddress.takeIf { it.isNotBlank() } ?: null
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !state.isLoading && canRegister,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Cuenta", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
