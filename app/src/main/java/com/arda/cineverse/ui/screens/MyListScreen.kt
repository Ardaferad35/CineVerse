package com.arda.cineverse.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arda.cineverse.R
import com.arda.cineverse.ui.components.*
import com.arda.cineverse.ui.theme.*
import com.arda.cineverse.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    modifier: Modifier = Modifier,
    onMovieClick: (movieId: Int) -> Unit = {},
    onTvShowClick: (tvId: Int) -> Unit = {},
    onStartExploring: () -> Unit = {},
    onNavigateTab: (Int) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Spacer(Modifier.height(12.dp))
            HomeTopBar(
                onProfileClick = onProfileClick,
                onNotificationsClick = onNotificationsClick,
                onFriendsClick = onFriendsClick,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.my_list_title), color = OnSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = viewModel::toggleSearch) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.my_list_search_cd),
                            tint = if (uiState.isSearchActive) Primary else OnSurface,
                        )
                    }
                    IconButton(onClick = { viewModel.setFilterSheetOpen(true) }) {
                        BadgedBox(
                            badge = {
                                if (uiState.hasActiveFilters) {
                                    Badge(containerColor = Primary) {
                                        Text("${uiState.activeFilterCount}")
                                    }
                                }
                            },
                        ) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = stringResource(R.string.my_list_filter_cd),
                                tint = if (uiState.hasActiveFilters) Primary else OnSurface,
                            )
                        }
                    }
                }
            }

            if (uiState.isSearchActive) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CVTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = stringResource(R.string.my_list_search_placeholder),
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = viewModel::toggleSearch) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = OnSurface)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ListTabButton(
                    text = stringResource(R.string.my_list_tab_favorites),
                    icon = Icons.Filled.Favorite,
                    selected = uiState.selectedTab == MyListTab.FAVORITES,
                    onClick = { viewModel.selectTab(MyListTab.FAVORITES) },
                    modifier = Modifier.weight(1f),
                )
                ListTabButton(
                    text = stringResource(R.string.my_list_tab_watchlist),
                    icon = Icons.Filled.BookmarkBorder,
                    selected = uiState.selectedTab == MyListTab.WATCHLIST,
                    onClick = { viewModel.selectTab(MyListTab.WATCHLIST) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            if (uiState.hasActiveFilters) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Primary.copy(alpha = 0.18f))
                                .clickable { viewModel.resetFilters() }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.common_clear), tint = Primary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.my_list_reset_filters), color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (uiState.sortCriterion != MyListSortCriterion.IMDB_RATING || uiState.sortOrder != MyListSortOrder.DESCENDING) {
                        item {
                            ActiveFilterChip(
                                text = stringResource(
                                    R.string.my_list_active_sort_chip,
                                    stringResource(uiState.sortCriterion.labelRes),
                                    if (uiState.sortOrder == MyListSortOrder.DESCENDING) {
                                        stringResource(R.string.my_list_sort_order_desc_short)
                                    } else {
                                        stringResource(R.string.my_list_sort_order_asc_short)
                                    },
                                ),
                                onClear = {
                                    viewModel.setSortCriterion(MyListSortCriterion.IMDB_RATING)
                                    viewModel.setSortOrder(MyListSortOrder.DESCENDING)
                                },
                            )
                        }
                    }

                    if (uiState.searchQuery.isNotBlank()) {
                        item {
                            ActiveFilterChip(
                                text = stringResource(R.string.my_list_active_search_chip, uiState.searchQuery),
                                onClear = { viewModel.setSearchQuery("") },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            when {
                uiState.isLoading -> {
                    GridShimmer(modifier = Modifier.weight(1f))
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(uiState.errorMessage!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        CVGradientButton(text = stringResource(R.string.common_retry), onClick = { viewModel.loadAll() })
                    }
                }
                uiState.currentList.isEmpty() -> {
                    val isFavTab = uiState.selectedTab == MyListTab.FAVORITES
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
                        MyListEmptyState(
                            title = if (uiState.hasActiveFilters) {
                                stringResource(R.string.my_list_empty_filtered_title)
                            } else if (isFavTab) {
                                stringResource(R.string.my_list_empty_favorites_title)
                            } else {
                                stringResource(R.string.my_list_empty_watchlist_title)
                            },
                            description = if (uiState.hasActiveFilters) {
                                stringResource(R.string.my_list_empty_filtered_desc)
                            } else if (isFavTab) {
                                stringResource(R.string.my_list_empty_favorites_desc)
                            } else {
                                stringResource(R.string.my_list_empty_watchlist_desc)
                            },
                            onStartExploring = if (uiState.hasActiveFilters) viewModel::resetFilters else onStartExploring,
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
                            Text(stringResource(R.string.my_list_content_count, uiState.currentList.size), color = OnSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.my_list_avg_rating, uiState.averageRating.toString()), color = OnSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
                        items(uiState.currentList, key = { "${it.mediaType}_${it.id}" }) { movie ->
                            SavedMovieCard(
                                movie = movie,
                                badgeIcon = if (uiState.selectedTab == MyListTab.FAVORITES) Icons.Filled.Favorite else Icons.Filled.Bookmark,
                                badgeTint = if (uiState.selectedTab == MyListTab.FAVORITES) ErrorColor else Primary,
                                onClick = {
                                    if (movie.mediaType == "tv") {
                                        onTvShowClick(movie.id)
                                    } else {
                                        onMovieClick(movie.id)
                                    }
                                },
                                onBadgeClick = { viewModel.removeFromCurrentList(movie.id, movie.mediaType) },
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

        if (uiState.isFilterSheetOpen) {
            MyListFilterBottomSheet(
                state = uiState,
                onSortCriterionSelected = viewModel::setSortCriterion,
                onSortOrderSelected = viewModel::setSortOrder,
                onReset = viewModel::resetFilters,
                onDismiss = { viewModel.setFilterSheetOpen(false) },
            )
        }

        ClearOfflineMessageAfterDelay(uiState.offlineMessage) { viewModel.clearOfflineMessage() }
        uiState.offlineMessage?.let {
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

@Composable
private fun ActiveFilterChip(text: String, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariant)
            .border(1.dp, Primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp).clickable { onClear() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyListFilterBottomSheet(
    state: MyListUiState,
    onSortCriterionSelected: (MyListSortCriterion) -> Unit,
    onSortOrderSelected: (MyListSortOrder) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.my_list_filter_sheet_title),
                    color = OnSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.my_list_sort_criterion_label),
                color = OnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MyListSortCriterion.entries.forEach { criterion ->
                    val isSelected = state.sortCriterion == criterion
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Primary.copy(alpha = 0.15f) else SurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) Primary else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { onSortCriterionSelected(criterion) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(criterion.labelRes),
                            color = if (isSelected) Primary else OnSurface,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.my_list_sort_direction_label),
                color = OnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MyListSortOrder.entries.forEach { order ->
                    val isSelected = state.sortOrder == order
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Primary.copy(alpha = 0.15f) else SurfaceVariant)
                            .border(1.dp, if (isSelected) Primary else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { onSortOrderSelected(order) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(order.labelRes),
                            color = if (isSelected) Primary else OnSurface,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DividerColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface),
                ) {
                    Text(stringResource(R.string.my_list_reset_filters), fontWeight = FontWeight.SemiBold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(brush = Brush.horizontalGradient(PrimaryGradient))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.my_list_done), color = OnPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}