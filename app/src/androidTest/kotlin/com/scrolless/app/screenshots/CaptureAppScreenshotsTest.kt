/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.screenshots

import android.Manifest
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.scrolless.app.R
import com.scrolless.app.features.main.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Test class for capturing screenshots of various screens in the app.
 * This class is used by the GitHub Actions workflow to automatically update app screenshots.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CaptureAppScreenshotsTest {

    companion object {
        private const val TAG = "ScreenshotTest"
        private const val SCREENSHOTS_DIRECTORY = "screenshots"
    }

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private lateinit var context: Context
    private lateinit var instrumentation: Instrumentation
    private lateinit var uiDevice: UiDevice
    private lateinit var screenshotsDir: File

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        instrumentation = InstrumentationRegistry.getInstrumentation()
        uiDevice = UiDevice.getInstance(instrumentation)
        
        // Create screenshot directory in app's data directory
        screenshotsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), SCREENSHOTS_DIRECTORY)
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs()
        }
        
        // Also create a directory in the build output
        val buildOutputDir = File(instrumentation.targetContext.dataDir.parentFile, "outputs/screenshots/debug")
        if (!buildOutputDir.exists()) {
            buildOutputDir.mkdirs()
        }
        
        Log.d(TAG, "Screenshot directory: ${screenshotsDir.absolutePath}")
        Log.d(TAG, "Build output directory: ${buildOutputDir.absolutePath}")
    }

    @Test
    fun captureHomeScreen() {
        // Wait for the main activity to fully render
        Thread.sleep(1000)
        
        // Capture screenshot of home screen
        takeScreenshot("home_screen")
    }
    
    @Test
    fun captureAccessibilityExplainerDialog() {
        // Wait for the main activity to fully render
        Thread.sleep(1000)
        
        // Click on one of the buttons that would trigger the accessibility dialog
        try {
            // Attempt to click the Block All button which should trigger the dialog
            onView(withId(R.id.blockAllButton)).perform(click())
            
            // Wait for dialog to appear
            Thread.sleep(500)
            
            // Take screenshot of the dialog
            takeScreenshot("accessibility_dialog")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture accessibility dialog: ${e.message}", e)
        }
    }

    /**
     * Takes a screenshot and saves it to both the app's external storage and the build output directory.
     *
     * @param fileName Name to use for the screenshot file (without extension)
     */
    private fun takeScreenshot(fileName: String) {
        try {
            // Take the screenshot using UiDevice
            val screenshot = uiDevice.takeScreenshot()
            
            // Save to app's external storage
            val appFile = File(screenshotsDir, "$fileName.png")
            FileOutputStream(appFile).use { stream ->
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }
            
            // Save to build output directory for GitHub Actions
            val buildOutputDir = File(instrumentation.targetContext.dataDir.parentFile, "outputs/screenshots/debug")
            val buildOutputFile = File(buildOutputDir, "$fileName.png")
            FileOutputStream(buildOutputFile).use { stream ->
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
            }
            
            Log.d(TAG, "Screenshot saved: $fileName.png")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to take screenshot: ${e.message}", e)
        }
    }
}