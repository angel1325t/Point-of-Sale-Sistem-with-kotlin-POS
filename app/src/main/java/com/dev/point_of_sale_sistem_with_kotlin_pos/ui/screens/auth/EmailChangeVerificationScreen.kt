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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import kotlinx.coroutines.launch

@Composable
fun EmailChangeVerificationScreen(
    navController: NavHostController,
    authSessionViewModel: AuthSessionViewModel,
    verificationToken: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by authSessionViewModel.state.collectAsState()
    
    var newEmail by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var verificationCode by rememberSaveable { mutableStateOf("") }
    var isVerificationStep = verificationToken != null
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Handle success message
    LaunchedEffect(state.successMessage) {
        when (state.successMessage) {
            "EMAIL_CHANGE_REQUEST_SENT" -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Verification email sent successfully! Please check your inbox.",
                        duration = SnackbarDuration.Long
                    )
                    // Navigate to verification code screen after showing success message
                    kotlinx.coroutines.delay(2000)
                    isVerificationStep = true
                }
            }
            "EMAIL_CHANGE_SUCCESS" -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Email changed successfully!",
                        duration = SnackbarDuration.Long
                    )
                    kotlinx.coroutines.delay(2000)
                    navController.navigate("profile") {
                        popUpTo("profile") { inclusive = false }
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
                is AuthError.InvalidOtpCode -> "Invalid verification code"
                is AuthError.ExpiredOtpCode -> "Verification code has expired"
                is AuthError.OtpRateLimitExceeded -> "Too many attempts. Please try again later."
                is AuthError.EmailVerificationFailed -> "Email verification failed"
                else -> "An error occurred"
            }
            if (isVerificationStep) {
                codeError = errorMessage
            } else {
                emailError = errorMessage
            }
        }
    }

    // Clear errors when input changes
    LaunchedEffect(newEmail, password) {
        if (emailError != null || passwordError != null) {
            emailError = null
            passwordError = null
            authSessionViewModel.sendIntent(AuthIntent.ClearMessages)
        }
    }
    
    LaunchedEffect(verificationCode) {
        if (codeError != null) {
            codeError = null
            authSessionViewModel.sendIntent(AuthIntent.ClearMessages)
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
                text = "Change Email",
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
            // Icon
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email",
                modifier = Modifier.size(80.dp),
                tint = colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = if (isVerificationStep) {
                    "Verify Email Change"
                } else {
                    "Change Your Email"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = if (isVerificationStep) {
                    "Enter the verification code from your email to complete the change."
                } else {
                    "Enter your new email address and current password to request an email change."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isVerificationStep) {
                // New email field
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it.trim() },
                    label = { Text("New email address") },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Current password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Current password") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    visualTransformation = PasswordVisualTransformation(),
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
            } else {
                // Verification code field
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { verificationCode = it.trim() },
                    label = { Text("Verification code") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outline,
                        errorBorderColor = colorScheme.error
                    ),
                    isError = codeError != null,
                    supportingText = codeError?.let { 
                        { Text(it, color = colorScheme.error) } 
                    },
                    enabled = !state.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "If you don't see the email, check your spam folder or request a new code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action button
            Button(
                onClick = {
                    if (isVerificationStep) {
                        // Verify email change
                        if (verificationCode.isBlank()) {
                            codeError = "Verification code cannot be empty"
                            return@Button
                        }
                        val token = verificationToken ?: verificationCode
                        authSessionViewModel.sendIntent(AuthIntent.VerifyEmailChange(token))
                    } else {
                        // Request email change
                        if (newEmail.isBlank()) {
                            emailError = "New email cannot be empty"
                            return@Button
                        }
                        if (!isValidEmail(newEmail)) {
                            emailError = "Please enter a valid email address"
                            return@Button
                        }
                        if (password.isBlank()) {
                            passwordError = "Password cannot be empty"
                            return@Button
                        }
                        authSessionViewModel.sendIntent(AuthIntent.RequestEmailChange(newEmail, password))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isLoading && if (isVerificationStep) {
                    verificationCode.isNotBlank()
                } else {
                    newEmail.isNotBlank() && password.isNotBlank()
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
                        text = if (isVerificationStep) "Verify Email Change" else "Request Email Change",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resend code button (only in verification step)
            if (isVerificationStep) {
                TextButton(
                    onClick = { 
                        // Go back to request step
                        isVerificationStep = false
                        verificationCode = ""
                    },
                    enabled = !state.isLoading
                ) {
                    Text(
                        text = "Request New Code",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Cancel button
            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onBackground.copy(alpha = 0.7f)
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