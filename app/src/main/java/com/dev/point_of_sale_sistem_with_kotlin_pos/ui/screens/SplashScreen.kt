package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun Splash(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    // 🔹 MVI: USA 'state' NO 'authState'
    val state = authViewModel.state.collectAsState()

    // 🔹 MVI: NO LLAMES checkSession() - ES AUTOMÁTICO
    LaunchedEffect(Unit) {
        delay(1500) // Solo espera 1.5s

        // 🔹 MVI: USA state.value.isAuthenticated
        if (state.value.isAuthenticated) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    SplashScreen()
}

@Preview(showBackground = true)
@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "logo",
            modifier = Modifier.size(350.dp)
        )
    }
}