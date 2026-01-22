package com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dev.point_of_sale_sistem_with_kotlin_pos.MyApplication
import com.dev.point_of_sale_sistem_with_kotlin_pos.R
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.permissions.PermissionChecker
import com.dev.point_of_sale_sistem_with_kotlin_pos.intents.auth.AuthIntent
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.auth.AuthError
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.HybridBranchRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.dashboard.DashboardRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.components.BranchSelectorDialog
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home.components.DashboardSection
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.BranchViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.dashboard.DashboardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    sessionViewModel: AuthSessionViewModel,
    branchRepository: HybridBranchRepository? = null
) {
    val state by sessionViewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val localContext = LocalContext.current

    val branchChangedMessage = stringResource(R.string.branch_changed_success)
    val logoutSuccessMessage = stringResource(R.string.logout_success)
    val userLabel = stringResource(R.string.user)
    val posSystemLabel = stringResource(R.string.pos_system)
    val currentBranchLabel = stringResource(R.string.current_branch)
    val operationsLabel = stringResource(R.string.operations)
    val salesLabel = stringResource(R.string.sales)
    val cashRegisterManagementLabel = stringResource(R.string.cash_register_management)
    val cashRegisterLabel = stringResource(R.string.cash_register)
    val returnsLabel = stringResource(R.string.returns)
    val inventoryLabel = stringResource(R.string.inventory)
    val reportsLabel = stringResource(R.string.reports)
    val administrationLabel = stringResource(R.string.administration)
    val roleManagementLabel = stringResource(R.string.role_management)
    val usersLabel = stringResource(R.string.users)
    val branchesLabel = stringResource(R.string.branches)
    val homeLabel = stringResource(R.string.home)
    val menuLabel = stringResource(R.string.menu)
    val dashboardLabel = stringResource(R.string.dashboard)
    val welcomePosSystemLabel = stringResource(R.string.welcome_pos_system)
    val changingBranchLabel = stringResource(R.string.changing_branch)
    val pleaseWaitLabel = stringResource(R.string.please_wait)

    val app = localContext.applicationContext as MyApplication
    val sessionPreferences = remember { app.sessionPreferences }

    val branchViewModel = remember(branchRepository, sessionPreferences) {
        branchRepository?.let {
            BranchViewModel(it)
        } ?: BranchViewModel(HybridBranchRepository(localContext, supabase, sessionPreferences))
    }
    val branchState by branchViewModel.state.collectAsState()

    val dashboardViewModel = remember(state.branchId) {
        if (state.branchId != null) {
            DashboardViewModel(
                repository = DashboardRepository(supabase),
                branchId = state.branchId
            )
        } else {
            null
        }
    }

    val dashboardState = dashboardViewModel?.state?.collectAsState()

    val showBranchSelector = state.isAuthenticated && state.branchId == null && !state.isLoading

    val currentBranch = remember(state.branchId, branchState.branches) {
        state.branchId?.let { branchId ->
            branchState.branches.find { it.branchId == branchId }
        }
    }

    LaunchedEffect(state.isAuthenticated, state.isLoading) {
        if (!state.isAuthenticated && !state.isLoading) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

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

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            val displayMessage = when (msg) {
                "BRANCH_CHANGED" -> branchChangedMessage
                "LOGOUT_SUCCESS" -> logoutSuccessMessage
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
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = state.email ?: userLabel,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = posSystemLabel,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )

                            state.roleName?.let { role ->
                                Text(
                                    text = "Rol: $role",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            currentBranch?.let { branch ->
                                Surface(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Store,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = currentBranchLabel,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = branch.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    DrawerSection(operationsLabel)

                    if (PermissionChecker.canCreateSales() || PermissionChecker.canViewSales()) {
                        DrawerItem(
                            icon = Icons.Default.ShoppingCart,
                            title = salesLabel,
                            onClick = {
                                navController.navigate("sales")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewCashRegister()) {
                        DrawerItem(
                            icon = Icons.Default.ManageAccounts,
                            title = cashRegisterManagementLabel,
                            onClick = {
                                navController.navigate("cash_register/manage")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewCashTransactions()) {
                        DrawerItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = cashRegisterLabel,
                            onClick = {
                                navController.navigate("cash_register/history")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewReturns()) {
                        DrawerItem(
                            icon = Icons.Default.KeyboardReturn,
                            title = returnsLabel,
                            onClick = {
                                navController.navigate("refunds")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewInventory()) {
                        DrawerItem(
                            icon = Icons.Default.Inventory,
                            title = inventoryLabel,
                            onClick = {
                                navController.navigate("inventory")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewReports()) {
                        DrawerItem(
                            icon = Icons.Default.Assessment,
                            title = reportsLabel,
                            onClick = {
                                navController.navigate("reports")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(8.dp))

                    DrawerSection(administrationLabel)

                    if (PermissionChecker.canViewRoles()) {
                        DrawerItem(
                            icon = Icons.Default.Security,
                            title = roleManagementLabel,
                            onClick = {
                                navController.navigate("roles")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewUsers()) {
                        DrawerItem(
                            icon = Icons.Default.People,
                            title = usersLabel,
                            onClick = {
                                navController.navigate("users")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewBranches()) {
                        DrawerItem(
                            icon = Icons.Default.Store,
                            title = branchesLabel,
                            onClick = {
                                navController.navigate("branches")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewCategories()) {
                        DrawerItem(
                            icon = Icons.Default.Category,
                            title = "Categorias",
                            onClick = {
                                navController.navigate("categories")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewProducts()) {
                        DrawerItem(
                            icon = Icons.Default.Inventory2,
                            title = "Productos",
                            onClick = {
                                navController.navigate("products")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewSuppliers()) {
                        DrawerItem(
                            icon = Icons.Default.LocalShipping,
                            title = "Proveedores",
                            onClick = {
                                navController.navigate("suppliers")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    if (PermissionChecker.canViewPurchases()) {
                        DrawerItem(
                            icon = Icons.Default.ShoppingCart,
                            title = "Pedidos de Reposición",
                            onClick = {
                                navController.navigate("purchase_orders")
                                scope.launch { drawerState.close() }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
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
                        icon = { Icon(Icons.Default.Home, contentDescription = homeLabel) },
                        label = { Text(homeLabel) },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    if (PermissionChecker.canViewBranches()) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Store, contentDescription = branchesLabel) },
                        label = { Text(branchesLabel) },
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            navController.navigate("branches")
                        }
                    )
                    }
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
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
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
                                    contentDescription = menuLabel,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dashboardLabel,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
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

                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notificaciones",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when {
                            dashboardViewModel != null && dashboardState != null -> {
                                DashboardSection(
                                    viewModel = dashboardViewModel,
                                    state = dashboardState.value,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                WelcomeContent(welcomePosSystemLabel)
                            }
                        }
                    }
                }

                if (state.isLoading && state.isAuthenticated) {
                    LoadingOverlay(changingBranchLabel, pleaseWaitLabel)
                }
            }
        }
    }

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
private fun WelcomeContent(welcomeMessage: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        Icon(
            Icons.Default.Dashboard,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Text(
            text = welcomeMessage,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Selecciona una sucursal para ver el dashboard",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Icon(
            Icons.Default.ArrowDownward,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )

        Text(
            text = "Usa el menú inferior para navegar",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LoadingOverlay(changingBranchMessage: String, pleaseWaitMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = changingBranchMessage,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = pleaseWaitMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.5.sp
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
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
