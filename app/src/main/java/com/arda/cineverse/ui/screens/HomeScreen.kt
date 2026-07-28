package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arda.cineverse.data.model.Category
import com.arda.cineverse.data.model.mockCategories
import com.arda.cineverse.ui.components.CVBottomNavBar
import com.arda.cineverse.ui.components.CVGradientButton
import com.arda.cineverse.ui.components.CategoryChip
import com.arda.cineverse.ui.components.FeaturedMovieBanner
import com.arda.cineverse.ui.components.HomeSearchBar
import com.arda.cineverse.ui.components.HomeSectionHeader
import com.arda.cineverse.ui.components.HomeTopBar
import com.arda.cineverse.ui.components.PopularMovieCard
import com.arda.cineverse.ui.components.SearchSuggestionsList
import com.arda.cineverse.ui.components.UpcomingMovieCard
import com.arda.cineverse.ui.components.categoryColor
import com.arda.cineverse.ui.components.categoryIcon
import com.arda.cineverse.ui.theme.Accent
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.ErrorColor
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.ui.theme.Surface
import com.arda.cineverse.ui.theme.TextSecondary
import com.arda.cineverse.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onMovieClick: (movieId: Int) -> Unit = {},
    onSeeAllClick: (section: String) -> Unit = {},
    onAiSearchClick: () -> Unit = {},
    onNavigateTab: (Int) -> Unit = {},
    onCategoryClick: (Category) -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
) {
    var selectedCategoryId by remember { mutableStateOf(mockCategories.first().id) }

    val uiState by homeViewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Spacer(Modifier.height(12.dp))
            HomeTopBar(onProfileClick = onProfileClick)
            Spacer(Modifier.height(12.dp))
            HomeSearchBar(
                value = uiState.searchQuery,
                onValueChange = { homeViewModel.onSearchQueryChange(it) },
                onAiClick = onAiSearchClick,
                onClear = { homeViewModel.clearSearch() },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(uiState.errorMessage!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        CVGradientButton(text = "Tekrar Dene", onClick = { homeViewModel.loadMovies() })
                    }
                }
                uiState.searchQuery.isNotBlank() -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                    ) {
                        when {
                            uiState.isSearching -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Surface)
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Aranıyor...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            uiState.searchSuggestions.isNotEmpty() -> {
                                SearchSuggestionsList(
                                    suggestions = uiState.searchSuggestions,
                                    onMovieClick = onMovieClick,
                                )
                            }
                            else -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Surface)
                                        .padding(16.dp),
                                ) {
                                    Text("Sonuç bulunamadı", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        val featured = uiState.featuredMovie
                        if (featured != null) {
                            item {
                                FeaturedMovieBanner(
                                    movie = featured,
                                    onDetailsClick = { onMovieClick(featured.id) },
                                    onAddToListClick = {},
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                )
                            }
                        }

                        if (uiState.recommendedMovies.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HomeSectionHeader(
                                        icon = Icons.Filled.Recommend,
                                        iconTint = Primary,
                                        title = "Sizin İçin Önerilenler",
                                        showSeeAll = false,
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(uiState.recommendedMovies, key = { it.id }) { movie ->
                                            PopularMovieCard(
                                                movie = movie,
                                                onClick = { onMovieClick(movie.id) },
                                                onFavoriteClick = {},
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeSectionHeader(
                                    icon = Icons.Filled.LocalFireDepartment,
                                    iconTint = Color(0xFFFF7A45),
                                    title = "Popüler Filmler",
                                    onSeeAllClick = { onSeeAllClick("popular") },
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(uiState.popularMovies, key = { it.id }) { movie ->
                                        PopularMovieCard(
                                            movie = movie,
                                            onClick = { onMovieClick(movie.id) },
                                            onFavoriteClick = {},
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeSectionHeader(
                                    icon = Icons.Filled.CalendarMonth,
                                    iconTint = Accent,
                                    title = "Yakında Vizyona Girecekler",
                                    onSeeAllClick = { onSeeAllClick("upcoming") },
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(uiState.upcomingMovies, key = { it.id }) { movie ->
                                        UpcomingMovieCard(
                                            movie = movie,
                                            onClick = { onMovieClick(movie.id) },
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeSectionHeader(
                                    icon = Icons.Filled.GridView,
                                    iconTint = Primary,
                                    title = "Kategoriler",
                                    onSeeAllClick = { onSeeAllClick("categories") },
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    items(mockCategories, key = { it.id }) { category ->
                                        CategoryChip(
                                            category = category,
                                            icon = categoryIcon(category.genreId),
                                            iconTint = categoryColor(category.genreId),
                                            selected = category.id == selectedCategoryId,
                                            onClick = {
                                                selectedCategoryId = category.id
                                                onCategoryClick(category)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!uiState.isLoading && uiState.errorMessage == null) {
            CVBottomNavBar(
                selectedIndex = 0,
                onItemSelected = onNavigateTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}