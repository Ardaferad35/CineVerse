package com.arda.cineverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arda.cineverse.data.model.TvEpisode
import com.arda.cineverse.data.model.TvSeasonDetail
import com.arda.cineverse.data.model.TvSeasonSummary
import com.arda.cineverse.ui.theme.*

@Composable
fun TvSeasonsAndEpisodesSection(
    seasons: List<TvSeasonSummary>,
    selectedSeasonNumber: Int,
    seasonDetail: TvSeasonDetail?,
    isLoadingSeason: Boolean,
    watchedEpisodesMap: Map<String, Boolean>,
    onSeasonSelect: (Int) -> Unit,
    onEpisodeToggle: (seasonNumber: Int, episodeNumber: Int) -> Unit,
    onSeasonToggleAll: (seasonNumber: Int, episodes: List<TvEpisode>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Sezonlar & Bölümler",
            color = OnSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Sezon Chip'leri
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(seasons, key = { it.seasonNumber }) { season ->
                val isSelected = season.seasonNumber == selectedSeasonNumber
                val isSeasonCompleted = season.episodeCount > 0 && (1..season.episodeCount).all { ep ->
                    watchedEpisodesMap["s${season.seasonNumber}_e$ep"] == true
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onSeasonSelect(season.seasonNumber) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(season.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSeasonCompleted) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary.copy(alpha = 0.2f),
                        selectedLabelColor = Primary,
                        containerColor = Surface,
                        labelColor = TextSecondary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = DividerColor,
                        selectedBorderColor = Primary,
                        borderWidth = 1.dp,
                    ),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoadingSeason) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
        } else if (seasonDetail != null && seasonDetail.episodes.isNotEmpty()) {
            val episodes = seasonDetail.episodes
            val allWatched = episodes.all { ep ->
                watchedEpisodesMap["s${seasonDetail.seasonNumber}_e${ep.episodeNumber}"] == true
            }

            // Sezon Başlık Satırı & Tümünü İzlendi Yap Butonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val watchedCount = episodes.count { ep ->
                    watchedEpisodesMap["s${seasonDetail.seasonNumber}_e${ep.episodeNumber}"] == true
                }

                Text(
                    text = "$watchedCount / ${episodes.size} Bölüm İzlendi",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )

                TextButton(
                    onClick = { onSeasonToggleAll(seasonDetail.seasonNumber, episodes) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Icon(
                        if (allWatched) Icons.Filled.CheckCircle else Icons.Filled.DoneAll,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (allWatched) "Tümünü Kaldır" else "Tümünü İzlendi Yap",
                        color = Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Bölüm Kartları
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                episodes.forEach { episode ->
                    val isWatched = watchedEpisodesMap["s${episode.seasonNumber}_e${episode.episodeNumber}"] == true
                    TvEpisodeCard(
                        episode = episode,
                        isWatched = isWatched,
                        onToggle = { onEpisodeToggle(episode.seasonNumber, episode.episodeNumber) },
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Bu sezona ait bölüm bilgisi bulunamadı.", color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TvEpisodeCard(
    episode: TvEpisode,
    isWatched: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isWatched) Surface.copy(alpha = 0.6f) else Surface)
            .border(
                1.dp,
                if (isWatched) Primary.copy(alpha = 0.4f) else DividerColor,
                RoundedCornerShape(14.dp),
            )
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Bölüm Görseli
        Box(
            modifier = Modifier
                .size(width = 84.dp, height = 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (episode.stillUrl != null) {
                AsyncImage(
                    model = episode.stillUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.Tv, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        // Bölüm Bilgileri
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNumber}. ${episode.name}",
                color = if (isWatched) TextSecondary else OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (episode.runtimeMinutes != null && episode.runtimeMinutes > 0) {
                    Text("${episode.runtimeMinutes} dk", color = TextSecondary, fontSize = 12.sp)
                }
                if (episode.airDate != null) {
                    if (episode.runtimeMinutes != null && episode.runtimeMinutes > 0) {
                        Text(" • ", color = TextSecondary, fontSize = 12.sp)
                    }
                    Text(episode.airDate, color = TextSecondary, fontSize = 12.sp)
                }
            }

            if (!episode.overview.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = episode.overview,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // İzlendi İkonu / Butonu
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (isWatched) "İzlendi" else "İzlenecek",
                tint = if (isWatched) Primary else TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
