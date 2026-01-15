package com.dev.point_of_sale_sistem_with_kotlin_pos

// Screens

// Theme

// Repositories

// ViewModels

// Security
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.users.BusinessInfo
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.BranchRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.categories.CategoryRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.products.ProductRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.roles.RoleRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.suppliers.SupplierRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.users.UserRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.credit_notes.CreditNoteRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.CreditNoteInvoiceRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.invoice.InvoiceRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.refunds.RefundRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.PaymentProofRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesProductRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.StripePaymentRepository
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
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home.profile.ProfileScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.refunds.RefundScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.CashRegisterHistoryScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.cash_register.CashRegisterManagementScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.sales.sales_orders.SalesScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.theme.AppTheme
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.BranchViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.CategoryViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.ProductViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.RoleViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.SupplierViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.admin.UserViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.LoginViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModelFactory
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.credit_notes.CreditNoteUsageViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.refunds.RefundViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.cash_register.CashRegisterViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.sales.sales_orders.SalesViewModel
import kotlinx.coroutines.launch
import java.util.UUID


class MainActivity : FragmentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLifecycleObserver.start()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {

                    val context = LocalContext.current
                    val activity = this@MainActivity
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
                    val productsRepository = remember { ProductRepository(supabase, sessionPreferences) }
                    val supplierRepository = remember { SupplierRepository(supabase) }
                    val cashRegisterRepository = remember { CashRegisterRepository(supabase) }
                    val salesRepository = remember { SalesRepository(supabase) }

                    val salesProductRepository = remember { SalesProductRepository(supabase, sessionPreferences) }
                    val paymentProofRepository = remember { PaymentProofRepository(supabase) }
                    val refundRepository = remember { RefundRepository(supabase) }
                    val creditNoteRepository = remember { CreditNoteRepository(supabase) }

                    val stripePaymentRepository = remember {
                        StripePaymentRepository(
                            context = context,
                            publishableKey = BuildConfig.STRIPE_PUBLISHABLE_KEY,
                            supabase = supabase
                        )
                    }

                    // 🔥 BUSINESS INFO STATE
                    var businessInfo by remember { mutableStateOf<BusinessInfo?>(null) }

                    LaunchedEffect(Unit) {
                        businessInfo = branchRepository.getBusinessInfo()
                    }

                    // 🔥 INVOICE REPOSITORY (solo si hay BusinessInfo)
                    val invoiceRepository: InvoiceRepository? = businessInfo?.let { info ->
                        remember(info) {
                            InvoiceRepository(
                                context = context,
                                businessInfo = info,
                                activity = activity
                            )
                        }
                    }

                    val creditNoteInvoiceRepository: CreditNoteInvoiceRepository? = businessInfo?.let { info ->
                        remember(info) {
                            CreditNoteInvoiceRepository(
                                context = context,
                                businessInfo = info,
                                activity = activity
                            )
                        }
                    }


                    // VIEWMODELS
                    val roleViewModel = remember { RoleViewModel(roleRepository,sessionPreferences) }
                    val userViewModel = remember { UserViewModel(userRepository) }
                    val branchViewModel = remember { BranchViewModel(branchRepository) }
                    val categoryViewModel = remember { CategoryViewModel(categoryRepository) }
                    val productsViewModel = remember { ProductViewModel(productsRepository) }
                    val supplierViewModel = remember { SupplierViewModel(supplierRepository) }
                    val cashRegisterViewModel = remember { CashRegisterViewModel(cashRegisterRepository) }
                    val creditNoteUsageViewModel = remember { CreditNoteUsageViewModel(creditNoteRepository) }

                    // 🔥 REFUND VIEWMODEL (necesita userId del session)
                    val authState by authSessionViewModel.state.collectAsState()
                    val refundViewModel = remember(authState.userId, creditNoteInvoiceRepository) {
                        val userId = authState.userId
                        val creditRepo = creditNoteInvoiceRepository

                        if (userId != null && creditRepo != null) {
                            RefundViewModel(
                                repository = refundRepository,
                                creditNoteInvoiceRepository = creditRepo,
                                userId = UUID.fromString(userId)
                            )
                        } else {
                            null
                        }
                    }


                    // 🔥 SALES VIEWMODEL (solo si hay InvoiceRepository)
                    val salesViewModel: SalesViewModel? = invoiceRepository?.let { repo ->
                        remember(repo) {
                            SalesViewModel(
                                salesRepository,
                                salesProductRepository,
                                paymentProofRepository,
                                stripePaymentRepository,
                                repo,
                                creditNoteRepository,
                                cashRegisterRepository,
                                this
                            )
                        }
                    }

                    AppNavigation(
                        navController = navController,
                        loginViewModel = loginViewModel,
                        authSessionViewModel = authSessionViewModel,
                        creditNoteUsageViewModel = creditNoteUsageViewModel,
                        roleViewModel = roleViewModel,
                        userViewModel = userViewModel,
                        branchViewModel = branchViewModel,
                        categoryViewModel = categoryViewModel,
                        productsViewModel = productsViewModel,
                        supplierViewModel = supplierViewModel,
                        cashRegisterViewModel = cashRegisterViewModel,
                        salesViewModel = salesViewModel,
                        refundViewModel = refundViewModel,
                        sessionPreferences = sessionPreferences
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    navController: NavHostController,
    loginViewModel: LoginViewModel,
    authSessionViewModel: AuthSessionViewModel,
    creditNoteUsageViewModel: CreditNoteUsageViewModel,
    roleViewModel: RoleViewModel,
    userViewModel: UserViewModel,
    branchViewModel: BranchViewModel,
    categoryViewModel: CategoryViewModel,
    productsViewModel: ProductViewModel,
    supplierViewModel: SupplierViewModel,
    cashRegisterViewModel: CashRegisterViewModel,
    salesViewModel: SalesViewModel?,
    refundViewModel: RefundViewModel?,
    sessionPreferences: SessionPreferences
) {
    val context = LocalContext.current
    val categoryState by categoryViewModel.state.collectAsState()
    val branchState by branchViewModel.state.collectAsState()
    val requireBiometric by AppLifecycleObserver.requireBiometric.collectAsState()
    val scope = rememberCoroutineScope()

    // Cargar branches al iniciar
    LaunchedEffect(Unit) {
        branchViewModel.handleIntent(
            com.dev.point_of_sale_sistem_with_kotlin_pos.intents.admin.braches.BranchIntent.LoadBranches
        )
    }

    // Control de biometría al volver del background
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
        } else if (requireBiometric && !hasBiometric) {
            // Si no tiene biométrico, solo resetear sin hacer nada
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

        // ROLES
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

        // USUARIOS
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

        // SUCURSALES
        composable("branches") {
            BranchesScreen(branchViewModel, authSessionViewModel) { tab ->
                when (tab) {
                    0 -> navController.navigate("home") { popUpTo("home") { inclusive = false }; launchSingleTop = true }
                    1 -> {} // Ya estamos en branches
                    2 -> navController.navigate("profile") { popUpTo("profile") { inclusive = false }; launchSingleTop = true }
                }
            }
        }

        // CATEGORÍAS
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

        // PRODUCTOS
        composable("products") {
            ProductsListScreen(productsViewModel,
                onNavigateToCreate = { navController.navigate("products/create") },
                onNavigateToEdit = { id -> navController.navigate("products/edit/$id") },
                onNavigateBack = { navController.popBackStack() })
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
        ) {
            val id = it.arguments?.getInt("productId")
            ProductFormScreen(
                viewModel = productsViewModel,
                productId = id,
                categories = categoryState.categories,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SUPPLIERS
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

        // CAJAS
        composable("cash_register/manage") {
            CashRegisterManagementScreen(cashRegisterViewModel) { navController.popBackStack() }
        }
        composable("cash_register/history") {
            CashRegisterHistoryScreen(cashRegisterViewModel) { navController.popBackStack() }
        }

        // VENTAS
        composable("sales") {
            SalesScreen(
                viewModel = salesViewModel,
                onNavigateBack = { navController.popBackStack() },
                authViewModel = authSessionViewModel,
                creditNoteUsageViewModel = creditNoteUsageViewModel
            )
        }

        // ✅ DEVOLUCIONES
        composable("refunds") {
            // Solo mostrar si el usuario está autenticado y el viewModel está disponible
            refundViewModel?.let { viewModel ->
                RefundScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}