/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.dialogs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.scrolless.app.R
import com.scrolless.app.databinding.DialogAccessibilityExplainerBinding
import com.scrolless.app.features.main.MainActivity
import com.scrolless.app.services.ScrollessBlockAccessibilityService
import com.scrolless.framework.extensions.isAccessibilityServiceEnabled
import com.scrolless.framework.extensions.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

/**
 * A dialog that explains to the user why accessibility permissions are needed
 * and guides them through the process of enabling them.
 */
@AndroidEntryPoint
class AccessibilityExplainerDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAccessibilityExplainerBinding? = null
    private val binding get() = _binding!!

    // Keep track of animation handlers to prevent memory leaks
    private val handlers = mutableListOf<Handler>()

    private var serviceEnabledReceiver: BroadcastReceiver? = null

    // Flag to ensure animations only run once
    private var animationsApplied = false

    companion object {
        const val TAG = "AccessibilityExplainerDialog"

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

        // Setup broadcast receiver to dismiss dialog when service is enabled
        setupBroadcastReceiver()
    }

    /**
     * This method sets up a broadcast receiver to listen for the event when the accessibility service is enabled.
     */
    private fun setupBroadcastReceiver() {
        serviceEnabledReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Timber.d("Accessibility service enabled, reopening app to front.")
                context?.let {
                    // Launch MainActivity to bring app to foreground
                    val mainIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(mainIntent)
                    dismissAllowingStateLoss()
                }
            }
        }

        // Register the broadcast receiver
        context?.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(
                serviceEnabledReceiver!!,
                IntentFilter(ScrollessBlockAccessibilityService.ACTION_ACCESSIBILITY_SERVICE_ENABLE)
            )
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
            // Don't dismiss here, we'll wait for the broadcast or onResume check
        }

        binding.btnNotNow.setOnClickListener {
            dismiss()
        }

        // Add GitHub link
        binding.tvOpenSourceNote.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = getString(R.string.github_url).toUri()
                },
            )
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

        // Unregister broadcast receiver
        serviceEnabledReceiver?.let { receiver ->
            context?.let { ctx ->
                LocalBroadcastManager.getInstance(ctx).unregisterReceiver(receiver)
            }
        }

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
