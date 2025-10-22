package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthViewModel

@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val state by authViewModel.state.collectAsState()

    // 🔹 Si el usuario ya no está autenticado → volver a login
    LaunchedEffect(state.isAuthenticated) {
        if (!state.isAuthenticated) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pantalla de inicio (Home)",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // 🔹 Enviar intent de logout al ViewModel
                    authViewModel.sendIntent(AuthIntent.Logout)
                },
                enabled = !state.isLoading
            ) {
                Text("Cerrar sesión")
            }

            // 🔹 Mostrar indicador de carga si está procesando logout
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            // 🔹 Mostrar mensaje de error o éxito
            state.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            state.successMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
