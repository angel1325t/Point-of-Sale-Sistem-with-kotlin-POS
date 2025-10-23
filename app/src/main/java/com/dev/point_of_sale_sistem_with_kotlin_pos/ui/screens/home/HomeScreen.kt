package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthViewModel

@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val state by authViewModel.state.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    // 🔹 Si el usuario no está autenticado, lo manda al login
    LaunchedEffect(state.isAuthenticated) {
        if (!state.isAuthenticated) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F2))
    ) {
        // 🔹 Top bar con imagen de usuario
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = "https://uhtlmanoxrfdefelpybs.supabase.co/storage/v1/object/public/avatars/default-image.webp",
                    contentDescription = "User Image",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { menuExpanded = true }
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Perfil") },
                        onClick = { menuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Notificaciones") },
                        onClick = { menuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Cerrar sesión") },
                        onClick = {
                            menuExpanded = false
                            // 🔹 Enviar intent de logout al ViewModel
                            authViewModel.sendIntent(AuthIntent.Logout)
                        }
                    )
                }
            }
        }

        // 🔹 Menú inferior debajo del top bar
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 4.dp
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Inicio") },
                selected = true,
                onClick = { /* acción futura */ }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Sucursales") },
                label = { Text("Sucursales") },
                selected = false,
                onClick = { /* acción futura */ }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Configuración") },
                label = { Text("Configuración") },
                selected = false,
                onClick = { /* acción futura */ }
            )
        }

        // 🔹 Contenido central
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Contenido del dashboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )

                // 🔹 Mostrar indicador de carga si está procesando logout
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                // 🔹 Mostrar errores o mensajes
                state.error?.let { authError ->
                    val message = (authError as? AuthError.Other)?.message ?: "Error desconocido"
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }

                state.successMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
