/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.dialogs

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.scrolless.app.R
import com.scrolless.app.databinding.DialogAccessibilitySuccessBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * A dialog that celebrates the successful enabling of accessibility permissions
 * and guides the user on how to begin using the app.
 */
@AndroidEntryPoint
class AccessibilitySuccessDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAccessibilitySuccessBinding? = null
    private val binding get() = _binding!!

    // Keep track of animation handlers to prevent memory leaks
    private val handlers = mutableListOf<Handler>()

    // Flag to ensure animations only run once
    private var animationsApplied = false

    companion object {
        const val TAG = "AccessibilitySuccessDialog"

        fun newInstance(): AccessibilitySuccessDialog = AccessibilitySuccessDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAccessibilitySuccessBinding.inflate(inflater, container, false)
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
            nextStepsContainer.alpha = 1f
            btnGetStarted.alpha = 1f
        }
    }

    private fun setupUI() {
        // Setup button
        binding.btnGetStarted.setOnClickListener {
            dismiss()
        }
    }

    private fun setupAnimations() {
        // Animate the icon with success check animation
        val checkAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.check_bounce_animation)
        binding.imgSuccess.startAnimation(checkAnimation)

        // Animate the icon container with pulse
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation)
        binding.iconContainer.startAnimation(pulseAnimation)

        // Animate content card with slight float animation
        binding.cardContent.apply {
            alpha = 0f
            translationY = 50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Animate next steps with a staggered entry
        animateWithDelay(binding.nextStepsContainer, 800)

        // Button animation
        animateWithDelay(binding.btnGetStarted, 1000)
    }

    private fun animateWithDelay(view: View, delay: Long) {
        view.alpha = 0f
        val handler = Handler(Looper.getMainLooper())
        handlers.add(handler)

        handler.postDelayed(
            {
                if (isAdded && view.isAttachedToWindow) {
                    val fadeInAnimation =
                        AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_slow)
                    view.alpha = 1f
                    view.startAnimation(fadeInAnimation)
                }
            },
            delay,
        )
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
