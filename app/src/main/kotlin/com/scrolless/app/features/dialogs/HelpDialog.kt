/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat.getString
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.scrolless.app.R
import com.scrolless.app.databinding.DialogHelpBinding

/**
 * Dialog that explains how to use the app and troubleshoot common issues
 */
class HelpDialog(
    private val context: Context,
    private val onClose: () -> Unit = {}
) {
    private lateinit var dialog: Dialog
    private lateinit var binding: DialogHelpBinding

    fun show() {
        val inflater = LayoutInflater.from(context)
        binding = DialogHelpBinding.inflate(inflater)

        setupSteps()
        setupButtons()

        dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
        }

        dialog.show()
    }

    /**
     * Method to set up the steps in the help dialog
     */
    private fun setupSteps() {
        // Setup Step 1: Enable Accessibility
        // Setup the GitHub card button
        binding.cardGithub.findViewById<View>(R.id.card_github)?.apply {
            setOnClickListener {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        data = context.getString(R.string.github_url).toUri()
                    },
                )
            }
        }
    }

    /**
     * Method to set up the buttons in the help dialog
     */
    private fun setupButtons() {
        binding.btnGoToAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
            dialog.dismiss()
            onClose()
        }

        binding.btnCloseHelp.setOnClickListener {
            dialog.dismiss()
            onClose()
        }
    }
}
