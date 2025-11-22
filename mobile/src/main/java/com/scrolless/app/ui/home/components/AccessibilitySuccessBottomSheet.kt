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
package com.scrolless.app.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolless.app.R
import com.scrolless.app.designsystem.component.AnimatedButton
import com.scrolless.app.designsystem.component.PopupCircleIcon
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.ui.tooling.DevicePreviews
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySuccessBottomSheet(onDismiss: () -> Unit) {
    val isPreview = LocalInspectionMode.current

    // Only use ModalBottomSheet in non-preview mode
    if (isPreview) {
        AccessibilitySuccessContent(
            onDismiss = {
                Timber.d("AccessibilitySuccess: Dismiss")
                onDismiss()
            },
        )
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = {
                Timber.d("AccessibilitySuccess: Dismiss")
                onDismiss()
            },
            sheetState = sheetState,
            containerColor = Color.Transparent,
        ) {
            AccessibilitySuccessContent(
                onDismiss = {
                    Timber.i("AccessibilitySuccess: Get Started Dismissed")
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun AccessibilitySuccessContent(onDismiss: () -> Unit) {
    val isPreview = LocalInspectionMode.current

    // Content card animation
    val cardAlpha = remember { Animatable(0f) }
    val cardOffsetY = remember { Animatable(50f) }

    LaunchedEffect(Unit) {
        if (!isPreview) {
            launch {
                cardAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 600, easing = EaseInOut),
                )
            }
            launch {
                cardOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 600, easing = EaseInOut),
                )
            }
        } else {
            cardAlpha.snapTo(1f)
            cardOffsetY.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Spacer for floating icon positioning
            Spacer(modifier = Modifier.height(50.dp))

            // Content Card (positioned below the floating icon)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(top = 36.dp), // Extra padding for icon overlap
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Title
                Text(
                    text = stringResource(R.string.accessibility_success_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = stringResource(R.string.accessibility_success_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Next Steps Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.next_steps_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Step 1
                        NextStep(
                            stepNumber = stringResource(R.string.step_one),
                            text = stringResource(R.string.next_step_1),
                            delay = 200L,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Step 2
                        NextStep(
                            stepNumber = stringResource(R.string.step_two),
                            text = stringResource(R.string.next_step_2),
                            delay = 300L,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Get Started Button with animation
            AnimatedButton(
                onClick = {
                    Timber.i("AccessibilitySuccess: Get Started clicked")
                    onDismiss()
                },
                text = stringResource(R.string.get_started),
                delay = 400L,
            )
        }

        // Floating Success Icon - positioned absolutely at the top center
        PopupCircleIcon(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            iconRes = R.drawable.ic_circle_success,
            contentDescription = stringResource(R.string.success_icon_description),
        )
    }
}

@Composable
private fun NextStep(stepNumber: String, text: String, delay: Long) {
    val isPreview = LocalInspectionMode.current

    val alpha = remember { Animatable(if (isPreview) 1f else 0f) }
    val offsetY = remember { Animatable(if (isPreview) 0f else 20f) }

    LaunchedEffect(Unit) {
        if (!isPreview) {
            delay(delay)
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 600, easing = EaseInOut),
                )
            }
            launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 600, easing = EaseInOut),
                )
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha = alpha.value
                    translationY = offsetY.value
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Step Number
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stepNumber,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Step Text
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@DevicePreviews
@Composable
fun AccessibilitySuccessBottomSheetPreview() {
    ScrollessTheme(darkTheme = true) {
        AccessibilitySuccessBottomSheet(
            onDismiss = {},
        )
    }
}
