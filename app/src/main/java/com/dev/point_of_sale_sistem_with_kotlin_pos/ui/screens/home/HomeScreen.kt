package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.BranchRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.components.BranchSelectorDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.BranchViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    sessionViewModel: AuthSessionViewModel
) {
    val state by sessionViewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ ViewModel de branches para obtener la lista
    val branchViewModel = remember { BranchViewModel(BranchRepository(supabase)) }
    val branchState by branchViewModel.state.collectAsState()

    // ✅ Determinar si mostrar selector de sucursal
    val showBranchSelector = state.isAuthenticated && state.branchId == null && !state.isLoading

    // ✅ Obtener la sucursal actual
    val currentBranch = remember(state.branchId, branchState.branches) {
        state.branchId?.let { branchId ->
            branchState.branches.find { it.branchId == branchId }
        }
    }

    // Redirigir al login si no está autenticado
    LaunchedEffect(state.isAuthenticated, state.isLoading) {
        if (!state.isAuthenticated && !state.isLoading) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
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

    // Mostrar mensaje de éxito
    val context = LocalContext.current

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            val displayMessage = when (msg) {
                "BRANCH_CHANGED" -> context.getString(R.string.branch_changed_success)
                "LOGOUT_SUCCESS" -> context.getString(R.string.logout_success)
                else -> msg
            }
            snackbarHostState.showSnackbar(displayMessage, duration = SnackbarDuration.Short)
        }
    }


    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Header del drawer
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = state.email ?: stringResource(R.string.user),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.pos_system),
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            Spacer(Modifier.height(8.dp))
                            currentBranch?.let { branch ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Store,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.current_branch),
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                            Text(
                                                text = branch.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    DrawerSection(stringResource(R.string.operations))

                    DrawerItem(
                        icon = Icons.Default.ShoppingCart,
                        title = stringResource(R.string.sales),
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.ManageAccounts,
                        title = stringResource(R.string.cash_register_management),
                        onClick = {
                            navController.navigate("cash_register/manage")
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = stringResource(R.string.cash_register),
                        onClick = {
                            navController.navigate("cash_register/history")
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.KeyboardReturn,
                        title = stringResource(R.string.returns),
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.Inventory,
                        title = stringResource(R.string.inventory),
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.Assessment,
                        title = stringResource(R.string.reports),
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(8.dp))

                    DrawerSection(stringResource(R.string.administration))

                    DrawerItem(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.role_management),
                        onClick = {
                            navController.navigate("roles")
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.People,
                        title = stringResource(R.string.users),
                        onClick = {
                            navController.navigate("users")
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        icon = Icons.Default.Store,
                        title = stringResource(R.string.branches),
                        onClick = {
                            navController.navigate("branches")
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.home)) },
                        label = { Text(stringResource(R.string.home)) },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Store, contentDescription = stringResource(R.string.branches)) },
                        label = { Text(stringResource(R.string.branches)) },
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
                        onClick = {
                            selectedTab = 2
                            navController.navigate("profile")
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(paddingValues)
                ) {
                    // Top bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open()
                                        else drawerState.close()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.menu),
                                    tint = Color.Gray
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.dashboard),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                currentBranch?.let {
                                    Text(
                                        text = it.name,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // ✅ Ícono de notificaciones simple (sin dropdown)
                            IconButton(
                                onClick = {
                                    // TODO: Navegar a pantalla de notificaciones
                                    // navController.navigate("notifications")
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notificaciones",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(28.dp)
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
                            Icon(
                                Icons.Default.Dashboard,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = stringResource(R.string.welcome_pos_system),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.use_bottom_menu_navigate),
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                            if (state.isLoading) {
                                Spacer(Modifier.height(16.dp))
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }

                // ✅ Overlay de carga cuando está cambiando de sucursal
                if (state.isLoading && state.isAuthenticated) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                strokeWidth = 6.dp
                            )
                            Text(
                                text = stringResource(R.string.changing_branch),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.please_wait),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ Mostrar selector de sucursal si es necesario
    if (showBranchSelector) {
        BranchSelectorDialog(
            branches = branchState.branches,
            isLoading = branchState.isLoading,
            onBranchSelected = { branchId ->
                sessionViewModel.sendIntent(AuthIntent.ChangeBranch(branchId))
            }
        )
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.inverseSurface
    )
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
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
                .padding(vertical = 12.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.inverseSurface
            )
        }
    }
}