package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModel
@Composable
fun UserInfoStep(viewModel: RegisterViewModel) {
    val state by viewModel.state.collectAsState()

    // Campos locales que recuerdan su valor al rotar la pantalla
    var userName by rememberSaveable { mutableStateOf(state.userName) }
    var userEmail by rememberSaveable { mutableStateOf(state.userEmail) }
    var userPassword by rememberSaveable { mutableStateOf(state.userPassword) }
    var userConfirmPassword by rememberSaveable { mutableStateOf(state.userConfirmPassword) }
    var showPassword by rememberSaveable { mutableStateOf(state.showPassword) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(state.showConfirmPassword) }

    // Sincronizar cambios locales con el ViewModel
    LaunchedEffect(userName) { viewModel.onUserNameChange(userName) }
    LaunchedEffect(userEmail) { viewModel.onUserEmailChange(userEmail) }
    LaunchedEffect(userPassword) { viewModel.onUserPasswordChange(userPassword) }
    LaunchedEffect(userConfirmPassword) { viewModel.onUserConfirmPasswordChange(userConfirmPassword) }
    LaunchedEffect(showPassword) { if (showPassword != state.showPassword) viewModel.toggleShowPassword() }
    LaunchedEffect(showConfirmPassword) { if (showConfirmPassword != state.showConfirmPassword) viewModel.toggleShowConfirmPassword() }

    val scrollState = rememberScrollState()

    // Validaciones
    val isUserNameValid = userName.length >= 3 &&
            userName.all { it.isLetterOrDigit() || it.isWhitespace() || it == '_' }
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()
    val isPasswordValid = userPassword.length >= 6
    val passwordsMatch = userPassword == userConfirmPassword

    val canProceed = userName.isNotBlank() &&
            userEmail.isNotBlank() &&
            isUserNameValid &&
            isEmailValid &&
            isPasswordValid &&
            passwordsMatch &&
            userConfirmPassword.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        // 🔹 Tarjeta superior informativa
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
                    text = stringResource(R.string.datos_personales),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // 🔹 Nombre completo
        CustomTextField(
            value = userName,
            onValueChange = { userName = it },
            label = "Nombre completo",
            icon = Icons.Default.Person,
            enabled = !state.isLoading,
            isError = userName.isNotBlank() && !isUserNameValid,
            supportingText = if (userName.isNotBlank() && !isUserNameValid)
                stringResource(R.string.nombre_validacion)
            else null
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Correo electrónico
        CustomTextField(
            value = userEmail,
            onValueChange = { userEmail = it },
            label = stringResource(R.string.email_label),
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            enabled = !state.isLoading,
            isError = userEmail.isNotBlank() && !isEmailValid,
            supportingText = if (userEmail.isNotBlank() && !isEmailValid)
                stringResource(R.string.email_validacion)
            else null
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Contraseña
        CustomTextField(
            value = userPassword,
            onValueChange = { userPassword = it },
            label = "Contraseña",
            icon = Icons.Default.Lock,
            isPassword = true,
            showPassword = showPassword,
            onTogglePassword = { showPassword = !showPassword },
            enabled = !state.isLoading,
            showPasswordStrength = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Confirmar contraseña
        CustomTextField(
            value = userConfirmPassword,
            onValueChange = { userConfirmPassword = it },
            label = stringResource(R.string.confirmar_contrasena_label),
            icon = Icons.Default.Lock,
            isPassword = true,
            showPassword = showConfirmPassword,
            onTogglePassword = { showConfirmPassword = !showConfirmPassword },
            enabled = !state.isLoading,
            isError = userConfirmPassword.isNotBlank() && !passwordsMatch,
            supportingText = if (userConfirmPassword.isNotBlank() && !passwordsMatch)
                stringResource(R.string.contrasena_validacion_6)
            else null
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // 🔹 Botón continuar
        Button(
            onClick = viewModel::goToNextStep,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !state.isLoading && canProceed,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                stringResource(R.string.continuar),
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