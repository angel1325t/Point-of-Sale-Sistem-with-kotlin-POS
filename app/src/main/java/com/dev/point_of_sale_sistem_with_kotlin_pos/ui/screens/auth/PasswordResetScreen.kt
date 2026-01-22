package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import kotlinx.coroutines.launch

@Composable
fun PasswordResetScreen(
    navController: NavHostController,
    authSessionViewModel: AuthSessionViewModel,
    deepLinkToken: String? = null,
    deepLinkEmail: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by authSessionViewModel.state.collectAsState()

    // Pre-fill email if coming from deep link
    var email by rememberSaveable { mutableStateOf(deepLinkEmail ?: "") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isTokenReset = deepLinkToken != null
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Handle success message
    LaunchedEffect(state.successMessage) {
        when (state.successMessage) {
            "PASSWORD_RESET_EMAIL_SENT" -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "Password reset email sent successfully!",
                        duration = SnackbarDuration.Long
                    )
                    // Navigate back to login after showing success message
                    kotlinx.coroutines.delay(2000)
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
            "PASSWORD_RESET_SUCCESS" -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "Password reset successfully!",
                        duration = SnackbarDuration.Long
                    )
                    kotlinx.coroutines.delay(2000)
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
        }
    }

    // Handle error messages
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val errorMessage = when (error) {
                is AuthError.Other -> error.toString()
                else -> "An error occurred"
            }
            emailError = errorMessage
        }
    }

    // Clear error when email or password changes
    LaunchedEffect(email) {
        if (emailError != null) {
            emailError = null
            authSessionViewModel.resetState()
        }
    }
    
    LaunchedEffect(newPassword) {
        if (passwordError != null) {
            passwordError = null
            authSessionViewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.onBackground
                )
            }
            
            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo or icon
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email",
                modifier = Modifier.size(80.dp),
                tint = colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Forgot your password?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = if (isTokenReset) {
                    "Enter your new password below."
                } else {
                    "Enter your email address and we'll send you a link to reset your password."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field (only show for email-based reset)
            if (!isTokenReset) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text("Email address") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        errorBorderColor = colorScheme.error
                    ),
                    isError = emailError != null,
                    supportingText = emailError?.let { 
                        { Text(it, color = colorScheme.error) } 
                    },
                    enabled = !state.isLoading
                )
            }

            // Password fields for token-based reset
            if (isTokenReset) {
                // New password field
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New password") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        errorBorderColor = colorScheme.error
                    ),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { 
                        { Text(it, color = colorScheme.error) } 
                    },
                    enabled = !state.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm password field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm new password") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        errorBorderColor = colorScheme.error
                    ),
                    isError = passwordError != null,
                    enabled = !state.isLoading
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reset button
            Button(
                onClick = {
                    if (isTokenReset) {
                        // Token-based password reset
                        if (newPassword.isBlank()) {
                            passwordError = "Password cannot be empty"
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            passwordError = "Password must be at least 6 characters"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            passwordError = "Passwords do not match"
                            return@Button
                        }
                        deepLinkToken?.let { token ->
                            authSessionViewModel.sendIntent(AuthIntent.ResetPasswordWithToken(token, newPassword))
                        }
                    } else {
                        // Email-based password reset
                        if (email.isBlank()) {
                            emailError = "Email cannot be empty"
                            return@Button
                        }
                        if (!isValidEmail(email)) {
                            emailError = "Please enter a valid email address"
                            return@Button
                        }
                        authSessionViewModel.sendIntent(AuthIntent.ResetPassword(email))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isLoading && if (isTokenReset) {
                    newPassword.isNotBlank() && confirmPassword.isNotBlank()
                } else {
                    email.isNotBlank()
                }
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isTokenReset) "Reset Password" else "Send Reset Email",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to login link
            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text(
                    text = "Back to Login",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

private fun isValidEmail(email: String): Boolean {
    val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
    return email.matches(emailPattern.toRegex())
}