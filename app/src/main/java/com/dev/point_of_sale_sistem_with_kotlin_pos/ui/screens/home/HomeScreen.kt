package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val state by authViewModel.state.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 🔹 Si el usuario no está autenticado, lo manda al login
    LaunchedEffect(state.isAuthenticated) {
        if (!state.isAuthenticated) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Menú",
                    modifier = Modifier
                        .padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Divider()

                DrawerItem("Ventas") {}
                DrawerItem("Devoluciones") {}
                DrawerItem("Inventario") {}
                DrawerItem("Reportes") {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
        ) {
            // 🔹 Top bar con icono de menú y foto de usuario
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🔹 Icono de menú lateral
                IconButton(onClick = {
                    scope.launch {
                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.Black
                    )
                }

                // 🔹 Imagen del usuario con menú desplegable
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
                                authViewModel.sendIntent(AuthIntent.Logout)
                            }
                        )
                    }
                }
            }

            // 🔹 Contenido central
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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

                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }

                    state.error?.let { authError ->
                        val message = (authError as? AuthError.Other)?.message ?: "Error desconocido"
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                    }

                    state.successMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 🔹 Menú inferior
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
        }
    }
}

@Composable
private fun DrawerItem(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 20.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}
