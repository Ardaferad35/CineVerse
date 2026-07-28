package com.arda.cineverse.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arda.cineverse.ui.screens.AllCategoriesScreen
import com.arda.cineverse.ui.screens.ForgotPasswordScreen
import com.arda.cineverse.ui.screens.HomeScreen
import com.arda.cineverse.ui.screens.LoginScreen
import com.arda.cineverse.ui.screens.MovieDetailScreen
import com.arda.cineverse.ui.screens.MovieListScreen
import com.arda.cineverse.ui.screens.MovieListSource
import com.arda.cineverse.ui.screens.MyListScreen
import com.arda.cineverse.ui.screens.ProfileScreen
import com.arda.cineverse.ui.screens.RegisterScreen
import com.arda.cineverse.ui.screens.SearchScreen
import com.google.firebase.auth.FirebaseAuth
import java.net.URLDecoder
import java.net.URLEncoder

object CVRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val LISTEM = "my_list"
    const val SEARCH = "search/{aiMode}"
    const val PROFILE = "profile"
    const val MOVIE_DETAIL = "movie_detail/{movieId}"
    const val MOVIE_LIST = "movie_list/{section}"
    const val MOVIE_LIST_GENRE = "movie_list_genre/{genreId}/{label}"
    const val ALL_CATEGORIES = "all_categories"

    fun movieDetail(movieId: Int) = "movie_detail/$movieId"
    fun search(aiMode: Boolean) = "search/$aiMode"
    fun movieList(section: String) = "movie_list/$section"
    fun movieListGenre(genreId: Int, label: String) =
        "movie_list_genre/$genreId/${URLEncoder.encode(label, "UTF-8")}"
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
                onSeeAllClick = { section ->
                    when (section) {
                        "popular", "upcoming" -> navController.navigate(CVRoutes.movieList(section))
                        "categories" -> navController.navigate(CVRoutes.ALL_CATEGORIES)
                    }
                },
                onAiSearchClick = { navController.navigate(CVRoutes.search(aiMode = true)) },
                onNavigateTab = { index ->
                    when (index) {
                        1 -> navController.navigate(CVRoutes.search(aiMode = false))
                        2 -> navController.navigate(CVRoutes.LISTEM) { launchSingleTop = true }
                    }
                },
                onCategoryClick = { category ->
                    navController.navigate(CVRoutes.movieListGenre(category.genreId, category.label))
                },
                onProfileClick = { navController.navigate(CVRoutes.PROFILE) },
            )
        }
        composable(CVRoutes.LISTEM) {
            MyListScreen(
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                onStartExploring = { navController.popBackStack(CVRoutes.HOME, inclusive = false) },
                onNavigateTab = { index ->
                    when (index) {
                        0 -> navController.popBackStack(CVRoutes.HOME, inclusive = false)
                        1 -> navController.navigate(CVRoutes.search(aiMode = false))
                    }
                },
                onProfileClick = { navController.navigate(CVRoutes.PROFILE) },
            )
        }
        composable(
            route = CVRoutes.SEARCH,
            arguments = listOf(navArgument("aiMode") { type = NavType.BoolType }),
        ) { backStackEntry ->
            val aiMode = backStackEntry.arguments?.getBoolean("aiMode") ?: false
            SearchScreen(
                startInAiMode = aiMode,
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                onNavigateTab = { index ->
                    if (index == 0) {
                        navController.popBackStack(CVRoutes.HOME, inclusive = false)
                    } else if (index == 2) {
                        navController.navigate(CVRoutes.LISTEM) { launchSingleTop = true }
                    }
                },
                onProfileClick = { navController.navigate(CVRoutes.PROFILE) },
            )
        }
        composable(CVRoutes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.navigate(CVRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = CVRoutes.MOVIE_LIST,
            arguments = listOf(navArgument("section") { type = NavType.StringType }),
        ) { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section") ?: "popular"
            MovieListScreen(
                source = if (section == "upcoming") MovieListSource.Upcoming else MovieListSource.Popular,
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
            )
        }
        composable(
            route = CVRoutes.MOVIE_LIST_GENRE,
            arguments = listOf(
                navArgument("genreId") { type = NavType.IntType },
                navArgument("label") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val genreId = backStackEntry.arguments?.getInt("genreId") ?: 0
            val rawLabel = backStackEntry.arguments?.getString("label") ?: "Kategori"
            val label = URLDecoder.decode(rawLabel, "UTF-8")
            MovieListScreen(
                source = MovieListSource.Genre(genreId = genreId, label = label),
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
            )
        }
        composable(CVRoutes.ALL_CATEGORIES) {
            AllCategoriesScreen(
                onBack = { navController.popBackStack() },
                onCategoryClick = { category ->
                    navController.navigate(CVRoutes.movieListGenre(category.genreId, category.label))
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