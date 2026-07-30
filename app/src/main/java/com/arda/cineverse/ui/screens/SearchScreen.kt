package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.SavedMovie
import com.arda.cineverse.data.repository.RecommendationRepository
import com.arda.cineverse.data.repository.UserListRepository
import com.arda.cineverse.ui.components.CVBottomNavBar
import com.arda.cineverse.ui.components.HomeTopBar
import com.arda.cineverse.ui.components.PopularMovieCard
import com.arda.cineverse.ui.components.SearchModeBar
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.OnSurface
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.ui.theme.TextSecondary
import com.arda.cineverse.viewmodel.SearchMode
import com.arda.cineverse.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    startInAiMode: Boolean = false,
    onMovieClick: (movieId: Int) -> Unit = {},
    onTvShowClick: (tvId: Int) -> Unit = {},
    onNavigateTab: (Int) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    viewModel: SearchViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val userListRepository = remember { UserListRepository() }
    val recommendationRepository = remember { RecommendationRepository() }

    var favoriteIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(Unit) {
        val favorites = userListRepository.getFavorites().getOrDefault(emptyList())
        favoriteIds = favorites.map { it.id }.toSet()
    }

    fun toggleFavorite(movie: Movie) {
        val isFav = movie.id in favoriteIds
        favoriteIds = if (isFav) favoriteIds - movie.id else favoriteIds + movie.id
        scope.launch {
            if (isFav) {
                userListRepository.removeFavorite(movie.id, mediaType = movie.mediaType)
                if (movie.mediaType == "tv") {
                    recommendationRepository.removeTvFavoriteSignal(movie.id)
                } else {
                    recommendationRepository.removeFavoriteSignal(movie.id)
                }
            } else {
                userListRepository.addFavorite(
                    SavedMovie(
                        id = movie.id,
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        rating = movie.rating,
                        year = movie.year,
                        genreIds = movie.genreIds,
                        mediaType = movie.mediaType,
                    ),
                )
                if (movie.mediaType == "tv") {
                    recommendationRepository.recordTvFavorite(movie.id, movie.genreIds)
                } else {
                    recommendationRepository.recordFavorite(movie.id, movie.genreIds)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (startInAiMode) viewModel.setMode(SearchMode.AI)
    }

    Box(modifier = modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Spacer(Modifier.height(12.dp))
            HomeTopBar(onProfileClick = onProfileClick, onNotificationsClick = onNotificationsClick)
            Spacer(Modifier.height(16.dp))

            SearchModeBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                mode = uiState.mode,
                isLoading = uiState.isLoading,
                onToggleMode = {
                    viewModel.setMode(if (uiState.mode == SearchMode.AI) SearchMode.NORMAL else SearchMode.AI)
                },
                onSearch = { viewModel.search() },
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(20.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(uiState.errorMessage!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                }
                !uiState.hasSearched -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (uiState.mode == SearchMode.AI) {
                                "Film veya diziyi kendi cümlelerinle tarif et, yapay zeka en olası 3 sonucu bulsun"
                            } else {
                                "Film veya dizi adı yazarak aramaya başla"
                            },
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                uiState.results.isEmpty() -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.SearchOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Sonuç bulunamadı", color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Farklı bir arama deneyin", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {
                    if (uiState.mode == SearchMode.AI) {
                        Text(
                            "Yapay zekanın önerileri",
                            color = OnSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(uiState.results, key = { "${it.mediaType}_${it.id}" }) { movie ->
                            PopularMovieCard(
                                movie = movie.copy(isFavorite = movie.id in favoriteIds),
                                onClick = {
                                    if (movie.mediaType == "tv") {
                                        onTvShowClick(movie.id)
                                    } else {
                                        onMovieClick(movie.id)
                                    }
                                },
                                onFavoriteClick = { toggleFavorite(movie) },
                            )
                        }
                    }
                }
            }
        }

        CVBottomNavBar(
            selectedIndex = 1,
            onItemSelected = onNavigateTab,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}