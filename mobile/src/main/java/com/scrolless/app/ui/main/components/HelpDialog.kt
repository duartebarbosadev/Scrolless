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
package com.scrolless.app.ui.main.components

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.scrolless.app.R
import com.scrolless.app.ui.theme.ScrollessTheme
import com.scrolless.app.ui.tooling.DevicePreviews
import timber.log.Timber

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        Timber.d("HelpDialog: show")
    }
    Dialog(
        onDismissRequest = {
            Timber.d("HelpDialog: dismiss request")
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header
                Text(
                    text = stringResource(R.string.help_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(
                    modifier = Modifier.alpha(0.5f),
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Step 1
                HelpStep(
                    stepNumber = "1",
                    title = stringResource(R.string.help_step1_title),
                    description = stringResource(R.string.help_step1_description),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Step 2
                HelpStep(
                    stepNumber = "2",
                    title = stringResource(R.string.help_step2_title),
                    description = stringResource(R.string.help_step2_description),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Step 3
                HelpStep(
                    stepNumber = "3",
                    title = stringResource(R.string.help_step3_description),
                    description = stringResource(R.string.help_step3_description),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // GitHub Card
                GitHubCard()

                Spacer(modifier = Modifier.height(8.dp))

                // Icons8 Attribution
                Text(
                    text = stringResource(R.string.icons8_attribution),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    modifier = Modifier.alpha(0.7f),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons
                val context = LocalContext.current

                TextButton(
                    onClick = {
                        try {
                            Timber.i("HelpDialog: open accessibility settings")
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                            onDismiss()
                        } catch (e: Exception) {
                            Timber.e(e, "HelpDialog: failed to open accessibility settings")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.go_to_accessibility_settings),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                TextButton(
                    onClick = {
                        Timber.d("HelpDialog: close clicked")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.close),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun HelpStep(stepNumber: String, title: String, description: String) {
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
            verticalAlignment = Alignment.Top,
        ) {
            // Step Number Circle
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 18.sp,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Step Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun GitHubCard() {
    val context = LocalContext.current
    val githubUrl = stringResource(R.string.github_url)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    Timber.i("HelpDialog: open GitHub link")
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = githubUrl.toUri()
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "HelpDialog: failed to open GitHub link")
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_github),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(R.string.visit_github),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Suppress("FunctionNaming")
@DevicePreviews
@Composable
fun HelpDialogPreview() {
    ScrollessTheme {
        HelpDialog(onDismiss = {})
    }
}
