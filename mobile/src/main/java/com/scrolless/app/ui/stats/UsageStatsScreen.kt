/*
 * Copyright (C) 2025 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrolless.app.R
import com.scrolless.app.designsystem.component.AutoResizingText
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.util.formatTime
import com.scrolless.app.util.radialGradientScrim

// App brand colors
private val InstagramGradient = listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))
private val InstagramColor = Color(0xFFE1306C)
private val YouTubeColor = Color(0xFFFF0000)
private val TikTokColor = Color(0xFF00F2EA)
private val TikTokSecondary = Color(0xFFFF0050)

@Composable
fun UsageStatsScreen(
    modifier: Modifier = Modifier,
    viewModel: UsageStatsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UsageStatsContent(
        modifier = modifier,
        uiState = uiState,
        onBackClick = onBackClick,
    )
}

@Composable
private fun UsageStatsContent(
    modifier: Modifier = Modifier,
    uiState: UsageStatsUiState,
    onBackClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .radialGradientScrim(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = stringResource(R.string.usage_stats_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Today's Total Card
            TotalUsageCard(
                totalUsage = uiState.totalTodayUsage,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section Title
            Text(
                text = stringResource(R.string.usage_by_app),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Per-App Cards
            val totalUsage = maxOf(uiState.reelsUsage + uiState.shortsUsage + uiState.tiktokUsage, 1L)

            AppUsageCard(
                appName = stringResource(R.string.app_instagram_reels),
                usage = uiState.reelsUsage,
                progress = uiState.reelsUsage.toFloat() / totalUsage,
                color = InstagramColor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppUsageCard(
                appName = stringResource(R.string.app_youtube_shorts),
                usage = uiState.shortsUsage,
                progress = uiState.shortsUsage.toFloat() / totalUsage,
                color = YouTubeColor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            AppUsageCard(
                appName = stringResource(R.string.app_tiktok),
                usage = uiState.tiktokUsage,
                progress = uiState.tiktokUsage.toFloat() / totalUsage,
                color = TikTokColor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Weekly Summary Section
            Text(
                text = stringResource(R.string.weekly_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            WeeklySummaryCard(
                weeklyTotal = uiState.weeklyTotal,
                weeklyReels = uiState.weeklyReels,
                weeklyShorts = uiState.weeklyShorts,
                weeklyTiktok = uiState.weeklyTiktok,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TotalUsageCard(
    totalUsage: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.today_total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            AutoResizingText(
                text = totalUsage.formatTime(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                minFontSize = 24.sp,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.time_on_short_content),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun AppUsageCard(
    appName: String,
    usage: Long,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "progress",
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Text(
                    text = usage.formatTime(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun WeeklySummaryCard(
    weeklyTotal: Long,
    weeklyReels: Long,
    weeklyShorts: Long,
    weeklyTiktok: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.last_7_days),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = weeklyTotal.formatTime(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                WeeklyAppStat(
                    appName = stringResource(R.string.app_reels_short),
                    usage = weeklyReels,
                    color = InstagramColor,
                )
                WeeklyAppStat(
                    appName = stringResource(R.string.app_shorts_short),
                    usage = weeklyShorts,
                    color = YouTubeColor,
                )
                WeeklyAppStat(
                    appName = stringResource(R.string.app_tiktok_short),
                    usage = weeklyTiktok,
                    color = TikTokColor,
                )
            }
        }
    }
}

@Composable
private fun WeeklyAppStat(
    appName: String,
    usage: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = usage.formatTime(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )

        Text(
            text = appName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageStatsScreenPreview() {
    ScrollessTheme {
        UsageStatsContent(
            uiState = UsageStatsUiState(
                totalTodayUsage = 5400000L, // 1h 30m
                reelsUsage = 2700000L, // 45m
                shortsUsage = 1800000L, // 30m
                tiktokUsage = 900000L, // 15m
                weeklyTotal = 25200000L, // 7h
                weeklyReels = 12600000L, // 3.5h
                weeklyShorts = 7200000L, // 2h
                weeklyTiktok = 5400000L, // 1.5h
                isLoading = false,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageStatsScreenEmptyPreview() {
    ScrollessTheme {
        UsageStatsContent(
            uiState = UsageStatsUiState(
                totalTodayUsage = 0L,
                reelsUsage = 0L,
                shortsUsage = 0L,
                tiktokUsage = 0L,
                weeklyTotal = 0L,
                weeklyReels = 0L,
                weeklyShorts = 0L,
                weeklyTiktok = 0L,
                isLoading = false,
            ),
        )
    }
}
