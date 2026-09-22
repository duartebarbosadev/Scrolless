/*
 * Copyright (C) 2026 Scrolless
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
package com.scrolless.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scrolless.app.R
import com.scrolless.app.core.data.repository.OnboardingPreferences
import com.scrolless.app.feature.settings.R as SettingsR
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit, replay: Boolean = false, viewModel: OnboardingViewModel = hiltViewModel()) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val draft = preferences
    if (draft == null) {
        Surface(Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                if (failed) {
                    TextButton(onClick = viewModel::load) { Text(stringResource(R.string.onboarding_retry)) }
                } else {
                    CircularProgressIndicator()
                }
            }
        }
        return
    }
    val pager = rememberPagerState(pageCount = { 5 })
    MaterialTheme(colorScheme = onboardingColors(pager.currentPage + pager.currentPageOffsetFraction)) {
        OnboardingContent(
            pager = pager,
            preferences = draft,
            saving = saving,
            failed = failed,
            onChange = viewModel::update,
            onFinish = { viewModel.finish(onFinished = onFinished) },
            onSkip = { if (replay) onFinished() else viewModel.finish(skip = true, onFinished = onFinished) },
        )
    }
}

private val PageBackgrounds = listOf(Color(0xFFF0E6FF), Color(0xFFDBF4E9), Color(0xFFFFE3D5), Color(0xFFFFEFC1), Color(0xFFE0EAFF))
private val PageAccents = listOf(Color(0xFF6541C6), Color(0xFF176B57), Color(0xFFA63F36), Color(0xFF795414), Color(0xFF355EB0))

/** Interpolate with the finger, so the whole screen changes colour during a swipe. */
@Composable
private fun onboardingColors(position: Float): ColorScheme {
    val base = MaterialTheme.colorScheme
    val dark = base.background.luminance() < 0.5f
    val progress = position.coerceIn(0f, 4f)
    val from = progress.toInt()
    val to = (from + 1).coerceAtMost(4)
    val tint = lerp(PageBackgrounds[from], PageBackgrounds[to], progress - from)
    val accent = lerp(PageAccents[from], PageAccents[to], progress - from)
    val background = if (dark) lerp(Color(0xFF151219), accent, 0.16f) else tint
    val primary = if (dark) lerp(accent, Color.White, 0.65f) else accent
    val container = if (dark) lerp(background, accent, 0.3f) else lerp(tint, Color.White, 0.65f)
    val ink = if (dark) Color(0xFFF5EDF9) else Color(0xFF292333)
    return base.copy(
        primary = primary,
        onPrimary = if (dark) Color(0xFF21192B) else Color.White,
        primaryContainer = container,
        onPrimaryContainer = ink,
        secondaryContainer = container,
        onSecondaryContainer = ink,
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        onSurfaceVariant = if (dark) Color(0xFFCEC3D9) else Color(0xFF62586B),
        surfaceContainerLow = container,
        surfaceContainerHighest = primary.copy(alpha = 0.18f),
        outlineVariant = primary.copy(alpha = 0.25f),
    )
}

@Composable
private fun OnboardingContent(
    pager: PagerState,
    preferences: OnboardingPreferences,
    saving: Boolean,
    failed: Boolean,
    onChange: (OnboardingPreferences) -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
) {
    val step = pager.currentPage
    val scope = rememberCoroutineScope()
    fun navigate(page: Int) {
        scope.launch { pager.animateScrollToPage(page, animationSpec = tween(450)) }
    }
    var feedEntry by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val progressDescription = stringResource(R.string.onboarding_step, step + 1, 5)
    BackHandler(enabled = step > 0 || saving) { if (!saving) navigate(step - 1) }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val progress = pager.currentPage + pager.currentPageOffsetFraction
                drawCircle(
                    colors.primary.copy(alpha = 0.07f), size.width * 0.85f,
                    Offset(size.width * (0.2f + progress * 0.13f), size.height * 0.32f),
                )
                drawCircle(
                    colors.primary.copy(alpha = 0.05f), size.width * 0.55f,
                    Offset(size.width * (1f - progress * 0.1f), size.height * 0.76f),
                )
            }
            BoxWithConstraints(Modifier.safeDrawingPadding()) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxSize().align(Alignment.TopCenter)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (step > 0) {
                            IconButton(onClick = { navigate(step - 1) }, enabled = !saving) {
                                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(SettingsR.string.back))
                            }
                        }
                        Text("Scrolless", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onSkip, enabled = !saving) { Text(stringResource(R.string.onboarding_skip)) }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).semantics { contentDescription = progressDescription },
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier.width(if (index == step) 24.dp else 6.dp).height(6.dp).clip(CircleShape)
                                    .background(if (index <= step) colors.primary else colors.surfaceContainerHighest),
                            )
                        }
                    }
                    HorizontalPager(
                        state = pager,
                        userScrollEnabled = !saving,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) { page ->
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            // Reserve room for wrapped headlines and controls on compact phones.
                            val controlsHeight = (if (page == 0) 112.dp else 80.dp) +
                                if (maxWidth < 400.dp) 64.dp else 0.dp
                            val phoneHeight = (maxHeight - controlsHeight)
                                .coerceAtLeast(180.dp)
                                .coerceAtMost((maxWidth - 72.dp) * (740f / 360f))
                            Column(
                                Modifier.fillMaxSize()
                                    .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                if (page < 4) {
                                    val asset = when (page) {
                                        0 -> if (feedEntry) "instagram_feed_reel_block" else "instagram_reels_block"
                                        1 -> "instagram_feed_reel_cover"
                                        2 -> "instagram_dm_reel_allowed"
                                        else -> "instagram_story_block"
                                    }
                                    AnimationStage(asset, blocking = true, phoneHeight = phoneHeight)
                                    if (page == 0) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = !feedEntry, onClick = { feedEntry = false },
                                                label = { Text(stringResource(R.string.onboarding_reels_tab)) },
                                            )
                                            FilterChip(
                                                selected = feedEntry, onClick = { feedEntry = true },
                                                label = { Text(stringResource(R.string.onboarding_feed_tab)) },
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.height(16.dp))
                                    }
                                } else {
                                    Box(
                                        Modifier.padding(vertical = 16.dp).size(80.dp).background(colors.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.ic_launcher_monochrome),
                                            null,
                                            Modifier.size(48.dp),
                                            tint = colors.onPrimaryContainer,
                                        )
                                    }
                                }
                                val title = when (page) {
                                    0 -> R.string.onboarding_reels_title
                                    1 -> R.string.onboarding_feed_title
                                    2 -> R.string.onboarding_dm_title
                                    3 -> R.string.onboarding_stories_title
                                    else -> R.string.onboarding_finish_title
                                }
                                Text(
                                    stringResource(title), style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                                    modifier = Modifier.semantics { heading() },
                                )
                                Spacer(Modifier.height(16.dp))
                                if (page == 4) {
                                    ChoiceToggle(
                                        stringResource(SettingsR.string.settings_include_stories_title),
                                        preferences.includeStories,
                                        { onChange(preferences.copy(includeStories = it)) },
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    ChoiceToggle(
                                        stringResource(R.string.onboarding_dm_toggle),
                                        preferences.allowDm,
                                        { onChange(preferences.copy(allowDm = it)) },
                                    )
                                }
                            }
                        }
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (failed) {
                            Text(
                                stringResource(R.string.onboarding_error), color = colors.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Button(
                            onClick = { if (step == 4) onFinish() else navigate(step + 1) },
                            enabled = !saving && !pager.isScrollInProgress,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            shape = CircleShape,
                        ) {
                            if (saving) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    stringResource(if (step == 4) R.string.onboarding_finish else R.string.onboarding_next),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        if (step == 4) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.onboarding_change_later),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimationStage(asset: String, blocking: Boolean, phoneHeight: Dp) {
    Box(Modifier.fillMaxWidth().height(phoneHeight + 12.dp), contentAlignment = Alignment.Center) {
        OnboardingAnimation(
            asset, blocking, stringResource(R.string.onboarding_demo),
            Modifier.height(phoneHeight).aspectRatio(360f / 740f)
                .shadow(6.dp, RoundedCornerShape(phoneHeight * (48f / 740f)))
                .clip(RoundedCornerShape(phoneHeight * (48f / 740f))),
        )
    }
}

@Composable
private fun ChoiceToggle(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (checked) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChecked)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}
