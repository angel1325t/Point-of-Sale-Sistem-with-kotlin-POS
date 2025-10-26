package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.components

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModel
@Composable
fun BusinessInfoStep(viewModel: RegisterViewModel) {
    val state by viewModel.state.collectAsState()

    // Usamos rememberSaveable para mantener datos al rotar pantalla
    var businessName by rememberSaveable { mutableStateOf(state.businessName) }
    var businessEmail by rememberSaveable { mutableStateOf(state.businessEmail) }
    var businessAddress by rememberSaveable { mutableStateOf(state.businessAddress) }
    var businessPhone by rememberSaveable { mutableStateOf(state.businessPhone) }

    // Sincronizar cambios locales con el ViewModel
    LaunchedEffect(businessName) { viewModel.onBusinessNameChange(businessName) }
    LaunchedEffect(businessEmail) { viewModel.onBusinessEmailChange(businessEmail) }
    LaunchedEffect(businessAddress) { viewModel.onBusinessAddressChange(businessAddress) }
    LaunchedEffect(businessPhone) { viewModel.onBusinessPhoneChange(businessPhone) }

    val scrollState = rememberScrollState()

    // Validaciones
    val isBusinessNameValid = businessName.isNotBlank() && businessName.length >= 3
    val isBusinessEmailValid = businessEmail.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(businessEmail).matches()
    val canRegister = isBusinessNameValid && isBusinessEmailValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        // Tarjeta informativa
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

        // Campo: Nombre del negocio
        OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Nombre del negocio") },
            singleLine = true,
            isError = businessName.isNotBlank() && !isBusinessNameValid,
            supportingText = {
                if (businessName.isNotBlank() && !isBusinessNameValid) {
                    Text("El nombre del negocio debe tener al menos 3 caracteres")
                }
            },
            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Campo: Email del negocio
        OutlinedTextField(
            value = businessEmail,
            onValueChange = { businessEmail = it },
            label = { Text("Email del negocio") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = businessEmail.isNotBlank() && !isBusinessEmailValid,
            supportingText = {
                if (businessEmail.isNotBlank() && !isBusinessEmailValid) {
                    Text("Ingresa un correo electrónico válido")
                }
            },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Campo: Dirección (opcional)
        OutlinedTextField(
            value = businessAddress,
            onValueChange = { businessAddress = it },
            label = { Text("Dirección (opcional)") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Campo: Teléfono (opcional)
        OutlinedTextField(
            value = businessPhone,
            onValueChange = { businessPhone = it },
            label = { Text("Teléfono (opcional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón Crear Cuenta
        Button(
            onClick = {
                viewModel.sendIntent(
                    AuthIntent.Register(
                        userEmail = state.userEmail,
                        userPassword = state.userPassword,
                        userName = state.userName,
                        businessName = businessName,
                        businessEmail = businessEmail,
                        businessPhone = businessPhone.takeIf { it.isNotBlank() },
                        businessAddress = businessAddress.takeIf { it.isNotBlank() }
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = canRegister && !state.isLoading,
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