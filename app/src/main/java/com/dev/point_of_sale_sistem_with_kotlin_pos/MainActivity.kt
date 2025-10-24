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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.home.HomeScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.LoginScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.auth.RegisterScreen
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.screens.Splash
import com.dev.point_of_sale_sistem_with_kotlin_pos.ui.theme.AppTheme
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.LoginViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.RegisterViewModel
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.supabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.viewmodel.auth.AuthSessionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Create the AuthSessionViewModel first
                    val authSessionViewModel = remember { AuthSessionViewModel(supabase) }
                    // Pass authSessionViewModel to LoginViewModel and RegisterViewModel
                    val loginViewModel = remember { LoginViewModel(supabase, authSessionViewModel) }
                    val registerViewModel = remember { RegisterViewModel(supabase, authSessionViewModel) }

                    AppNavigation(navController, loginViewModel, registerViewModel, authSessionViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    authSessionViewModel: AuthSessionViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") { Splash(navController, authSessionViewModel) }
        composable("login") { LoginScreen(navController, loginViewModel, authSessionViewModel) }
        composable("home") { HomeScreen(navController, authSessionViewModel) }
        composable("register") { RegisterScreen(navController, registerViewModel, authSessionViewModel) }
    }
}