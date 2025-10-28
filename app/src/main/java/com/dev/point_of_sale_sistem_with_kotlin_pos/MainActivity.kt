package com.dev.point_of_sale_sistem_with_kotlin_pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home.HomeScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.LoginScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.RegisterScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.Splash
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.LoginViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModelFactory
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme { // Use AppTheme from com.example.compose
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Create the AuthSessionViewModel first
                    val authSessionViewModel = remember { AuthSessionViewModel(supabase) }
                    // Pass authSessionViewModel to LoginViewModel
                    val loginViewModel = remember { LoginViewModel(supabase, authSessionViewModel) }

                    AppNavigation(navController, loginViewModel, authSessionViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    loginViewModel: LoginViewModel,
    authSessionViewModel: AuthSessionViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") { Splash(navController, authSessionViewModel) }
        composable("login") { LoginScreen(navController, loginViewModel, authSessionViewModel) }
        composable("home") { HomeScreen(navController, authSessionViewModel) }
        composable("register") {
            // Use viewModel to create RegisterViewModel with SavedStateHandle
            val registerViewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModelFactory(supabase, authSessionViewModel)
            )
            RegisterScreen(navController, registerViewModel, authSessionViewModel)
        }
    }
}