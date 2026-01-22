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
import androidx.compose.material.icons.filled.Lock
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
fun EnhancedPasswordResetScreen(
    navController: NavHostController,
    authSessionViewModel: AuthSessionViewModel,
    deepLinkToken: String? = null,
    deepLinkEmail: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val state by authSessionViewModel.state.collectAsState()

    var email by rememberSaveable { mutableStateOf(deepLinkEmail ?: "") }
    var verificationCode by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    
    var currentStep: ResetStep by remember { mutableStateOf(
        if (deepLinkToken != null) ResetStep.NEW_PASSWORD else ResetStep.EMAIL_ENTRY
    )}
    
    var emailError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Handle success messages
    LaunchedEffect(state.successMessage) {
        when (state.successMessage) {
            "PASSWORD_RESET_EMAIL_SENT" -> {
"PASSWORD_RESET_EMAIL_SENT" -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Password reset code sent successfully! Please check your email.",
                        duration = SnackbarDuration.Long
                    )
                    kotlinx.coroutines.delay(2000)
                    currentStep = ResetStep.VERIFICATION_CODE
                }
            }
            "PASSWORD_RESET_CODE_VALID" -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Verification successful! Please enter your new password.",
                        duration = SnackbarDuration.Short
                    )
                    kotlinx.coroutines.delay(1000)
                    currentStep = ResetStep.NEW_PASSWORD
                }
            }
            "PASSWORD_RESET_SUCCESS" -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Password reset successfully!",
                        duration = SnackbarDuration.Long
                    )
                    kotlinx.coroutines.delay(2000)
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
            }
            "PASSWORD_RESET_CODE_VALID" -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Verification successful! Please enter your new password.",
                        duration = SnackbarDuration.Short
                    )
                    kotlinx.coroutines.delay(1000)
                    currentStep = ResetStep.NEW_PASSWORD
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
            "PASSWORD_RESET_TOKEN_VALID" -> {
                currentStep = ResetStep.NEW_PASSWORD
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
            when (currentStep) {
                ResetStep.EMAIL_ENTRY -> emailError = errorMessage
                ResetStep.VERIFICATION_CODE -> codeError = errorMessage
                ResetStep.NEW_PASSWORD -> passwordError = errorMessage
            }
        }
    }

    // Clear errors when input changes
    LaunchedEffect(email) {
        if (emailError != null && currentStep == ResetStep.EMAIL_ENTRY) {
            emailError = null
            authSessionViewModel.sendIntent(AuthIntent.ClearMessages)
        }
    }
    
    LaunchedEffect(verificationCode) {
        if (codeError != null && currentStep == ResetStep.VERIFICATION_CODE) {
            codeError = null
            authSessionViewModel.sendIntent(AuthIntent.ClearMessages)
        }
    }
    
    LaunchedEffect(newPassword, confirmPassword) {
        if (passwordError != null && currentStep == ResetStep.NEW_PASSWORD) {
            passwordError = null
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
                onClick = { 
                    if (currentStep != ResetStep.EMAIL_ENTRY) {
                        currentStep = ResetStep.EMAIL_ENTRY
                        verificationCode = ""
                        newPassword = ""
                        confirmPassword = ""
                    } else {
                        navController.popBackStack()
                    }
                }
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
            // Step indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicator(
                    step = 1,
                    isActive = currentStep == ResetStep.EMAIL_ENTRY,
                    isCompleted = currentStep != ResetStep.EMAIL_ENTRY,
                    colorScheme = colorScheme
                )
                
                Divider(
                    modifier = Modifier.width(40.dp),
                    color = if (currentStep != ResetStep.EMAIL_ENTRY) colorScheme.primary else colorScheme.outline
                )
                
                StepIndicator(
                    step = 2,
                    isActive = currentStep == ResetStep.VERIFICATION_CODE,
                    isCompleted = currentStep == ResetStep.NEW_PASSWORD,
                    colorScheme = colorScheme
                )
                
                Divider(
                    modifier = Modifier.width(40.dp),
                    color = if (currentStep == ResetStep.NEW_PASSWORD) colorScheme.primary else colorScheme.outline
                )
                
                StepIndicator(
                    step = 3,
                    isActive = currentStep == ResetStep.NEW_PASSWORD,
                    isCompleted = false,
                    colorScheme = colorScheme
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Icon
            Icon(
                imageVector = when (currentStep) {
                    ResetStep.EMAIL_ENTRY -> Icons.Default.Email
                    ResetStep.VERIFICATION_CODE -> Icons.Default.Email
                    ResetStep.NEW_PASSWORD -> Icons.Default.Lock
                },
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title and subtitle
            when (currentStep) {
                ResetStep.EMAIL_ENTRY -> {
                    Text(
                        text = "Forgot your password?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter your email address and we'll send you a verification code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                ResetStep.VERIFICATION_CODE -> {
                    Text(
                        text = "Verify your email",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter the verification code we sent to $email",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                ResetStep.NEW_PASSWORD -> {
                    Text(
                        text = "Set new password",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter your new password below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Form fields based on current step
            when (currentStep) {
                ResetStep.EMAIL_ENTRY -> {
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
                
                ResetStep.VERIFICATION_CODE -> {
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
                        text = "If you don't see the email, check your spam folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                ResetStep.NEW_PASSWORD -> {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password") },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm new password") },
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
                        enabled = !state.isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action button
            Button(
                onClick = {
                    when (currentStep) {
                        ResetStep.EMAIL_ENTRY -> {
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
                        
                        ResetStep.VERIFICATION_CODE -> {
                            if (verificationCode.isBlank()) {
                                codeError = "Verification code cannot be empty"
                                return@Button
                            }
                            authSessionViewModel.sendIntent(AuthIntent.VerifyPasswordResetCode(email, verificationCode))
                        }
                        
                        ResetStep.NEW_PASSWORD -> {
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
                            } ?: run {
                                // For code-based flow, we need to generate or use a token
                                val resetToken = "token_${verificationCode}_${System.currentTimeMillis()}"
                                authSessionViewModel.sendIntent(AuthIntent.ResetPasswordWithToken(resetToken, newPassword))
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isLoading && when (currentStep) {
                    ResetStep.EMAIL_ENTRY -> email.isNotBlank()
                    ResetStep.VERIFICATION_CODE -> verificationCode.isNotBlank()
                    ResetStep.NEW_PASSWORD -> newPassword.isNotBlank() && confirmPassword.isNotBlank()
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
                        text = when (currentStep) {
                            ResetStep.EMAIL_ENTRY -> "Send Verification Code"
                            ResetStep.VERIFICATION_CODE -> "Verify Code"
                            ResetStep.NEW_PASSWORD -> "Reset Password"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

// Resend code button (only in verification step)
            if (currentStep == ResetStep.VERIFICATION_CODE) {
                val currentTime = System.currentTimeMillis()
                val canResend = state.codeResendCooldown == 0L || currentTime - state.codeResendCooldown > 60000 // 1 minute cooldown
                
                TextButton(
                    onClick = { 
                        if (canResend) {
                            authSessionViewModel.sendIntent(AuthIntent.ResetPassword(email))
                        }
                    },
                    enabled = !state.isLoading && canResend
                ) {
                    Text(
                        text = if (canResend) "Resend Code" else "Wait ${((60000 - (currentTime - state.codeResendCooldown)) / 1000)}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (canResend) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Back to login link
            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text(
                    text = "Back to Login",
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

enum class ResetStep {
    EMAIL_ENTRY,
    VERIFICATION_CODE,
    NEW_PASSWORD
}

@Composable
private fun StepIndicator(
    step: Int,
    isActive: Boolean,
    isCompleted: Boolean,
    colorScheme: ColorScheme
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(
                color = when {
                    isCompleted -> colorScheme.primary
                    isActive -> colorScheme.primary
                    else -> colorScheme.outline
                },
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isCompleted) "✓" else step.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive || isCompleted) colorScheme.onPrimary else colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun isValidEmail(email: String): Boolean {
    val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
    return email.matches(emailPattern.toRegex())
}