package com.dev.point_of_sale_sistem_with_kotlin_pos

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.biometric.BiometricManager
import kotlinx.coroutines.launch

// Screens
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.Splash
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.BranchesScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.categories.CategoriesListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.categories.CategoryFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.ProductsListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.ProductFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles.RolesListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles.RoleFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles.RolePermissionsScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.users.UsersListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.users.UserFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.SupplierListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.SupplierFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.BiometricAuthScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.LoginScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.RegisterScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.components.UserDisabledScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home.HomeScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home.profile.ProfileScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.CashRegisterManagementScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.CashRegisterHistoryScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.SalesScreen

// Theme
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.theme.AppTheme

// Repositories
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.BranchRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.categories.CategoryRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.products.ProductRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.suppliers.SupplierRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesProductRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase

// ViewModels
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.cash_register.CashRegisterViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.sales_orders.SalesViewModel

// Security
import com.dev.point_of_sale_sistem_with_kotlin_pos.security.AppLifecycleObserver
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.ProductViewModel

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

                    // SESSION PREFS
                    val sessionPreferences = remember {
                        (application as MyApplication).sessionPreferences
                    }

                    // AUTH
                    val authSessionViewModel = remember {
                        AuthSessionViewModel(supabase, sessionPreferences)
                    }

                    val loginViewModel = remember {
                        LoginViewModel(supabase, authSessionViewModel)
                    }

                    // REPOSITORIES
                    val roleRepository = remember { RoleRepository(supabase) }
                    val userRepository = remember { UserRepository(supabase) }
                    val branchRepository = remember { BranchRepository(supabase) }
                    val categoryRepository = remember { CategoryRepository(supabase) }
                    val context = LocalContext.current
                    val productsRepository = remember { ProductRepository(supabase, context) }
                    val supplierRepository = remember { SupplierRepository(supabase) }
                    val cashRegisterRepository = remember { CashRegisterRepository(supabase) }
                    val salesRepository = remember { SalesProductRepository(supabase) }

                    // VIEWMODELS
                    val roleViewModel = remember { RoleViewModel(roleRepository) }
                    val userViewModel = remember { UserViewModel(userRepository) }
                    val branchViewModel = remember { BranchViewModel(branchRepository) }
                    val categoryViewModel = remember { CategoryViewModel(categoryRepository) }
                    val productsViewModel = remember { ProductViewModel(productsRepository) }
                    val supplierViewModel = remember { SupplierViewModel(supplierRepository) }
                    val cashRegisterViewModel = remember { CashRegisterViewModel(cashRegisterRepository) }
                    val salesViewModel = remember { SalesViewModel(salesRepository) }

                    AppNavigation(
                        navController = navController,
                        loginViewModel = loginViewModel,
                        authSessionViewModel = authSessionViewModel,
                        roleViewModel = roleViewModel,
                        userViewModel = userViewModel,
                        branchViewModel = branchViewModel,
                        categoryViewModel = categoryViewModel,
                        productsViewModel = productsViewModel,
                        supplierViewModel = supplierViewModel,
                        cashRegisterViewModel = cashRegisterViewModel,
                        salesViewModel = salesViewModel,
                        sessionPreferences = sessionPreferences
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
    categoryViewModel: CategoryViewModel,
    productsViewModel: ProductViewModel,
    supplierViewModel: SupplierViewModel,
    cashRegisterViewModel: CashRegisterViewModel,
    salesViewModel: SalesViewModel,
    sessionPreferences: com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
) {
    val context = LocalContext.current
    val categoryState by categoryViewModel.state.collectAsState()
    val requireBiometric by AppLifecycleObserver.requireBiometric.collectAsState()
    val scope = rememberCoroutineScope()

    // === CONTROL DEFINITIVO DE BIOMETRÍA AL VOLVER DEL BACKGROUND ===
    LaunchedEffect(requireBiometric) {
        if (!requireBiometric) return@LaunchedEffect

        scope.launch {
            val biometricEnabled = sessionPreferences.isBiometricEnabled()
            if (!biometricEnabled) {
                AppLifecycleObserver.reset()
                return@launch
            }

            val biometricManager = BiometricManager.from(context)
            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )

            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                val currentRoute = navController.currentDestination?.route ?: "home"
                navController.navigate("biometric_auth/$currentRoute") {
                    launchSingleTop = true
                    popUpTo(currentRoute) { inclusive = false }
                }
            }

            AppLifecycleObserver.reset()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") { Splash(navController, authSessionViewModel) }

        composable(
            route = "biometric_auth/{targetRoute}",
            arguments = listOf(navArgument("targetRoute") { type = NavType.StringType })
        ) { backStackEntry ->
            val targetRoute = backStackEntry.arguments?.getString("targetRoute") ?: "home"
            BiometricAuthScreen(navController = navController, targetRoute = targetRoute)
        }

        composable("login") { LoginScreen(navController, loginViewModel, authSessionViewModel) }
        composable("user_disabled") { UserDisabledScreen(navController, authSessionViewModel) }
        composable("register") {
            val registerViewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModelFactory(supabase, authSessionViewModel)
            )
            RegisterScreen(navController, registerViewModel, authSessionViewModel)
        }

        composable("home") { HomeScreen(navController, authSessionViewModel) }
        composable("profile") { ProfileScreen(navController, authSessionViewModel) }

        // =========================
        //        ROLES
        // =========================
        composable("roles") { RolesListScreen(navController, roleViewModel) }
        composable("roles/create") { RoleFormScreen(navController, roleViewModel, null) }
        composable(
            "roles/edit/{roleId}",
            arguments = listOf(navArgument("roleId") { type = NavType.IntType })
        ) {
            val roleId = it.arguments?.getInt("roleId")
            RoleFormScreen(navController, roleViewModel, roleId)
        }
        composable(
            "roles/{roleId}/permissions",
            arguments = listOf(navArgument("roleId") { type = NavType.IntType })
        ) {
            val roleId = it.arguments?.getInt("roleId")!!
            RolePermissionsScreen(navController, roleViewModel, roleId)
        }

        // =========================
        //        USUARIOS
        // =========================
        composable("users") {
            UsersListScreen(
                navController,
                userViewModel,
                onNavigateToCreate = { navController.navigate("users/create") },
                onNavigateToEdit = { id -> navController.navigate("users/edit/$id") }
            )
        }

        composable("users/create") {
            UserFormScreen(userViewModel) { navController.popBackStack() }
        }

        composable(
            "users/edit/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            val userId = it.arguments?.getString("userId")
            UserFormScreen(userViewModel, userId) { navController.popBackStack() }
        }

        // =========================
        //        SUCURSALES
        // =========================
        composable("branches") {
            BranchesScreen(branchViewModel, authSessionViewModel) { tab ->
                when (tab) {
                    0 -> navController.navigate("home") { popUpTo("home") { inclusive = false }; launchSingleTop = true }
                    1 -> {} // Ya estamos en branches
                    2 -> navController.navigate("profile") { popUpTo("profile") { inclusive = false }; launchSingleTop = true }
                }
            }
        }

        // =========================
        //        CATEGORÍAS
        // =========================
        composable("categories") {
            CategoriesListScreen(categoryViewModel,
                onNavigateToCreate = { navController.navigate("categories/create") },
                onNavigateToEdit = { id -> navController.navigate("categories/edit/$id") },
                onNavigateBack = { navController.popBackStack() })
        }

        composable("categories/create") {
            CategoryFormScreen(categoryViewModel, null) { navController.popBackStack() }
        }

        composable(
            "categories/edit/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.IntType })
        ) {
            val id = it.arguments?.getInt("categoryId")
            CategoryFormScreen(categoryViewModel, id) { navController.popBackStack() }
        }

        // =========================
        //        PRODUCTOS
        // =========================
        composable("products") {
            ProductsListScreen(productsViewModel,
                onNavigateToCreate = { navController.navigate("products/create") },
                onNavigateToEdit = { id -> navController.navigate("products/edit/$id") },
                onNavigateBack = { navController.popBackStack() })
        }

        composable("products/create") {
            ProductFormScreen(productsViewModel, null, categoryState.categories) { navController.popBackStack() }
        }

        composable(
            "products/edit/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) {
            val id = it.arguments?.getInt("productId")
            ProductFormScreen(productsViewModel, id!!, categoryState.categories) { navController.popBackStack() }
        }

        // =========================
        //        SUPPLIERS
        // =========================
        composable("suppliers") {
            SupplierListScreen(supplierViewModel,
                onNavigateToCreate = { navController.navigate("suppliers/create") },
                onNavigateToEdit = { id -> navController.navigate("suppliers/edit/$id") },
                onNavigateBack = { navController.popBackStack() })
        }

        composable("suppliers/create") {
            SupplierFormScreen(supplierViewModel, null) { navController.popBackStack() }
        }

        composable(
            "suppliers/edit/{supplierId}",
            arguments = listOf(navArgument("supplierId") { type = NavType.IntType })
        ) {
            val id = it.arguments?.getInt("supplierId")
            SupplierFormScreen(supplierViewModel, id) { navController.popBackStack() }
        }

        // =========================
        //        CAJAS
        // =========================
        composable("cash_register/manage") {
            CashRegisterManagementScreen(cashRegisterViewModel) { navController.popBackStack() }
        }
        composable("cash_register/history") {
            CashRegisterHistoryScreen(cashRegisterViewModel) { navController.popBackStack() }
        }

        // =========================
        //        VENTAS
        // =========================
        composable("sales") {
            SalesScreen(
                viewModel = salesViewModel,
                onNavigateBack = { navController.popBackStack() },
                authViewModel = authSessionViewModel
            )
        }
    }
}