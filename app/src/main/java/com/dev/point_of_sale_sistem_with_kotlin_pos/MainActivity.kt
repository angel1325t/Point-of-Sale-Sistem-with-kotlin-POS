package com.dev.point_of_sale_sistem_with_kotlin_pos

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.BranchRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.security.AppLifecycleObserver
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.Splash
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.BranchesScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles.RoleFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles.RolePermissionsScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles.RolesListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.users.UserFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.users.UsersListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.BiometricAuthScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.LoginScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.RegisterScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.components.UserDisabledScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home.HomeScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.CashRegisterHistoryScreen
//import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.CashRegisterMainScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.CashRegisterManagementScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home.profile.ProfileScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.theme.AppTheme
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.BranchViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.RoleViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.UserViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.LoginViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModelFactory
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.cash_register.CashRegisterViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLifecycleObserver.start()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    val navController = rememberNavController()

                    val sessionPreferences = remember {
                        (application as MyApplication).sessionPreferences
                    }
                    val authSessionViewModel = remember {
                        AuthSessionViewModel(supabase, sessionPreferences)
                    }
                    val loginViewModel = remember {
                        LoginViewModel(supabase, authSessionViewModel)
                    }

                    // ViewModels de roles y usuarios
                    val roleRepository = remember { RoleRepository(supabase) }
                    val roleViewModel = remember { RoleViewModel(roleRepository) }

                    val userRepository = remember { UserRepository(supabase) }
                    val userViewModel = remember { UserViewModel(userRepository) }

                    // ViewModel de sucursales
                    val branchRepository = remember { BranchRepository(supabase) }
                    val branchViewModel = remember { BranchViewModel(branchRepository) }

                    val cashRegisterRepository = remember {
                        CashRegisterRepository(supabase)
                    }

                    val cashRegisterViewModel = remember {
                        CashRegisterViewModel(cashRegisterRepository)
                    }

                    AppNavigation(
                        navController = navController,
                        loginViewModel = loginViewModel,
                        authSessionViewModel = authSessionViewModel,
                        roleViewModel = roleViewModel,
                        userViewModel = userViewModel,
                        branchViewModel = branchViewModel,
                        cashRegisterViewModel = cashRegisterViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    loginViewModel: LoginViewModel,
    authSessionViewModel: AuthSessionViewModel,
    roleViewModel: RoleViewModel,
    userViewModel: UserViewModel,
    branchViewModel: BranchViewModel,
    cashRegisterViewModel: CashRegisterViewModel
) {
    val requireBiometricState = AppLifecycleObserver.requireBiometric.collectAsState()
    val requireBiometric = requireBiometricState.value

    LaunchedEffect(requireBiometric) {
        if (requireBiometric) {
            val currentRoute = navController.currentDestination?.route ?: "home"

            navController.navigate("biometric_auth/$currentRoute") {
                launchSingleTop = true
            }

            AppLifecycleObserver.reset()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // ===== AUTENTICACIÓN =====
        composable("splash") {
            Splash(navController, authSessionViewModel)
        }

        // ✅ Nueva pantalla de autenticación biométrica
        composable(
            route = "biometric_auth/{targetRoute}",
            arguments = listOf(navArgument("targetRoute") { type = NavType.StringType })
        ) { backStackEntry ->
            val targetRoute = backStackEntry.arguments?.getString("targetRoute") ?: "login"
            BiometricAuthScreen(
                navController = navController,
                targetRoute = targetRoute
            )
        }

        composable("login") {
            LoginScreen(navController, loginViewModel, authSessionViewModel)
        }

        composable("user_disabled") {
            UserDisabledScreen(navController, authSessionViewModel)
        }

        composable("register") {
            val registerViewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModelFactory(supabase, authSessionViewModel)
            )
            RegisterScreen(navController, registerViewModel, authSessionViewModel)
        }

        composable("home") {
            HomeScreen(navController, authSessionViewModel)
        }

        composable("profile") {
            ProfileScreen(navController, authSessionViewModel) }

        // ===== GESTIÓN DE ROLES =====
        composable("roles") {
            RolesListScreen(navController, roleViewModel)
        }

        composable("roles/create") {
            RoleFormScreen(navController, roleViewModel, null)
        }

        composable(
            "roles/edit/{roleId}",
            arguments = listOf(navArgument("roleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val roleId = backStackEntry.arguments?.getInt("roleId")
            roleId?.let {
                RoleFormScreen(navController, roleViewModel, it)
            }
        }

        composable(
            "roles/{roleId}/permissions",
            arguments = listOf(navArgument("roleId") { type = NavType.IntType })
        ) { backStackEntry ->
            val roleId = backStackEntry.arguments?.getInt("roleId")
            roleId?.let {
                RolePermissionsScreen(navController, roleViewModel, it)
            }
        }

        // ===== GESTIÓN DE USUARIOS =====
        composable("users") {
            UsersListScreen(
                navController,
                viewModel = userViewModel,
                onNavigateToCreate = { navController.navigate("users/create") },
                onNavigateToEdit = { userId ->
                    navController.navigate("users/edit/$userId")
                }
            )
        }

        composable("users/create") {
            UserFormScreen(
                viewModel = userViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "users/edit/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            userId?.let {
                UserFormScreen(
                    viewModel = userViewModel,
                    userId = it,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // ===== GESTIÓN DE SUCURSALES =====
        composable("branches") {
            BranchesScreen(
                viewModel = branchViewModel,
                sessionViewModel = authSessionViewModel,
                onNavigateToTab = { tab ->
                    when(tab) {
                        0 -> {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                        1 -> {
                            // Ya estamos en branches, no hacer nada
                        }
                        2 -> {
                            // TODO: Navegar a ajustes cuando esté implementado
                        }
                    }
                }
            )
        }

        // ===== GESTIÓN DE CAJAS =====
        composable("cash_register/manage") {
            CashRegisterManagementScreen(
                viewModel = cashRegisterViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("cash_register/history") {
            CashRegisterHistoryScreen(
                viewModel = cashRegisterViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }


//        // ===== CAJA REGISTRADORA =====
//        composable("cash_register") {
//            CashRegisterMainScreen(
//                viewModel = cashRegisterViewModel,
//                onNavigateToManagement = {
//                    navController.navigate("cash_register/manage")
//                }
//            )
//        }

        composable("cash_register/history") {
            CashRegisterHistoryScreen(
                viewModel = cashRegisterViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}