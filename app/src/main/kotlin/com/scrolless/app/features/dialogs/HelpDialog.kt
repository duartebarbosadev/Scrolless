/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.dialogs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import com.scrolless.app.R
import com.scrolless.app.databinding.DialogHelpBinding
import com.scrolless.framework.extensions.showToast
import timber.log.Timber

/**
 * DialogFragment that explains how to use the app and troubleshoot common issues.
 */
class HelpDialog : DialogFragment() {

    private var _binding: DialogHelpBinding? = null
    private val binding get() = _binding!! // Only valid between onCreateView and onDestroyView

    companion object {
        const val TAG = "HelpDialog"

        /**
         * Factory method to create a new instance of this dialog fragment.
         */
        fun newInstance(): HelpDialog = HelpDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogHelpBinding.inflate(inflater, container, false)

        // Make the dialog window background transparent to show the rounded corners
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply entrance animation to the root view
        val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        view.startAnimation(slideUpAnimation)

        setupButtonInteractions()
    }

    override fun onStart() {
        super.onStart()
        // Set dialog dimensions
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    /**
     * Sets up click listeners for interactive elements within the main content area (e.g., links).
     */
    private fun setupButtonInteractions() {
        // Setup the GitHub card click listener
        binding.cardGithub.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = getString(R.string.github_url).toUri()
                }
                requireContext().startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Timber.w(e, "Could not open GitHub link. No browser found?")
                requireContext().showToast(getString(R.string.error_unknown))
            }
        }

        // Button to navigate to Accessibility Settings
        binding.btnGoToAccessibility.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                requireContext().startActivity(intent)
                dismiss()
            } catch (e: ActivityNotFoundException) {
                Timber.w(e, "Could not open Accessibility Settings.")
                requireContext().showToast(getString(R.string.error_accessibility_settings_unavailable))
            }
        }

        // Button to simply close the dialog
        binding.btnCloseHelp.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Avoid memory leaks
    }
}
