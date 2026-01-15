package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun BiometricAuthScreen(
    navController: NavController,
    targetRoute: String
) {
    val context = LocalContext.current
    var authenticationSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Navegar cuando la autenticación sea exitosa
    LaunchedEffect(authenticationSuccess) {
        if (authenticationSuccess) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                }
            }
            authenticationSuccess = false
        }
    }

    // Verificar disponibilidad PRIMERO y mostrar prompt solo si está disponible
    LaunchedEffect(Unit) {
        val activity = context as? FragmentActivity
        if (activity != null) {
            val biometricManager = BiometricManager.from(activity)

            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
                        or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            // Si NO hay biometría disponible, dejar pasar directamente
            if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                authenticationSuccess = true
                return@LaunchedEffect
            }

            // Si SÍ hay biometría, mostrar el prompt
            showBiometricPrompt(
                activity = activity,
                onSuccess = { authenticationSuccess = true },
                onError = { message ->
                    showError = true
                    errorMessage = message
                },
                onFailed = {
                    // Ya no mostramos nada aquí
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Huella digital",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Autenticación Biométrica",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Usa tu huella digital o reconocimiento facial para continuar",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (showError) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = {
                    showError = false
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        showBiometricPrompt(
                            activity = activity,
                            onSuccess = { authenticationSuccess = true },
                            onError = { message ->
                                showError = true
                                errorMessage = message
                            },
                            onFailed = {
                                // No mostramos mensaje
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reintentar")
            }
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onFailed: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)

    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)

                // Cancelación normal (botón atrás, cancelar)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED) {

                    onError("Autenticación cancelada")
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                // YA NO MOSTRAMOS MENSAJE AQUÍ
                super.onAuthenticationFailed()
                onFailed()
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Autenticación requerida")
        .setSubtitle("Usa tu cara, huella o patrón del sistema")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
                    or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    biometricPrompt.authenticate(promptInfo)
}