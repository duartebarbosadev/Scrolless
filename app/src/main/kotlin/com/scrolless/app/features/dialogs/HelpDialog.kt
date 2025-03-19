/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.scrolless.app.R
import com.scrolless.app.databinding.DialogHelpBinding
import com.scrolless.app.databinding.ItemHelpStepBinding
import com.scrolless.app.features.dialogs.AccessibilityExplainerDialog.Companion.GITHUB_URL
import androidx.core.net.toUri

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
        val step1Binding = ItemHelpStepBinding.bind(binding.helpStepsContainer.findViewById(R.id.step_accessibility))
        with(step1Binding) {
            stepNumber.text = context.getString(R.string.step_one)
            stepTitle.text = context.getString(R.string.help_step1_title)
            stepDescription.text = context.getString(R.string.help_step1_description)
        }

        // Setup Step 2: Choose Block Option
        val step2Binding = ItemHelpStepBinding.bind(binding.helpStepsContainer.findViewById(R.id.step_block_option))
        with(step2Binding) {
            stepNumber.text = context.getString(R.string.step_two)
            stepTitle.text = context.getString(R.string.help_step2_title)
            stepDescription.text = context.getString(R.string.help_step2_description)
        }

        // Setup Step 3: Troubleshooting
        val step3Binding = ItemHelpStepBinding.bind(binding.helpStepsContainer.findViewById(R.id.step_troubleshooting))
        with(step3Binding) {
            stepNumber.text = context.getString(R.string.step_three)
            stepTitle.text = context.getString(R.string.help_step3_title)
            stepDescription.text = context.getString(R.string.help_step3_description)
            root.setOnClickListener {

                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        data = GITHUB_URL.toUri()
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
