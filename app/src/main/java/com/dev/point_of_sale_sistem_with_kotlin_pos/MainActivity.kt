package com.dev.point_of_sale_sistem_with_kotlin_pos

import androidx.compose.ui.Modifier
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

// Suppliers
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.SupplierListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.SupplierFormScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.BranchRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.categories.CategoryRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.products.ProductsRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.suppliers.SupplierRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.security.AppLifecycleObserver
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.Splash
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.branches.BranchesScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.categories.CategoriesListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.categories.CategoryFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.ProductFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.products.ProductsListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles.RoleFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles.RolePermissionsScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.roles.RolesListScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.SupplierFormScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.admin.suppliers.SupplierListScreen
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
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.*
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
                    val productsRepository = remember { ProductsRepository(supabase) }
                    val supplierRepository = remember { SupplierRepository(supabase) }   // ← NUEVO

                    // VIEWMODELS
                    val roleViewModel = remember { RoleViewModel(roleRepository) }
                    val userViewModel = remember { UserViewModel(userRepository) }
                    val branchViewModel = remember { BranchViewModel(branchRepository) }
                    val categoryViewModel = remember { CategoryViewModel(categoryRepository) }
                    val productsViewModel = remember { ProductsViewModel(productsRepository) }

                    val supplierViewModel = remember {         // ← NUEVO
                        SupplierViewModel(supplierRepository)
                    }

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
                        categoryViewModel = categoryViewModel,
                        productsViewModel = productsViewModel,
                        supplierViewModel = supplierViewModel     // ← NUEVO
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
    categoryViewModel: CategoryViewModel,
    productsViewModel: ProductsViewModel,
    supplierViewModel: SupplierViewModel
) {

    val categoryState by categoryViewModel.state.collectAsState()
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

        // =========================
        //        AUTENTICACIÓN
        // =========================
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
            ProfileScreen(navController, authSessionViewModel)
        }

        // =========================
        //        ROLES
        // =========================
        composable("roles") {
            RolesListScreen(navController, roleViewModel)
        }

        composable("roles/create") {
            RoleFormScreen(navController, roleViewModel, null)
        }

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
            val roleId = it.arguments?.getInt("roleId")
            RolePermissionsScreen(navController, roleViewModel, roleId!!)
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
            UserFormScreen(
                viewModel = userViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            "users/edit/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) {
            val userId = it.arguments?.getString("userId")
            UserFormScreen(
                viewModel = userViewModel,
                userId = userId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // =========================
        //        SUCURSALES
        // =========================
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
                            navController.navigate("profile") {
                                popUpTo("profile") { inclusive = false }
                                launchSingleTop = true
                            }
                        }

                    }
                }
            )
        }

        // =========================
        //        CATEGORÍAS
        // =========================
        composable("categories") {
            CategoriesListScreen(
                viewModel = categoryViewModel,
                onNavigateToCreate = { navController.navigate("categories/create") },
                onNavigateToEdit = { id -> navController.navigate("categories/edit/$id") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("categories/create") {
            CategoryFormScreen(
                viewModel = categoryViewModel,
                categoryId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            "categories/edit/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.IntType })
        ) {
            val id = it.arguments?.getInt("categoryId")
            CategoryFormScreen(
                viewModel = categoryViewModel,
                categoryId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // =========================
        //        PRODUCTOS
        // =========================
        composable("products") {
            ProductsListScreen(
                viewModel = productsViewModel,
                onNavigateToCreate = { navController.navigate("products/create") },
                onNavigateToEdit = { id -> navController.navigate("products/edit/$id") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("products/create") {
            ProductFormScreen(
                viewModel = productsViewModel,
                productId = null,
                categories = categoryState.categories,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            "products/edit/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("productId")
            ProductFormScreen(
                viewModel = productsViewModel,
                productId = id!!,
                categories = categoryState.categories,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // =========================
        //        SUPPLIERS (NUEVO)
        // =========================
        composable("suppliers") {
            SupplierListScreen(
                viewModel = supplierViewModel,
                onNavigateToCreate = { navController.navigate("suppliers/create") },
                onNavigateToEdit = { id -> navController.navigate("suppliers/edit/$id") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("suppliers/create") {
            SupplierFormScreen(
                viewModel = supplierViewModel,
                supplierId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            "suppliers/edit/{supplierId}",
            arguments = listOf(navArgument("supplierId") { type = NavType.IntType })
        ) {
            val id = it.arguments?.getInt("supplierId")
            SupplierFormScreen(
                viewModel = supplierViewModel,
                supplierId = id,
                onNavigateBack = { navController.popBackStack() }
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
