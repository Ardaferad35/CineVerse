package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arda.cineverse.data.model.Movie
import com.arda.cineverse.data.model.UpcomingMovie
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.ui.components.CVGradientButton
import com.arda.cineverse.ui.components.PopularMovieCard
import com.arda.cineverse.ui.components.UpcomingMovieCard
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.OnSurface
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.ui.theme.TextSecondary
import com.arda.cineverse.viewmodel.PaginatedMovieListViewModel
import com.arda.cineverse.viewmodel.PaginatedMovieListViewModelFactory

enum class MovieListSection { POPULAR, UPCOMING }

@Composable
fun MovieListScreen(
    section: MovieListSection,
    onBack: () -> Unit = {},
    onMovieClick: (Int) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = OnSurface)
            }
            Text(
                text = if (section == MovieListSection.POPULAR) "Popüler Filmler" else "Yakında Vizyona Girecekler",
                color = OnSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { /* Filtreleme sonraki adımda eklenecek */ }) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filtrele", tint = OnSurface)
            }
        }

        when (section) {
            MovieListSection.POPULAR -> PopularMoviesGrid(onMovieClick = onMovieClick)
            MovieListSection.UPCOMING -> UpcomingMoviesGrid(onMovieClick = onMovieClick)
        }
    }
}

@Composable
private fun PopularMoviesGrid(onMovieClick: (Int) -> Unit) {
    val viewModel: PaginatedMovieListViewModel<Movie> = viewModel(
        factory = PaginatedMovieListViewModelFactory(
            fetchPage = { page -> MovieRepository().getPopularMovies(page) },
            idSelector = { it.id },
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    MovieGridContent(
        items = uiState.items,
        isLoading = uiState.isLoading,
        isLoadingMore = uiState.isLoadingMore,
        errorMessage = uiState.errorMessage,
        onLoadMore = viewModel::loadNextPage,
        onRetry = viewModel::loadFirstPage,
        itemKey = { it.id },
        itemContent = { movie ->
            PopularMovieCard(movie = movie, onClick = { onMovieClick(movie.id) }, onFavoriteClick = {})
        },
    )
}

@Composable
private fun UpcomingMoviesGrid(onMovieClick: (Int) -> Unit) {
    val viewModel: PaginatedMovieListViewModel<UpcomingMovie> = viewModel(
        factory = PaginatedMovieListViewModelFactory(
            fetchPage = { page -> MovieRepository().getUpcomingMovies(page) },
            idSelector = { it.id },
        ),
    )
    val uiState by viewModel.uiState.collectAsState()

    MovieGridContent(
        items = uiState.items,
        isLoading = uiState.isLoading,
        isLoadingMore = uiState.isLoadingMore,
        errorMessage = uiState.errorMessage,
        onLoadMore = viewModel::loadNextPage,
        onRetry = viewModel::loadFirstPage,
        itemKey = { it.id },
        itemContent = { movie ->
            UpcomingMovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
        },
    )
}

@Composable
private fun <T> MovieGridContent(
    items: List<T>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String?,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        }
        errorMessage != null && items.isEmpty() -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(errorMessage, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                CVGradientButton(text = "Tekrar Dene", onClick = onRetry)
            }
        }
        else -> {
            val gridState = rememberLazyGridState()

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = itemKey) { item -> itemContent(item) }

                if (isLoadingMore) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }

            LaunchedEffect(gridState) {
                snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .collect { lastVisibleIndex ->
                        if (lastVisibleIndex != null && lastVisibleIndex >= items.size - 6) {
                            onLoadMore()
                        }
                    }
            }
        }
    }
}