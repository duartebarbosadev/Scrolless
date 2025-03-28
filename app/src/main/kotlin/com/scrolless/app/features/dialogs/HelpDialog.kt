/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.dialogs

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.scrolless.app.R
import com.scrolless.app.databinding.DialogHelpBinding
import timber.log.Timber

/**
 * Dialog that explains how to use the app and troubleshoot common issues.
 * Uses MaterialAlertDialogBuilder and ViewBinding.
 *
 * @param context The context used for inflating views, building the dialog, and starting activities.
 * @param onClose A lambda function invoked when the dialog is dismissed via its action buttons.
 */
class HelpDialog(
    private val context: Context,
    private val onClose: () -> Unit = {}
) {
    private lateinit var dialog: Dialog
    private lateinit var binding: DialogHelpBinding

    /**
     * Inflates the layout, sets up interactions, builds, styles, and shows the dialog.
     */
    fun show() {
        // Inflate the view using View Binding
        binding = DialogHelpBinding.inflate(LayoutInflater.from(context))

        // Setup interactive elements within the dialog content
        setupContentInteractions()
        // Setup the main action buttons
        setupActionButtons()

        // Build the dialog using MaterialAlertDialogBuilder
        dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setCancelable(true) // Allows dismissing by tapping outside or back button
            .create()

        // Apply custom window styling
        dialog.window?.apply {
            // Make the dialog window background transparent to show the rounded corners of the root view
            setBackgroundDrawableResource(android.R.color.transparent)
            // Set dialog dimensions (match parent width, wrap content height)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        }

        // Display the dialog
        dialog.show()
    }

    /**
     * Sets up click listeners for interactive elements within the main content area (e.g., links).
     */
    private fun setupContentInteractions() {
        // Setup the GitHub card click listener
        binding.cardGithub.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = context.getString(R.string.github_url).toUri()
                }
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Handle case where no browser is available
                Timber.d("Error opening GitHub link: ${e.message}")
            }
        }
    }

    /**
     * Sets up click listeners for the main action buttons at the bottom of the dialog.
     */
    private fun setupActionButtons() {
        // Button to navigate to Accessibility Settings
        binding.btnGoToAccessibility.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
                dialog.dismiss() // Dismiss the dialog after starting activity
                onClose() // Notify caller
            } catch (e: ActivityNotFoundException) {
                // Handle rare case where Accessibility Settings screen is not found
                Timber.d("Error opening GitHub link: ${e.message}")
            }
        }

        // Button to simply close the dialog
        binding.btnCloseHelp.setOnClickListener {
            dialog.dismiss() // Dismiss the dialog
            onClose() // Notify caller
        }
    }
}
