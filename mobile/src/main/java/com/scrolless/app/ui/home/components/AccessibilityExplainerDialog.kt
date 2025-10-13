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

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.scrolless.app.R
import com.scrolless.app.designsystem.component.AnimatedIcon
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.ui.tooling.DevicePreviews
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityExplainerBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    // Only use ModalBottomSheet in non-preview mode
    if (isPreview) {
        AccessibilityExplainerContent(
            onDismiss = {
                Timber.d("AccessibilityExplainer: Dismiss (preview)")
                onDismiss()
            },
            onOpenSettings = {},
        )
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = {
                Timber.d("AccessibilityExplainer: Dismiss")
                onDismiss()
            },
            sheetState = sheetState,
            containerColor = Color.Transparent,
        ) {
            AccessibilityExplainerContent(
                onDismiss = {
                    Timber.d("AccessibilityExplainer: Not Now")
                    onDismiss()
                },
                onOpenSettings = {
                    try {
                        Timber.i("AccessibilityExplainer: Open accessibility settings")
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to open accessibility settings")
                    }
                },
            )
        }
    }
}

@Composable
private fun AccessibilityExplainerContent(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(top = 60.dp), // Space for floating icon (40dp + 20dp)
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title
            Text(
                text = stringResource(R.string.accessibility_explainer_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.accessibility_explainer_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Steps with staggered animation
            AccessibilityStep(
                stepNumber = stringResource(R.string.step_one),
                text = stringResource(R.string.accessibility_explainer_step1),
            )

            Spacer(modifier = Modifier.height(12.dp))

            AccessibilityStep(
                stepNumber = stringResource(R.string.step_two),
                text = stringResource(R.string.accessibility_explainer_step2),
            )

            Spacer(modifier = Modifier.height(12.dp))

            AccessibilityStep(
                stepNumber = stringResource(R.string.step_three),
                text = stringResource(R.string.accessibility_explainer_step3),
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Privacy Note
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.accessibility_explainer_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val githubUrl = stringResource(R.string.github_url)

            // Open Source Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            Timber.i("AccessibilityExplainer: Open source link clicked")
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = githubUrl.toUri()
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to open GitHub link")
                        }
                    },

                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_github),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.accessibility_explainer_open_source),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Proceed Button
            Button(
                onClick = {
                    Timber.i("AccessibilityExplainer: Proceed -> open settings")
                    onOpenSettings()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.accessibility_explainer_proceed_button),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Not Now Button
            TextButton(
                onClick = {
                    Timber.d("AccessibilityExplainer: Not now")
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.accessibility_explainer_not_now_button),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Floating Icon at the top
        AnimatedIcon(
            modifier = Modifier.align(Alignment.TopCenter),
            iconRes = R.drawable.ic_logo_outline,
            contentDescription = stringResource(R.string.accessibility_explainer_icon_description),
        )
    }
}

@Composable
private fun AccessibilityStep(stepNumber: String, text: String) {
    val isPreview = LocalInspectionMode.current
    var isVisible by remember { mutableStateOf(isPreview) } // Start visible in preview

    // Small animation delay for staggered effect
    LaunchedEffect(Unit) {
        if (!isPreview) {
            isVisible = true
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(durationMillis = 500),
        ) + androidx.compose.animation.slideInVertically(
            animationSpec = tween(durationMillis = 500),
            initialOffsetY = { it / 2 },
        ),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@DevicePreviews
@Composable
fun AccessibilityExplainerBottomSheetPreview() {
    ScrollessTheme(darkTheme = true) {
        AccessibilityExplainerBottomSheet(
            onDismiss = {},
        )
    }
}
