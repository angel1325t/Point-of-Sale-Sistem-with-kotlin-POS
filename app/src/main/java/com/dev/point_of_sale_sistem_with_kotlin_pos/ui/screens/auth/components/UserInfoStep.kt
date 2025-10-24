package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModel

@Composable
fun UserInfoStep(viewModel: RegisterViewModel) {

    val state by viewModel.state.collectAsState()

    val passwordsMatch = state.userPassword == state.userConfirmPassword
    val isPasswordValid = state.userPassword.length >= 6
    val canProceed = state.userName.isNotBlank() &&
            state.userEmail.isNotBlank() &&
            isPasswordValid &&
            passwordsMatch &&
            state.userConfirmPassword.isNotBlank()

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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Ingresa tus datos personales para comenzar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        CustomTextField(
            value = state.userName,
            onValueChange = viewModel::onUserNameChange,
            label = "Nombre completo",
            icon = Icons.Default.Person,
            enabled = !state.isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = state.userEmail,
            onValueChange = viewModel::onUserEmailChange,
            label = "Correo electrónico",
            icon = Icons.Default.Email,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
            enabled = !state.isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = state.userPassword,
            onValueChange = viewModel::onUserPasswordChange,
            label = "Contraseña",
            icon = Icons.Default.Lock,
            isPassword = true,
            showPassword = state.showPassword,
            onTogglePassword = viewModel::toggleShowPassword,
            enabled = !state.isLoading,
            supportingText = if (state.userPassword.isNotBlank() && !isPasswordValid)
                "Mínimo 6 caracteres" else null
        )
        Spacer(modifier = Modifier.height(16.dp))

        CustomTextField(
            value = state.userConfirmPassword,
            onValueChange = viewModel::onUserConfirmPasswordChange,
            label = "Confirmar contraseña",
            icon = Icons.Default.Lock,
            isPassword = true,
            showPassword = state.showConfirmPassword,
            onTogglePassword = viewModel::toggleShowConfirmPassword,
            enabled = !state.isLoading,
            isError = state.userConfirmPassword.isNotBlank() && !passwordsMatch,
            supportingText = if (state.userConfirmPassword.isNotBlank() && !passwordsMatch)
                "Las contraseñas no coinciden" else null
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::goToNextStep,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !state.isLoading && canProceed,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Continuar",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
