/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.scrolless.app.R
import com.scrolless.app.databinding.DialogAccessibilityExplainerBinding
import com.scrolless.app.services.ScrollessBlockAccessibilityService
import com.scrolless.framework.extensions.isAccessibilityServiceEnabled
import com.scrolless.framework.extensions.showToast
import androidx.core.net.toUri

/**
 * A dialog that explains to the user why accessibility permissions are needed
 * and guides them through the process of enabling them.
 */
class AccessibilityExplainerDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAccessibilityExplainerBinding? = null
    private val binding get() = _binding!!

    // Keep track of animation handlers to prevent memory leaks
    private val handlers = mutableListOf<Handler>()

    // Flag to ensure animations only run once
    private var animationsApplied = false

    companion object {
        const val TAG = "AccessibilityExplainerDialog"
        const val GITHUB_URL = "https://github.com/duartebarbosadev/scrolless"

        fun newInstance(): AccessibilityExplainerDialog = AccessibilityExplainerDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAccessibilityExplainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()

        // Only run animations if they haven't been applied
        // and there's no saved instance state (first creation)
        if (!animationsApplied && savedInstanceState == null) {
            setupAnimations()
            animationsApplied = true
        } else {
            // If we're restoring state, make everything visible without animation
            makeAllElementsVisible()
        }
    }

    private fun makeAllElementsVisible() {
        binding.apply {
            cardContent.alpha = 1f
            cardContent.translationY = 0f
            step1Container.alpha = 1f
            step2Container.alpha = 1f
            step3Container.alpha = 1f
            privacyContainer.alpha = 1f
            tvOpenSourceNote.alpha = 1f
            btnProceed.alpha = 1f
            btnNotNow.alpha = 1f
        }
    }

    private fun setupUI() {
        // Setup buttons
        binding.btnProceed.setOnClickListener {
            openAccessibilitySettings()
            dismiss()
        }

        binding.btnNotNow.setOnClickListener {
            dismiss()
        }

        // Add GitHub link
        binding.tvOpenSourceNote.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri())
            startActivity(intent)
        }
    }

    private fun setupAnimations() {
        // Animate the icon container with elevation and pulse
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation)
        binding.iconContainer.startAnimation(pulseAnimation)
        binding.imgLogo.startAnimation(pulseAnimation)

        // Animate content card with slight float animation
        binding.cardContent.apply {
            alpha = 0f
            translationY = 50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Animate steps with a staggered entry
        animateWithDelay(binding.step1Container, 200)
        animateWithDelay(binding.step2Container, 400)
        animateWithDelay(binding.step3Container, 600)
        animateWithDelay(binding.privacyContainer, 800)
        animateWithDelay(binding.tvOpenSourceNote, 900)

        // Button animations
        animateWithDelay(binding.btnProceed, 1000)
        animateWithDelay(binding.btnNotNow, 1100)
    }

    private fun animateWithDelay(view: View, delay: Long) {
        view.alpha = 0f
        val handler = Handler(Looper.getMainLooper())
        handlers.add(handler)

        handler.postDelayed({
            if (isAdded && view.isAttachedToWindow) {
                val fadeInAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_slow)
                view.alpha = 1f
                view.startAnimation(fadeInAnimation)
            }
        }, delay)
    }

    override fun onResume() {
        super.onResume()
        // Check if the service was enabled while in settings
        if (requireContext().isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
            dismiss()
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        requireContext().startActivity(intent)
        requireContext().showToast(getString(R.string.accessibility_settings_toast))
    }

    override fun onDestroyView() {
        // Clear all animation handlers to prevent memory leaks
        handlers.forEach { it.removeCallbacksAndMessages(null) }
        handlers.clear()

        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save that animations have been applied
        outState.putBoolean("animationsApplied", true)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            animationsApplied = savedInstanceState.getBoolean("animationsApplied", false)
        }
    }
}
