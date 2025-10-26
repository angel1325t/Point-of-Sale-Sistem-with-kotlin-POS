package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.components.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModel
import kotlinx.coroutines.delay
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RegisterScreen(
    navController: NavHostController,
    registerViewModel: RegisterViewModel,
    authSessionViewModel: AuthSessionViewModel
) {
    val state by registerViewModel.state.collectAsState()
    val sessionState by authSessionViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate to home if authenticated
    LaunchedEffect(sessionState.isAuthenticated) {
        if (sessionState.isAuthenticated) {
            Log.d("RegisterScreen", "User is authenticated, navigating to home")
            navController.navigate("home") {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    // Show success message in Snackbar
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            Log.d("RegisterScreen", "Showing success message: ${state.successMessage}")
            snackbarHostState.showSnackbar(state.successMessage!!)
            delay(2000) // Mostrar el mensaje durante 2 segundos
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Crear Cuenta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.currentStep > 1) registerViewModel.goToPreviousStep()
                        else navController.navigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
        ) {
            StepIndicator(
                currentStep = state.currentStep,
                totalSteps = 2,
                stepTitles = listOf("Información Personal", "Datos del Negocio"),
                modifier = Modifier.padding(24.dp)
            )

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    1 -> UserInfoStep(registerViewModel)
                    2 -> BusinessInfoStep(registerViewModel)
                }
            }

            // Show error message in Snackbar
            LaunchedEffect(state.error) {
                state.error?.let { error ->
                    val errorMessage = when (error) {
                        is AuthError.InvalidCredentials -> error.message
                        is AuthError.UsernameTaken -> error.message
                        is AuthError.CompanyNameTaken -> error.message
                        is AuthError.BranchNameTaken -> error.message
                        is AuthError.Other -> error.message
                    }
                    Log.d("RegisterScreen", "Displaying error: $errorMessage")
                    snackbarHostState.showSnackbar(errorMessage)
                    delay(4000) // Show error for 4 seconds
                    registerViewModel.clearError() // Clear error after display
                }
            }
        }
    }
}