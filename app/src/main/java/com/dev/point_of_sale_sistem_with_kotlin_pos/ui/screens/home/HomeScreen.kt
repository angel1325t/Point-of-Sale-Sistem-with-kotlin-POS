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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    sessionViewModel: AuthSessionViewModel
) {
    val state by sessionViewModel.state.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Redirigir al login si no está autenticado
    LaunchedEffect(state.isAuthenticated, state.isLoading) {
        if (!state.isAuthenticated && !state.isLoading) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    // Mostrar error en Snackbar (mejor UX que un Text fijo)
    // Mostrar error en Snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            val message = when (error) {
                is AuthError.Other -> error.customMessage
                else -> null // Ignorar errores que no aplican al home
            }

            message?.let {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }


    // Mostrar mensaje de éxito
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Menú",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                HorizontalDivider()
                DrawerItem(stringResource(R.string.drawer_item_1)) {}
                DrawerItem(stringResource(R.string.drawer_item_2)) {}
                DrawerItem(stringResource(R.string.drawer_item_3)) {}
                DrawerItem(stringResource(R.string.drawer_item_4)) {}
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                        label = { Text(stringResource(R.string.navegacion_1)) },
                        selected = true,
                        onClick = { }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Sucursales") },
                        label = { Text(stringResource(R.string.navegacion_2)) },
                        selected = false,
                        onClick = { }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Configuración") },
                        label = { Text(stringResource(R.string.navegacion_3)) },
                        selected = false,
                        onClick = { }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF2F2F2))
                    .padding(paddingValues)
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.Black)
                    }

                    Box {
                        AsyncImage(
                            model = "https://uhtlmanoxrfdefelpybs.supabase.co/storage/v1/object/public/avatars/default-image.webp",
                            contentDescription = "Foto de usuario",
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
                                text = { Text(stringResource(R.string.dropdown_menu_1)) },
                                onClick = { menuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dropdown_menu_2)) },
                                onClick = { menuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dropdown_menu_3)) },
                                onClick = {
                                    menuExpanded = false
                                    sessionViewModel.sendIntent(AuthIntent.Logout)
                                }
                            )
                        }
                    }
                }

                // Contenido principal
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
                            text = stringResource(R.string.bienvenida),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
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
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 20.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    )
}