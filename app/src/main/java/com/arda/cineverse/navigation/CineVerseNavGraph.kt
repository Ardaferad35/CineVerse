package com.arda.cineverse.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arda.cineverse.ui.screens.ForgotPasswordScreen
import com.arda.cineverse.ui.screens.LoginScreen
import com.arda.cineverse.ui.screens.RegisterScreen

object CVRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home" // placeholder destination after successful auth
}

@Composable
fun CineVerseNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = CVRoutes.LOGIN) {
        composable(CVRoutes.LOGIN) {
            LoginScreen(
                onBack = { /* no-op: entry screen */ },
                onNavigateToRegister = { navController.navigate(CVRoutes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(CVRoutes.FORGOT_PASSWORD) },
                onLoginSuccess = { navController.navigate(CVRoutes.HOME) },
            )
        }
        composable(CVRoutes.REGISTER) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = { navController.navigate(CVRoutes.HOME) },
            )
        }
        composable(CVRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(CVRoutes.LOGIN) {
                        popUpTo(CVRoutes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        // TODO: composable(CVRoutes.HOME) { HomeScreen() }
    }
}
