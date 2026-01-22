package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.components.profile.ProfilePhotoDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    sessionViewModel: AuthSessionViewModel
) {
val state by sessionViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(2) } // Tab de Perfil seleccionado
    var showPhotoDialog by remember { mutableStateOf(false) }

    // Redirigir al login si no está autenticado
    LaunchedEffect(state.isAuthenticated, state.isLoading) {
        if (!state.isAuthenticated && !state.isLoading) {
            navController.navigate("login") {
                popUpTo("profile") { inclusive = true }
            }
        }
    }

// Mostrar error en Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val message = when (error) {
                is AuthError.Other -> error.customMessage
                else -> null
            }

            message?.let {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Mostrar éxito en Snackbar
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            val displayMessage = when (message) {
                "PROFILE_PHOTO_UPDATED" -> "Profile photo updated successfully"
                "EMAIL_CHANGE_SUCCESS" -> "Email changed successfully"
                else -> message
            }
            
            snackbarHostState.showSnackbar(
                message = displayMessage,
                duration = SnackbarDuration.Short
            )
            
            // Clear success message
            sessionViewModel.sendIntent(AuthIntent.ClearMessages)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Store, contentDescription = "Sucursales") },
                    label = { Text("Sucursales") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        navController.navigate("branches")
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    selected = selectedTab == 2,
                    onClick = { }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
        ) {
            // Top bar simple: solo el título "Perfil"
            Text(
                text = "Perfil",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

// User Info Card
            UserProfileCard(
                userName = extractUserName(state.email),
                profilePhotoUrl = state.profilePhotoUrl,
                onPhotoClick = { showPhotoDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Menu Options
            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Configuración",
                onClick = {
                    // TODO: Navegar a configuración
                    // navController.navigate("settings")
                }
            )

ProfileMenuItem(
                icon = Icons.Default.Lock,
                title = "Resetear mi contraseña",
                onClick = {
                    navController.navigate("enhanced_password_reset")
                }
            )

            ProfileMenuItem(
                icon = Icons.Default.Email,
                title = "Cambiar email",
                onClick = {
                    navController.navigate("email_change_verification")
                }
            )

            ProfileMenuItem(
                icon = Icons.Default.Assessment,
                title = "Reportes",
                onClick = {
                    // TODO: Navegar a reportes
                    // navController.navigate("reports")
                }
            )

            ProfileMenuItem(
                icon = Icons.Default.Description,
                title = "Política",
                onClick = {
                    // TODO: Navegar a política
                    // navController.navigate("policy")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // Logout option
            ProfileMenuItem(
                icon = Icons.Default.ExitToApp,
                title = "Cerrar sesión",
                onClick = {
                    sessionViewModel.sendIntent(AuthIntent.Logout)
                },
                textColor = Color(0xFFD32F2F)
            )

if (state.isLoading || state.isUploadingPhoto) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = if (state.isUploadingPhoto) "Uploading photo..." else "Loading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
}
        }
    }

    // Profile photo dialog
    if (showPhotoDialog) {
        ProfilePhotoDialog(
            currentPhotoUrl = state.profilePhotoUrl,
            onDismiss = { 
                showPhotoDialog = false
                sessionViewModel.sendIntent(AuthIntent.ClearPhotoUploadState)
            },
            sessionViewModel = sessionViewModel
        )
    }
}
    }
}

@Composable
private fun UserProfileCard(
    userName: String,
    profilePhotoUrl: String?,
    onPhotoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circular desde Supabase con click para cambiar
            Box(
                modifier = Modifier.size(64.dp)
            ) {
                AsyncImage(
                    model = profilePhotoUrl ?: "https://uhtlmanoxrfdefelpybs.supabase.co/storage/v1/object/public/avatars/default-image.webp",
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPhotoClick)
                )
                
                // Camera icon overlay
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onPhotoClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap photo to change",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    textColor: Color = Color.Unspecified
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (textColor != Color.Unspecified) textColor
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (textColor != Color.Unspecified) textColor
                    else MaterialTheme.colorScheme.inverseSurface
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ir",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

// Función auxiliar para extraer el nombre del email
private fun extractUserName(email: String?): String {
    if (email.isNullOrBlank()) return "Usuario"

    // Si el email tiene un nombre antes del @, lo usamos
    val userName = email.substringBefore("@")

    // Capitalizamos la primera letra de cada palabra
    return userName.split(".", "_", "-")
        .joinToString(" ") { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }
}