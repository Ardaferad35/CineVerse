package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arda.cineverse.data.model.AiChatMessage
import com.arda.cineverse.data.model.AiMovieSuggestion
import com.arda.cineverse.data.model.ChatErrorMessage
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.model.UserChatMessage
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.UserListRepository
import com.arda.cineverse.ui.components.AiChatBubble
import com.arda.cineverse.ui.components.AiChatEmptyState
import com.arda.cineverse.ui.components.AiChatHeader
import com.arda.cineverse.ui.components.AiChatInputBar
import com.arda.cineverse.ui.components.AiThinkingBubble
import com.arda.cineverse.ui.components.CVBottomNavBar
import com.arda.cineverse.ui.components.ChatErrorBubble
import com.arda.cineverse.ui.components.OfflineActionSnackbar
import com.arda.cineverse.ui.components.UserChatBubble
import com.arda.cineverse.ui.components.rememberOfflineWriteMessageState
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.viewmodel.AiChatViewModel
import kotlinx.coroutines.launch

private fun Movie.toSavedMovie() = SavedMovie(
    id = id,
    title = title,
    posterUrl = posterUrl,
    rating = rating,
    year = year,
    genreIds = genreIds,
    mediaType = mediaType,
)

@Composable
fun AiChatScreen(
    modifier: Modifier = Modifier,
    /** Ana sayfadan gelen Filmler/Diziler seçimi; null ise asistan son kaldığı modda kalır. */
    startInTvMode: Boolean? = null,
    onMovieClick: (movieId: Int) -> Unit = {},
    onTvShowClick: (tvId: Int) -> Unit = {},
    onNavigateTab: (Int) -> Unit = {},
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    val userListRepository = remember { UserListRepository.default() }
    val recommendationRepository = remember { RecommendationRepository.default() }

    var favoriteIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var watchlistIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val offlineMessageState = rememberOfflineWriteMessageState()

    LaunchedEffect(Unit) {
        favoriteIds = userListRepository.getFavorites().getOrDefault(emptyList()).map { it.id }.toSet()
        watchlistIds = userListRepository.getWatchlist().getOrDefault(emptyList()).map { it.id }.toSet()
    }

    LaunchedEffect(startInTvMode) {
        if (startInTvMode != null) viewModel.setTvMode(startInTvMode)
    }

    // Yeni mesaj geldiğinde ya da asistan düşünmeye başladığında sohbetin sonuna in.
    LaunchedEffect(uiState.messages.size, uiState.isThinking) {
        val lastIndex = uiState.messages.size // "yazıyor" balonu listenin son öğesi
        if (lastIndex > 0) listState.animateScrollToItem(lastIndex)
    }

    fun toggleFavorite(movie: Movie) {
        val isFav = movie.id in favoriteIds
        favoriteIds = if (isFav) favoriteIds - movie.id else favoriteIds + movie.id
        fun revert() {
            favoriteIds = if (isFav) favoriteIds + movie.id else favoriteIds - movie.id
        }
        scope.launch {
            val result = if (isFav) {
                userListRepository.removeFavorite(movie.id, mediaType = movie.mediaType)
            } else {
                userListRepository.addFavorite(movie.toSavedMovie())
            }
            result.fold(
                onSuccess = {
                    if (isFav) {
                        if (movie.mediaType == "tv") recommendationRepository.removeTvFavoriteSignal(movie.id) else recommendationRepository.removeFavoriteSignal(movie.id)
                    } else {
                        if (movie.mediaType == "tv") recommendationRepository.recordTvFavorite(movie.id, movie.genreIds) else recommendationRepository.recordFavorite(movie.id, movie.genreIds)
                    }
                },
                onFailure = { error -> offlineMessageState.handle(error) { revert() } },
            )
        }
    }

    fun toggleWatchlist(movie: Movie) {
        val isSaved = movie.id in watchlistIds
        watchlistIds = if (isSaved) watchlistIds - movie.id else watchlistIds + movie.id
        scope.launch {
            val result = if (isSaved) {
                userListRepository.removeFromWatchlist(movie.id, mediaType = movie.mediaType)
            } else {
                userListRepository.addToWatchlist(movie.toSavedMovie())
            }
            result.onFailure { error ->
                offlineMessageState.handle(error) {
                    watchlistIds = if (isSaved) watchlistIds + movie.id else watchlistIds - movie.id
                }
            }
        }
    }

    fun openSuggestion(suggestion: AiMovieSuggestion) {
        val movie = suggestion.movie
        if (movie.mediaType == "tv") onTvShowClick(movie.id) else onMovieClick(movie.id)
    }

    fun send(text: String) {
        keyboard?.hide()
        viewModel.send(text)
    }

    // Klavye açıkken alt navigasyon barı gizleniyor: aksi halde klavyenin
    // üstüne binip yazma alanını daraltıyor.
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    Box(modifier = modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Spacer(Modifier.height(12.dp))
            AiChatHeader(
                isTvMode = uiState.isTvMode,
                onClearClick = viewModel::clear,
                canClear = uiState.messages.isNotEmpty(),
            )
            Spacer(Modifier.height(12.dp))

            if (uiState.messages.isEmpty()) {
                AiChatEmptyState(
                    isTvMode = uiState.isTvMode,
                    onStarterClick = { starter -> send(starter) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        when (message) {
                            is UserChatMessage -> UserChatBubble(message.text)
                            is AiChatMessage -> AiChatBubble(
                                message = message,
                                favoriteIds = favoriteIds,
                                watchlistIds = watchlistIds,
                                onOpenClick = { openSuggestion(it) },
                                onFavoriteClick = { toggleFavorite(it.movie) },
                                onWatchlistClick = { toggleWatchlist(it.movie) },
                            )
                            is ChatErrorMessage -> ChatErrorBubble(
                                text = message.text,
                                onRetry = viewModel::retry,
                            )
                        }
                    }
                    if (uiState.isThinking) {
                        item(key = "thinking") { AiThinkingBubble() }
                    }
                }
            }

            AiChatInputBar(
                value = uiState.input,
                onValueChange = viewModel::onInputChange,
                onSend = { send(uiState.input) },
                isThinking = uiState.isThinking,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            if (isKeyboardOpen) {
                Spacer(Modifier.height(12.dp))
            } else {
                CVBottomNavBar(
                    selectedIndex = 1,
                    onItemSelected = onNavigateTab,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }

        offlineMessageState.message?.let {
            OfflineActionSnackbar(
                message = it,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 88.dp)
                    .fillMaxWidth(),
            )
        }
    }
}
