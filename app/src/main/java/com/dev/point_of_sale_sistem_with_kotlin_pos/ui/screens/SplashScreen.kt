package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens

import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import kotlinx.coroutines.delay

@Composable
fun Splash(
    navController: NavHostController,
    authSessionViewModel: AuthSessionViewModel
) {
    val state by authSessionViewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isAuthenticated) {
        delay(1500)

        // Verificar si el dispositivo tiene biométrico configurado
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
                    or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        val hasBiometric = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS

        // Guardar el estado de biométrico en las preferencias
        authSessionViewModel.setHasBiometric(hasBiometric)

        // Determinar la ruta destino
        val target = if (state.isAuthenticated) "home" else "login"

        // Si tiene biométrico, pasar por la pantalla de autenticación
        if (hasBiometric) {
            navController.navigate("biometric_auth/$target") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            // Si no tiene biométrico, ir directamente al destino
            navController.navigate(target) {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    SplashScreen()
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
    }
}