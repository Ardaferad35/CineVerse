package com.arda.cineverse.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arda.cineverse.ui.screens.ForgotPasswordScreen
import com.arda.cineverse.ui.screens.HomeScreen
import com.arda.cineverse.ui.screens.LoginScreen
import com.arda.cineverse.ui.screens.MovieDetailScreen
import com.arda.cineverse.ui.screens.MyListScreen
import com.arda.cineverse.ui.screens.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

object CVRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val LISTEM = "my_list"
    const val MOVIE_DETAIL = "movie_detail/{movieId}"

    fun movieDetail(movieId: Int) = "movie_detail/$movieId"
}

@Composable
fun CineVerseNavGraph(navController: NavHostController = rememberNavController()) {
    val startDestination = remember {
        if (FirebaseAuth.getInstance().currentUser != null) CVRoutes.HOME else CVRoutes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(CVRoutes.LOGIN) {
            LoginScreen(
                onBack = { /* no-op: entry screen */ },
                onNavigateToRegister = { navController.navigate(CVRoutes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(CVRoutes.FORGOT_PASSWORD) },
                onLoginSuccess = {
                    navController.navigate(CVRoutes.HOME) {
                        popUpTo(CVRoutes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(CVRoutes.REGISTER) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(CVRoutes.HOME) {
                        popUpTo(CVRoutes.LOGIN) { inclusive = true }
                    }
                },
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
        composable(CVRoutes.HOME) {
            HomeScreen(
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                onSeeAllClick = { /* ilgili liste ekranına yönlendirilecek */ },
                onAiSearchClick = { /* AI arama ekranına yönlendirilecek */ },
                onNavigateTab = { index ->
                    if (index == 2) {
                        navController.navigate(CVRoutes.LISTEM) { launchSingleTop = true }
                    }
                    // 1 (Arama) ve 3 (Profil) ekranları henüz hazır değil
                },
            )
        }
        composable(CVRoutes.LISTEM) {
            MyListScreen(
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                onStartExploring = { navController.popBackStack(CVRoutes.HOME, inclusive = false) },
                onNavigateTab = { index ->
                    if (index == 0) {
                        navController.popBackStack(CVRoutes.HOME, inclusive = false)
                    }
                    // 1 (Arama) ve 3 (Profil) ekranları henüz hazır değil
                },
            )
        }
        composable(
            route = CVRoutes.MOVIE_DETAIL,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("movieId") ?: return@composable
            MovieDetailScreen(
                movieId = movieId,
                onBack = { navController.popBackStack() },
                onGoHome = { navController.popBackStack(CVRoutes.HOME, inclusive = false) },
                onMovieClick = { relatedMovieId -> navController.navigate(CVRoutes.movieDetail(relatedMovieId)) },
            )
        }
    }
}