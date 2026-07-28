package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arda.cineverse.ui.components.CVBottomNavBar
import com.arda.cineverse.ui.components.CVGradientButton
import com.arda.cineverse.ui.components.HomeTopBar
import com.arda.cineverse.ui.components.ListTabButton
import com.arda.cineverse.ui.components.MyListEmptyState
import com.arda.cineverse.ui.components.SavedMovieCard
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.ErrorColor
import com.arda.cineverse.ui.theme.OnSurface
import com.arda.cineverse.ui.theme.Primary
import com.arda.cineverse.ui.theme.TextSecondary
import com.arda.cineverse.viewmodel.MyListTab
import com.arda.cineverse.viewmodel.MyListViewModel

@Composable
fun MyListScreen(
    modifier: Modifier = Modifier,
    onMovieClick: (movieId: Int) -> Unit = {},
    onStartExploring: () -> Unit = {},
    onNavigateTab: (Int) -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: MyListViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Spacer(Modifier.height(12.dp))
            HomeTopBar(onProfileClick = onProfileClick)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Listem", color = OnSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { /* film arama, sonra bağlanacak */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "Ara", tint = OnSurface)
                    }
                    IconButton(onClick = { /* filtre/sıralama, sonra bağlanacak */ }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filtre", tint = OnSurface)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ListTabButton(
                    text = "Favorilerim",
                    icon = Icons.Filled.Favorite,
                    selected = uiState.selectedTab == MyListTab.FAVORITES,
                    onClick = { viewModel.selectTab(MyListTab.FAVORITES) },
                    modifier = Modifier.weight(1f),
                )
                ListTabButton(
                    text = "İzleme Listem",
                    icon = Icons.Filled.BookmarkBorder,
                    selected = uiState.selectedTab == MyListTab.WATCHLIST,
                    onClick = { viewModel.selectTab(MyListTab.WATCHLIST) },
                    modifier = Modifier.weight(1f),
                )
            }

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
                        CVGradientButton(text = "Tekrar Dene", onClick = { viewModel.loadAll() })
                    }
                }
                uiState.currentList.isEmpty() -> {
                    val isFavTab = uiState.selectedTab == MyListTab.FAVORITES
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
                        MyListEmptyState(
                            title = if (isFavTab) "Henüz favori film yok" else "İzleme listen boş",
                            description = if (isFavTab) {
                                "Beğendiğin filmleri favorilere ekleyerek burada görüntüleyebilirsin."
                            } else {
                                "İzlemek istediğin filmleri listene ekleyerek burada görüntüleyebilirsin."
                            },
                            onStartExploring = onStartExploring,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                        )
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x1A7C5CFC))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${uiState.currentList.size} Film", color = OnSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${uiState.averageRating} Ortalama Puan", color = OnSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(uiState.currentList, key = { it.id }) { movie ->
                            SavedMovieCard(
                                movie = movie,
                                badgeIcon = if (uiState.selectedTab == MyListTab.FAVORITES) Icons.Filled.Favorite else Icons.Filled.Bookmark,
                                badgeTint = if (uiState.selectedTab == MyListTab.FAVORITES) ErrorColor else Primary,
                                onClick = { onMovieClick(movie.id) },
                                onBadgeClick = { viewModel.removeFromCurrentList(movie.id) },
                            )
                        }
                    }
                }
            }
        }

        CVBottomNavBar(
            selectedIndex = 2,
            onItemSelected = onNavigateTab,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}