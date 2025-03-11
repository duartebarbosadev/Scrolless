/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.screenshots

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import com.github.takahirom.roborazzi.captureRoboImage
import com.scrolless.app.features.home.HomeScreen
import com.scrolless.app.features.accessibility.AccessibilityExplainerDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config

/**
 * Roborazzi screenshot tests for the app.
 * This class captures screenshots of UI components for documentation and regression testing.
 * 
 * Use the Gradle tasks:
 * - `./gradlew verifyRoborazziDebug` to verify screenshots match
 * - `./gradlew recordRoborazziDebug` to update the screenshot references
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xxhdpi") // Add a consistent screen density for tests
class RoborazziScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreen() {
        composeRule.setContent {
            HomeScreen(
                onNavigateToDetails = {},
                onShowAccessibilityDialog = {}
            )
        }
        
        // Wait for content to be rendered
        composeRule.waitForIdle()
        
        composeRule.captureRoboImage("home_screen")
    }
    
    @Test
    fun accessibilityDialog() {
        composeRule.setContent {
            // Make sure the dialog is properly displayed by wrapping it with a theme if needed
            androidx.compose.material.MaterialTheme {
                AccessibilityExplainerDialog(
                    onDismiss = {},
                    onConfirm = {}
                )
            }
        }
        
        // Wait for dialog to be fully rendered
        composeRule.waitForIdle()
        
        // Alternative method if the standard captureRoboImage doesn't work well with dialogs
        composeRule.captureRoboImage("accessibility_dialog")
    }
}
