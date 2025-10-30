package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.components.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModel
import kotlinx.coroutines.delay

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
    val context = LocalContext.current  // Para acceder a strings.xml

    // Navegar si el usuario ya está autenticado
    LaunchedEffect(sessionState.isAuthenticated) {
        if (sessionState.isAuthenticated) {
            Log.d("RegisterScreen", "User is authenticated, navigating to home")
            navController.navigate("home") {
                popUpTo("register") { inclusive = true }
            }
        }
    }

    // Mostrar mensaje de éxito
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            Log.d("RegisterScreen", "Showing success message: $message")
            snackbarHostState.showSnackbar(message)
            delay(2000)
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val errorMessage = when (error) {
                is AuthError.InvalidCredentials ->
                    context.getString(R.string.invalid_credentials)
                is AuthError.UsernameTaken ->
                    context.getString(R.string.username_already_exists, error.username)
                is AuthError.EmailTaken ->
                    context.getString(R.string.user_email_already_exists, error.email)
                is AuthError.CompanyEmailTaken ->
                    context.getString(R.string.company_email_already_exists, error.email)
                is AuthError.CompanyNameTaken ->
                    context.getString(R.string.company_name_already_exists, error.name)
                is AuthError.BranchNameTaken ->
                    context.getString(R.string.branch_name_already_exists, error.name)
                is AuthError.Other ->
                    error.customMessage
            }

            Log.d("RegisterScreen", "Displaying error: $errorMessage")
            snackbarHostState.showSnackbar(errorMessage)
            delay(4000)
            registerViewModel.clearError()
        }
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.create_account_text)  ,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.currentStep > 1) {
                            registerViewModel.goToPreviousStep()
                        } else {
                            navController.navigateUp()
                        }
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
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    1 -> UserInfoStep(registerViewModel)
                    2 -> BusinessInfoStep(registerViewModel)
                }
            }
        }
    }
}