package com.arda.cineverse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arda.cineverse.R
import com.arda.cineverse.data.model.Category
import com.arda.cineverse.data.repository.MovieRepository
import com.arda.cineverse.data.repository.TvRepository
import com.arda.cineverse.ui.components.CVGradientButton
import com.arda.cineverse.ui.components.GridShimmer
import com.arda.cineverse.ui.components.categoryColor
import com.arda.cineverse.ui.components.categoryIcon
import com.arda.cineverse.ui.theme.Background
import com.arda.cineverse.ui.theme.OnSurface
import com.arda.cineverse.ui.theme.Surface
import com.arda.cineverse.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AllCategoriesScreen(
    isTvMode: Boolean = false,
    onBack: () -> Unit = {},
    onCategoryClick: (Category) -> Unit = {},
) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val movieRepository = remember { MovieRepository.default() }
    val tvRepository = remember { TvRepository.default() }
    val scope = rememberCoroutineScope()
    // load() bir @Composable değil, bu yüzden stringResource() burada değil,
    // composable gövdesinde önceden çözülüp closure ile yakalanıyor.
    val loadErrorMessage = stringResource(R.string.all_categories_load_error)

    suspend fun load() {
        isLoading = true
        errorMessage = null
        val result = if (isTvMode) tvRepository.getAllTvGenres() else movieRepository.getAllGenres()
        result.fold(
            onSuccess = { list -> categories = list; isLoading = false },
            onFailure = { errorMessage = loadErrorMessage; isLoading = false },
        )
    }

    LaunchedEffect(isTvMode) { load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = OnSurface)
            }
            Spacer(Modifier.width(8.dp))
            Text(if (isTvMode) stringResource(R.string.all_categories_title_tv) else stringResource(R.string.all_categories_title_movie), color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        when {
            isLoading -> {
                GridShimmer(columns = 2, itemCount = 10)
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(errorMessage!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    CVGradientButton(text = stringResource(R.string.common_retry), onClick = { scope.launch { load() } })
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(categories, key = { it.genreId }) { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Surface)
                                .clickable { onCategoryClick(category) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                categoryIcon(category.genreId),
                                contentDescription = null,
                                tint = categoryColor(category.genreId),
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(category.label, color = OnSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}