package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val state by authViewModel.state.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) }

    // Datos del usuario (Paso 1)
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userAddress by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var userPassword by remember { mutableStateOf("") }
    var userConfirmPassword by remember { mutableStateOf("") }

    // Datos de la empresa (Paso 2)
    var businessName by remember { mutableStateOf("") }
    var businessEmail by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var businessPhone by remember { mutableStateOf("") }

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            navController.navigate("home") {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Cuenta") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) {
                            currentStep--
                        } else {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Indicadores de paso
            StepIndicator(
                currentStep = currentStep,
                totalSteps = 2,
                modifier = Modifier.padding(24.dp)
            )

            // Contenido del paso actual
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "step_animation"
            ) { step ->
                when (step) {
                    1 -> UserInfoStep(
                        userName = userName,
                        onUserNameChange = { userName = it },
                        userEmail = userEmail,
                        onUserEmailChange = { userEmail = it },
                        userAddress = userAddress,
                        onUserAddressChange = { userAddress = it },
                        userPhone = userPhone,
                        onUserPhoneChange = { userPhone = it },
                        userPassword = userPassword,
                        onUserPasswordChange = { userPassword = it },
                        userConfirmPassword = userConfirmPassword,
                        onUserConfirmPasswordChange = { userConfirmPassword = it },
                        isLoading = state.isLoading,
                        onNext = { currentStep = 2 }
                    )

                    2 -> BusinessInfoStep(
                        businessName = businessName,
                        onBusinessNameChange = { businessName = it },
                        businessEmail = businessEmail,
                        onBusinessEmailChange = { businessEmail = it },
                        businessAddress = businessAddress,
                        onBusinessAddressChange = { businessAddress = it },
                        businessPhone = businessPhone,
                        onBusinessPhoneChange = { businessPhone = it },
                        isLoading = state.isLoading,
                        error = state.error,
                        onRegister = {
                            // Aquí enviarás el intent de registro cuando implementes la lógica
                            // authViewModel.sendIntent(
                            //     AuthIntent.Register(
                            //         userName, userEmail, userAddress, userPhone, userPassword,
                            //         businessName, businessEmail, businessAddress, businessPhone
                            //     )
                            // )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (step in 1..totalSteps) {
            StepCircle(
                stepNumber = step,
                isActive = step == currentStep,
                isCompleted = step < currentStep
            )

            if (step < totalSteps) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = if (step < currentStep)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun StepCircle(
    stepNumber: Int,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    isActive -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stepNumber.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isActive || isCompleted)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UserInfoStep(
    userName: String,
    onUserNameChange: (String) -> Unit,
    userEmail: String,
    onUserEmailChange: (String) -> Unit,
    userAddress: String,
    onUserAddressChange: (String) -> Unit,
    userPhone: String,
    onUserPhoneChange: (String) -> Unit,
    userPassword: String,
    onUserPasswordChange: (String) -> Unit,
    userConfirmPassword: String,
    onUserConfirmPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Información Personal",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = userName,
            onValueChange = onUserNameChange,
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = userEmail,
            onValueChange = onUserEmailChange,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = userAddress,
            onValueChange = onUserAddressChange,
            label = { Text("Dirección") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = userPhone,
            onValueChange = onUserPhoneChange,
            label = { Text("Teléfono") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = userPassword,
            onValueChange = onUserPasswordChange,
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = userConfirmPassword,
            onValueChange = onUserConfirmPasswordChange,
            label = { Text("Confirmar Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && userName.isNotBlank() &&
                    userEmail.isNotBlank() && userPassword.isNotBlank() &&
                    userPassword == userConfirmPassword
        ) {
            Text("Siguiente")
        }

        if (userPassword.isNotBlank() && userConfirmPassword.isNotBlank() &&
            userPassword != userConfirmPassword) {
            Text(
                text = "Las contraseñas no coinciden",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun BusinessInfoStep(
    businessName: String,
    onBusinessNameChange: (String) -> Unit,
    businessEmail: String,
    onBusinessEmailChange: (String) -> Unit,
    businessAddress: String,
    onBusinessAddressChange: (String) -> Unit,
    businessPhone: String,
    onBusinessPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    error: Any?,
    onRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Información del Negocio",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = businessName,
            onValueChange = onBusinessNameChange,
            label = { Text("Nombre del Negocio") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = businessEmail,
            onValueChange = onBusinessEmailChange,
            label = { Text("Email del Negocio") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = businessAddress,
            onValueChange = onBusinessAddressChange,
            label = { Text("Dirección") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = businessPhone,
            onValueChange = onBusinessPhoneChange,
            label = { Text("Teléfono") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        error?.let {
            Text(
                text = "Error al crear la cuenta",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && businessName.isNotBlank() &&
                    businessEmail.isNotBlank()
        ) {
            Text("Crear Cuenta")
        }
    }
}