package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.LoginViewModel

@Composable
fun LoginScreen(
    navController: NavHostController,
    loginViewModel: LoginViewModel,
    authSessionViewModel: AuthSessionViewModel
) {
    val state by loginViewModel.state.collectAsState()
    val sessionState by authSessionViewModel.state.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(sessionState.isAuthenticated) {
        if (sessionState.isAuthenticated) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
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
            Text(stringResource(R.string.iniciar_sesion), style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.contrasena)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            Button(
                onClick = {
                    loginViewModel.sendIntent(AuthIntent.Login(email, password))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                Text(stringResource(R.string.entrar))
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            state.error?.let { authError ->
                val message = when(authError) {
                    AuthError.InvalidCredentials -> stringResource(R.string.email_o_contrasena_incorrectos)
                    is AuthError.Other -> authError.message
                }
                Text(text = message, color = Color.Red)
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.no_tienes_cuenta),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { navController.navigate("register") }
                ) {
                    Text(stringResource(R.string.registrate))
                }
            }

            LaunchedEffect(state.successMessage, state.error) {
                state.successMessage?.let { _ ->
                    kotlinx.coroutines.delay(5000)
                }
            }
        }
    }
}

