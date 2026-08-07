package com.arda.cineverse.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arda.cineverse.ui.screens.AiChatScreen
import com.arda.cineverse.ui.screens.AllCategoriesScreen
import com.arda.cineverse.ui.screens.ForgotPasswordScreen
import com.arda.cineverse.ui.screens.FriendsScreen
import com.arda.cineverse.ui.screens.HomeScreen
import com.arda.cineverse.ui.screens.LoginScreen
import com.arda.cineverse.ui.screens.MovieDetailScreen
import com.arda.cineverse.ui.screens.MovieListScreen
import com.arda.cineverse.ui.screens.MovieListSource
import com.arda.cineverse.ui.screens.MyListScreen
import com.arda.cineverse.ui.screens.NotificationsScreen
import com.arda.cineverse.ui.screens.ProfileScreen
import com.arda.cineverse.ui.screens.ReelsScreen
import com.arda.cineverse.ui.screens.RegisterScreen
import com.arda.cineverse.ui.screens.TvShowDetailScreen
import com.arda.cineverse.viewmodel.AiChatViewModel
import com.arda.cineverse.viewmodel.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth
import java.net.URLDecoder
import java.net.URLEncoder

object CVRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val LISTEM = "my_list"
    const val AI_CHAT = "ai_chat?isTvMode={isTvMode}"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val FRIENDS = "friends"
    const val MOVIE_DETAIL = "movie_detail/{movieId}"
    const val TV_DETAIL = "tv_detail/{tvId}"
    const val MOVIE_LIST = "movie_list/{section}"
    const val MOVIE_LIST_GENRE = "movie_list_genre/{genreId}/{label}"
    const val TV_LIST_GENRE = "tv_list_genre/{genreId}/{label}"
    const val ALL_CATEGORIES = "all_categories?isTvMode={isTvMode}"
    const val REELS = "reels"

    fun movieDetail(movieId: Int) = "movie_detail/$movieId"
    fun tvDetail(tvId: Int) = "tv_detail/$tvId"

    /**
     * [isTvMode] null bırakıldığında asistan son kaldığı modda açılıyor — Listem
     * sekmesi gibi film/dizi ayrımı olmayan ekranlardan gelirken kullanılıyor.
     */
    fun aiChat(isTvMode: Boolean? = null) =
        if (isTvMode == null) "ai_chat" else "ai_chat?isTvMode=$isTvMode"

    fun movieList(section: String) = "movie_list/$section"
    fun movieListGenre(genreId: Int, label: String) =
        "movie_list_genre/$genreId/${URLEncoder.encode(label, "UTF-8")}"
    fun tvListGenre(genreId: Int, label: String) =
        "tv_list_genre/$genreId/${URLEncoder.encode(label, "UTF-8")}"
    fun allCategories(isTvMode: Boolean = false) = "all_categories?isTvMode=$isTvMode"
}

@Composable
fun CineVerseNavGraph(
    navController: NavHostController = rememberNavController(),
    pendingDeepLinkRoute: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val startDestination = remember {
        if (FirebaseAuth.getInstance().currentUser != null) CVRoutes.HOME else CVRoutes.LOGIN
    }

    // HomeScreen (rozet) ve NotificationsScreen (liste + "hepsini okundu say")
    // KASITLI OLARAK aynı örneği paylaşıyor. Her ikisi de kendi varsayılan
    // viewModel()'ini kullansaydı, her biri nav-backstack kaydına bağlı AYRI
    // bir örnek olurdu — Bildirimler ekranında hepsini okundu işaretlemek,
    // Home'daki tamamen farklı objeye ait rozeti hiç etkilemezdi (rozet eski
    // değerde donup kalırdı, tam da bu bug).
    val notificationViewModel: NotificationViewModel = hiltViewModel()

    // Sohbet geçmişi oturum boyunca korunsun diye ViewModel burada, NavHost'un
    // dışında oluşturuluyor: composable(AI_CHAT) { } içinde yaratılsaydı örnek
    // o backstack kaydına bağlı olurdu ve sekmeden çıkıp dönen kullanıcı her
    // seferinde bomboş bir sohbete düşerdi.
    val aiChatViewModel: AiChatViewModel = hiltViewModel()

    // Bildirime dokunularak açılışta (soğuk başlangıç) veya uygulama zaten
    // açıkken (onNewIntent) tetiklenir. Giriş yapılmamışsa hedefe gitmenin
    // anlamı yok — kullanıcı normal şekilde Login ekranında kalır.
    LaunchedEffect(pendingDeepLinkRoute) {
        if (pendingDeepLinkRoute != null && FirebaseAuth.getInstance().currentUser != null) {
            navController.navigate(pendingDeepLinkRoute)
        }
        onDeepLinkHandled()
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
                notificationViewModel = notificationViewModel,
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                onTvShowClick = { tvId -> navController.navigate(CVRoutes.tvDetail(tvId)) },
                onSeeAllClick = { section ->
                    when (section) {
                        "popular", "upcoming" -> navController.navigate(CVRoutes.movieList(section))
                        "popular_tv" -> navController.navigate(CVRoutes.movieList("popular_tv"))
                        "on_air_tv" -> navController.navigate(CVRoutes.movieList("on_air_tv"))
                        "upcoming_tv" -> navController.navigate(CVRoutes.movieList("upcoming_tv"))
                        "categories_tv" -> navController.navigate(CVRoutes.allCategories(isTvMode = true))
                        "categories_movie", "categories" -> navController.navigate(CVRoutes.allCategories(isTvMode = false))
                    }
                },
                onAiSearchClick = { isTvMode ->
                    navController.navigate(CVRoutes.aiChat(isTvMode)) { launchSingleTop = true }
                },
                onNavigateTab = { index ->
                    if (index == 2) navController.navigate(CVRoutes.LISTEM) { launchSingleTop = true }
                },
                onCategoryClick = { category ->
                    navController.navigate(CVRoutes.movieListGenre(category.genreId, category.label))
                },
                onTvCategoryClick = { category ->
                    navController.navigate(CVRoutes.tvListGenre(category.genreId, category.label))
                },
                onProfileClick = { navController.navigate(CVRoutes.PROFILE) },
                onNotificationsClick = { navController.navigate(CVRoutes.NOTIFICATIONS) },
                onReelsClick = { navController.navigate(CVRoutes.REELS) },
            )
        }
        composable(CVRoutes.LISTEM) {
            MyListScreen(
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                onTvShowClick = { tvId -> navController.navigate(CVRoutes.tvDetail(tvId)) },
                onStartExploring = { navController.popBackStack(CVRoutes.HOME, inclusive = false) },
                onNavigateTab = { index ->
                    when (index) {
                        0 -> navController.popBackStack(CVRoutes.HOME, inclusive = false)
                        1 -> navController.navigate(CVRoutes.aiChat()) { launchSingleTop = true }
                    }
                },
                onProfileClick = { navController.navigate(CVRoutes.PROFILE) },
                onNotificationsClick = { navController.navigate(CVRoutes.NOTIFICATIONS) },
            )
        }
        composable(
            route = CVRoutes.AI_CHAT,
            arguments = listOf(navArgument("isTvMode") { type = NavType.StringType; defaultValue = "" }),
        ) { backStackEntry ->
            AiChatScreen(
                viewModel = aiChatViewModel,
                startInTvMode = backStackEntry.arguments?.getString("isTvMode")?.toBooleanStrictOrNull(),
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                onTvShowClick = { tvId -> navController.navigate(CVRoutes.tvDetail(tvId)) },
                onNavigateTab = { index ->
                    if (index == 0) {
                        navController.popBackStack(CVRoutes.HOME, inclusive = false)
                    } else if (index == 2) {
                        navController.navigate(CVRoutes.LISTEM) { launchSingleTop = true }
                    }
                },
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
                onNavigateToFriends = { navController.navigate(CVRoutes.FRIENDS) },
            )
        }
        composable(CVRoutes.NOTIFICATIONS) {
            NotificationsScreen(
                viewModel = notificationViewModel,
                onBack = { navController.popBackStack() },
                onNotificationClick = { notification ->
                    when {
                        notification.type == "friend_request" || notification.type == "friend_request_accepted" ->
                            navController.navigate(CVRoutes.FRIENDS)
                        notification.tvId != null || notification.mediaType == "tv" -> {
                            val id = notification.tvId ?: notification.movieId
                            id?.let { tvId -> navController.navigate(CVRoutes.tvDetail(tvId)) }
                        }
                        else -> notification.movieId?.let { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) }
                    }
                },
            )
        }
        composable(CVRoutes.FRIENDS) {
            FriendsScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                onTvShowClick = { tvId -> navController.navigate(CVRoutes.tvDetail(tvId)) },
            )
        }
        composable(
            route = CVRoutes.MOVIE_LIST,
            arguments = listOf(navArgument("section") { type = NavType.StringType }),
        ) { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section") ?: "popular"
            when (section) {
                "upcoming" -> MovieListScreen(
                    source = MovieListSource.Upcoming,
                    onBack = { navController.popBackStack() },
                    onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                )
                "popular_tv" -> MovieListScreen(
                    source = MovieListSource.TvPopular,
                    onBack = { navController.popBackStack() },
                    onTvShowClick = { tvId -> navController.navigate(CVRoutes.tvDetail(tvId)) },
                )
                "on_air_tv" -> MovieListScreen(
                    source = MovieListSource.TvOnAir,
                    onBack = { navController.popBackStack() },
                    onTvShowClick = { tvId -> navController.navigate(CVRoutes.tvDetail(tvId)) },
                )
                "upcoming_tv" -> MovieListScreen(
                    source = MovieListSource.TvUpcoming,
                    onBack = { navController.popBackStack() },
                    onTvShowClick = { tvId -> navController.navigate(CVRoutes.tvDetail(tvId)) },
                )
                else -> MovieListScreen(
                    source = MovieListSource.Popular,
                    onBack = { navController.popBackStack() },
                    onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                )
            }
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
        composable(
            route = CVRoutes.TV_LIST_GENRE,
            arguments = listOf(
                navArgument("genreId") { type = NavType.IntType },
                navArgument("label") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val genreId = backStackEntry.arguments?.getInt("genreId") ?: 0
            val rawLabel = backStackEntry.arguments?.getString("label") ?: "Kategori"
            val label = URLDecoder.decode(rawLabel, "UTF-8")
            MovieListScreen(
                source = MovieListSource.TvGenre(genreId = genreId, label = label),
                onBack = { navController.popBackStack() },
                onTvShowClick = { tvId -> navController.navigate(CVRoutes.tvDetail(tvId)) },
            )
        }
        composable(
            route = CVRoutes.ALL_CATEGORIES,
            arguments = listOf(navArgument("isTvMode") { type = NavType.BoolType; defaultValue = false }),
        ) { backStackEntry ->
            val isTvMode = backStackEntry.arguments?.getBoolean("isTvMode") ?: false
            AllCategoriesScreen(
                isTvMode = isTvMode,
                onBack = { navController.popBackStack() },
                onCategoryClick = { category ->
                    if (isTvMode) {
                        navController.navigate(CVRoutes.tvListGenre(category.genreId, category.label))
                    } else {
                        navController.navigate(CVRoutes.movieListGenre(category.genreId, category.label))
                    }
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
        composable(
            route = CVRoutes.TV_DETAIL,
            arguments = listOf(navArgument("tvId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val tvId = backStackEntry.arguments?.getInt("tvId") ?: return@composable
            TvShowDetailScreen(
                tvId = tvId,
                onBack = { navController.popBackStack() },
                onGoHome = { navController.popBackStack(CVRoutes.HOME, inclusive = false) },
                onTvShowClick = { relatedTvId -> navController.navigate(CVRoutes.tvDetail(relatedTvId)) },
            )
        }
        composable(route = CVRoutes.REELS) {
            ReelsScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId -> navController.navigate(CVRoutes.movieDetail(movieId)) },
                onTvShowClick = { tvId -> navController.navigate(CVRoutes.tvDetail(tvId)) },
            )
        }
    }
}
