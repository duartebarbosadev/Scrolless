/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.home

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.review.ReviewManagerFactory
import com.maxkeppeler.sheets.duration.DurationSheet
import com.maxkeppeler.sheets.duration.DurationTimeFormat
import com.scrolless.app.R
import com.scrolless.app.base.BaseFragment
import com.scrolless.app.databinding.FragmentHomeBinding
import com.scrolless.app.features.dialogs.AccessibilityExplainerDialog
import com.scrolless.app.features.main.MainActivity
import com.scrolless.app.features.main.MainActivity.Companion.EXTRA_SHOW_ACCESSIBILITY_PERMISSION_GRANTED
import com.scrolless.app.provider.AppProvider
import com.scrolless.app.provider.NavigationProvider
import com.scrolless.app.provider.UsageTracker
import com.scrolless.app.services.ScrollessBlockAccessibilityService
import com.scrolless.framework.core.base.application.CoreConfig
import com.scrolless.framework.extensions.*
import com.scrolless.framework.extensions.isAccessibilityServiceEnabled
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    companion object {
        private const val ARG_ACCESSIBILITY_GRANTED = "accessibilityServiceGranted"

        @JvmStatic
        fun newInstance(accessibilityServiceGranted: Boolean): HomeFragment = HomeFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_ACCESSIBILITY_GRANTED, accessibilityServiceGranted)
            }
        }

        private const val FEATURE_NOT_IMPLEMENTED_ALPHA = 0.5f
    }

    @Inject
    lateinit var appProvider: AppProvider

    private var lastProgress = 0
    private val maxProgress = 100

    @Inject
    lateinit var usageTracker: UsageTracker

    @Inject
    lateinit var navigationProvider: NavigationProvider

    @Inject
    lateinit var appConfig: CoreConfig

    private var progressAnimator: ValueAnimator? = null

    private var backgroundAnimation: AnimationDrawable? = null

    private var serviceEnabledReceiver: BroadcastReceiver? = null

    private var isReceiverRegistered = false

    override fun onViewReady(bundle: Bundle?) {
        val rootView = binding.root
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBarsInsets.top, bottom = systemBarsInsets.bottom)
            insets
        }

        observeFlow(appProvider.blockConfigFlow) { config ->
            updateUIForBlockOption(config.blockOption)
            updateInfoText(
                usageTracker.dailyUsageInMemory,
                config.blockOption == BlockOption.DailyLimit,
                config.timeLimit,
            )

            binding.switchTimerOverlay.isVisible = config.blockOption == BlockOption.DailyLimit
        }

        startGradientAnimation()

        observeFlow(usageTracker.dailyUsageInMemoryFlow) { config ->
            updateInfoText(
                config,
                appProvider.blockConfig.blockOption == BlockOption.DailyLimit,
                appProvider.blockConfig.timeLimit,
            )
        }

//        binding.btnSettings.setOnClickListener {
//            appProvider.totalDailyUsage = 0L
//        }

        binding.blockAllButton.setOnClickListener {
            safeguardedAction {
                val currentConfig = appProvider.blockConfig

                // Toggle selection
                val newBlockOption = if (currentConfig.blockOption == BlockOption.BlockAll) {
                    BlockOption.NothingSelected
                } else {
                    BlockOption.BlockAll
                }

                appProvider.blockConfig = currentConfig.copy(blockOption = newBlockOption)
                updateUIForBlockOption(newBlockOption)
            }
        }

        binding.dailyLimitButton.setOnClickListener {
            safeguardedAction {
                val currentConfig = appProvider.blockConfig

                // Toggle selection
                val newBlockOption = if (currentConfig.blockOption == BlockOption.DailyLimit) {
                    BlockOption.NothingSelected
                } else {
                    BlockOption.DailyLimit
                }

                if (appProvider.blockConfig.timeLimit == 0L) {
                    showDurationPicker(
                        onSuccess = {
                            val updatedConfig =
                                appProvider.blockConfig.copy(blockOption = newBlockOption)
                            appProvider.blockConfig = updatedConfig
                            updateUIForBlockOption(newBlockOption)
                        },
                        onCancel = {
                            // Nothing to do
                        },
                    )
                } else {
                    appProvider.blockConfig = currentConfig.copy(blockOption = newBlockOption)
                    updateUIForBlockOption(newBlockOption)
                }
            }
        }

        binding.pauseButton.apply {
            alpha = FEATURE_NOT_IMPLEMENTED_ALPHA
            setOnClickListener {
                safeguardedAction {
                    showFeatureComingSoonSnackBar()
                }
            }
        }

        binding.intervalTimerButton.apply {
            alpha = FEATURE_NOT_IMPLEMENTED_ALPHA
            setOnClickListener {
                safeguardedAction {
                    showFeatureComingSoonSnackBar()
                }
            }
        }

        binding.configDailyLimitButton.setOnClickListener {
            safeguardedAction {
                showDurationPicker(onSuccess = {}, onCancel = {})
            }
        }

        binding.detailsHelpButton.setOnClickListener {
            showHelpDialog()
        }

        binding.btnRateScrolless.setOnClickListener {
            reviewApp()
        }

//        binding.intervalTimerButton.setOnClickListener {
//            val currentConfig = appProvider.blockConfig
//
//            // Toggle selection
//            val newBlockOption = if (currentConfig.blockOption == BlockOption.IntervalTimer) {
//                BlockOption.NothingSelected
//            } else {
//                BlockOption.IntervalTimer
//            }
//
//            appProvider.blockConfig = currentConfig.copy(blockOption = newBlockOption)
//            updateUIForBlockOption(newBlockOption)
//        }

        setTimerOverlayCheckBoxListener()
        setupProgressIndicator()

        // Handle the argument passed from MainActivity
        val accessibilityServiceGranted =
            arguments?.getBoolean(ARG_ACCESSIBILITY_GRANTED, false) == true
        if (accessibilityServiceGranted) {
            Timber.d("Received argument that accessibility service was granted.")
            showAccessibilitySuccessDialog()
            // IMPORTANT: Consume the argument so it doesn't trigger again on config change/recreation
            arguments?.remove(ARG_ACCESSIBILITY_GRANTED)
        }
    }

    /**
     * Runs an action only if the accessibility service is enabled,
     * otherwise shows the accessibility explainer dialog.
     *
     * @param action The action to run if the accessibility service is enabled.
     */
    private inline fun safeguardedAction(crossinline action: () -> Unit) {
        context?.let { ctx ->
            if (ctx.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
                action()
            } else {
                showAccessibilityExplainerDialog()
            }
        } ?: Timber.w("Context was null during safeguardedAction")
    }

    /**
     * Shows the accessibility explainer dialog to guide the user through enabling
     * the accessibility service required for the app to function properly.
     */
    private fun showAccessibilityExplainerDialog() {
        val dialog = AccessibilityExplainerDialog.newInstance()
        dialog.show(childFragmentManager, AccessibilityExplainerDialog.TAG)

        val permission = ScrollessBlockAccessibilityService.ACTION_ACCESSIBILITY_SERVICE_ENABLE

        // Register the broadcast receiver
        context?.let { context ->
            if (!isReceiverRegistered) {
                if (serviceEnabledReceiver == null) {
                    setupBroadcastReceiver()
                }

                ContextCompat.registerReceiver(
                    context,
                    serviceEnabledReceiver,
                    IntentFilter(ScrollessBlockAccessibilityService.ACTION_ACCESSIBILITY_SERVICE_ENABLE),
                    permission,
                    null,
                    ContextCompat.RECEIVER_EXPORTED,
                )
                isReceiverRegistered = true
                Timber.d("BroadcastReceiver registered.")
            } else {
                Timber.w("BroadcastReceiver was already registered, skipping registration.")
            }
        }
    }

    private fun showFeatureComingSoonSnackBar() {
        Snackbar.make(
            requireView(),
            getString(R.string.feature_coming_soon),
            Snackbar.LENGTH_SHORT,
        ).show()
    }

    /**
     * Setup the progress indicator
     */
    private fun setupProgressIndicator() {
        observeFlow(appProvider.blockConfigFlow) { config ->
            updateProgress()
        }

        // Update progress when resuming
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                updateProgress()
            }
        }
    }

    /**
     * show the duration picker.
     */
    private fun showDurationPicker(onSuccess: () -> Unit, onCancel: () -> Unit) {
        DurationSheet().show(requireContext(), childFragmentManager) {
            title(R.string.duration)
            format(DurationTimeFormat.HH_MM)
            onPositive { durationTimeInSeconds: Long ->
                Timber.d("Duration selected: $durationTimeInSeconds seconds")
                val newTimeLimit = durationTimeInSeconds * 1000
                val updatedConfig = appProvider.blockConfig.copy(
                    timeLimit = newTimeLimit,
                    blockOption = BlockOption.DailyLimit, // Ensure DailyLimit is set when duration picked
                )
                appProvider.blockConfig = updatedConfig
                onSuccess()
            }
            onNegative {
                Timber.d("Duration selection cancelled")
                onCancel()
            }
        }
    }

    /**
     * Updates the progress indicator
     */
    private fun updateProgress() {
        val config = appProvider.blockConfig
        val timeLimit = config.timeLimit

        // If the block option is daily limit, use the daily usage,
        //  otherwise use 0 as there's no time limit associated with the block option
        val currentUsage =
            if (config.blockOption == BlockOption.DailyLimit) usageTracker.dailyUsageInMemory else 0L

        val targetProgress = calculateTargetProgress(currentUsage, timeLimit)
        if (view != null) {
            animateProgressTo(targetProgress)
        }
    }

    /**
     * Calculates the target progress for the progress indicator.
     *
     * @param currentUsage The current usage time in milliseconds.
     * @param timeLimit The time limit in milliseconds.
     * @return The target progress (0-100).
     *
     * If the time limit is greater than 0,
     *  the progress is calculated based on the ratio of current usage to the time limit.
     * If the remaining time (timeLimit - currentUsage) is greater than 0,
     *  the progress is a percentage of the current usage relative to the time limit.
     * If the remaining time is not greater than 0,
     *  it means the limit has been reached or exceeded, so the progress is set to maxProgress.
     * If the time limit is 0 or less, it means there's no limit, so the progress is set to 0.
     */
    private fun calculateTargetProgress(currentUsage: Long, timeLimit: Long): Int =
        if (timeLimit > 0) {
            val remainingTime = timeLimit - currentUsage
            if (remainingTime > 0) {
                // Ensure progress doesn't exceed maxProgress due to floating point inaccuracies
                minOf(
                    maxProgress,
                    ((currentUsage.toDouble() / timeLimit.toDouble()) * maxProgress).toInt(),
                )
            } else { // The limit is reached or exceeded
                maxProgress
            }
        } else {
            0 // No limit set or not applicable block option
        }

    /**
     * Animate the progress indicator
     * @param targetProgress The target progress to animate to.
     * @param duration The duration of the animation.
     */
    private fun animateProgressTo(targetProgress: Int, duration: Long = 1000) {
        // Ensure targetProgress is within bounds
        val clampedTargetProgress = targetProgress.coerceIn(0, maxProgress)

        // Prevent animation if already at target or view is gone
        if (lastProgress == clampedTargetProgress || view == null) {
            if (view == null) Timber.w("View is null, skipping animation to $clampedTargetProgress")
            return
        }

        // Cancel any existing animation
        progressAnimator?.cancel()

        // Create a new animator
        val startProgress = lastProgress
        progressAnimator = ValueAnimator.ofInt(startProgress, clampedTargetProgress).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                // Check view again inside listener in case fragment is detached during animation
                if (view == null) {
                    animator.cancel()
                    return@addUpdateListener
                }
                val progress = animator.animatedValue as Int
                binding.circleProgress.progress = progress
                updateProgressIndicatorColor(progress)
                lastProgress = progress
            }
            start()
        }
    }

    /**
     * Updates the color of the progress indicator based on the current progress.
     *
     * @param progress The current progress value (0-100).
     */
    private fun updateProgressIndicatorColor(progress: Int) {
        // Ensure fragment is attached and context is available
        context?.let { ctx ->
            binding.circleProgress.setIndicatorColor(calculateColorForProgress(ctx, progress))
        } ?: Timber.w("Context null in updateProgressIndicatorColor")
    }

    /**
     *
     * Calculates the color for the progress indicator based on the current progress.
     * @param context Context to resolve colors.
     * @param progress The current progress value (0-100).
     * @return The color to be used for the progress indicator.
     */
    private fun calculateColorForProgress(context: Context, progress: Int): Int {
        val startColor = ContextCompat.getColor(context, R.color.green)
        val middleColor = ContextCompat.getColor(context, R.color.orangeDark)
        val endColor = ContextCompat.getColor(context, R.color.red)

        val middleProgress = 75f // Use float for ratio calculation

        return when {
            progress < middleProgress -> blendColors(
                startColor,
                middleColor,
                progress / middleProgress,
            )

            else -> blendColors(
                middleColor,
                endColor,
                // Ensure ratio doesn't divide by zero or go > 1 if progress is exactly 100 or middleProgress
                if (maxProgress - middleProgress <= 0) {
                    1f
                } else {
                    ((progress - middleProgress) / (maxProgress - middleProgress)).coerceIn(
                        0f,
                        1f,
                    )
                },
            )
        }
    }

    /**
     * Blends two colors together based on a given ratio.
     *
     * @param color1 The first color.
     * @param color2 The second color.
     * @param ratio The ratio of the second color to blend (0.0-1.0).
     * @return The blended color.
     */
    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r =
            (android.graphics.Color.red(color1) * inverseRatio + android.graphics.Color.red(color2) * ratio)
        val g = (
            android.graphics.Color.green(color1) * inverseRatio + android.graphics.Color.green(
                color2,
            ) * ratio
            )
        val b =
            (android.graphics.Color.blue(color1) * inverseRatio + android.graphics.Color.blue(color2) * ratio)
        val a = (
            android.graphics.Color.alpha(color1) * inverseRatio + android.graphics.Color.alpha(
                color2,
            ) * ratio
            ) // Blend alpha too
        return android.graphics.Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
    }

    /**
     * Start the gradient animation.
     */
    private fun startGradientAnimation() {
        val layout = binding.layout
        backgroundAnimation = layout.background as AnimationDrawable
        backgroundAnimation?.apply {
            setEnterFadeDuration(6000)
            setExitFadeDuration(2000)
            start()
        }
    }

    /**
     * Sets up the listener for the timer overlay checkbox.
     *
     * Initializes the checkbox state from appProvider.timerOverlayEnabled
     *  and updates it when the checkbox is toggled.
     */
    private fun setTimerOverlayCheckBoxListener() {
        // Initialize state only once, maybe check if binding is already initialized if needed
        binding.switchTimerOverlay.isChecked = appProvider.timerOverlayEnabled

        binding.switchTimerOverlay.setOnCheckedChangeListener { _, isChecked ->
            appProvider.timerOverlayEnabled = isChecked
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure animation restarts only if it exists and wasn't already running
        if (backgroundAnimation?.isRunning == false) {
            backgroundAnimation?.start()
        }
        // Re-check service status in case it was disabled while paused
        context?.let {
            if (!it.isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
                // Optional: Maybe show a warning or disable controls if service disabled externally
                Timber.w("Accessibility service seems disabled onResume.")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Check if animation exists before stopping
        backgroundAnimation?.stop()
    }

    /**
     * Reset the buttons.
     */
    private fun resetButtons() {
        binding.configDailyLimitButton.beGone()
        applyButtonEffect(binding.blockAllButton, false)
        applyButtonEffect(binding.dailyLimitButton, false)
        applyButtonEffect(binding.pauseButton, false)
        applyButtonEffect(binding.intervalTimerButton, false)
    }

    /**
     * Apply the button effect.
     * @param button The button to apply the effect to.
     * @param activated True if the button is activated, false otherwise.
     */

    private fun applyButtonEffect(
        button: MaterialButton,
        activated: Boolean
    ) {
        button.apply {
            if (activated) {
                strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke)
                strokeColor = ColorStateList.valueOf(
                    context.getColorFromAttr(androidx.appcompat.R.attr.colorPrimary),
                )
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.color_background_dark,
                    ),
                )
            } else {
                strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke)
                strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.gray_600,
                    ),
                )
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        R.color.color_background_darker,
                    ),
                )
            }
        }
    }

    override fun onDestroyView() {
        // Cancel the progress animator if it's running
        progressAnimator?.removeAllUpdateListeners()
        progressAnimator?.cancel()
        progressAnimator = null

        // Stop the background animation if it's running
        backgroundAnimation?.stop()
        backgroundAnimation = null

        // Unregister receiver
        context?.let { ctx ->

            if (isReceiverRegistered && serviceEnabledReceiver != null) {
                Timber.d("BroadcastReceiver is registered, attempting to unregister.")
                try {
                    ctx.unregisterReceiver(serviceEnabledReceiver)
                    Timber.d("BroadcastReceiver unregistered.")
                } catch (e: IllegalArgumentException) {
                    // Receiver might have already been unregistered or never registered
                    Timber.w("Error unregistering receiver: ${e.message}")
                } finally {
                    isReceiverRegistered = false // Reset flag
                }
            }
        }

        serviceEnabledReceiver = null // Clear reference
        super.onDestroyView()
    }

    /**
     * Updates the info text.
     * @param totalDailyUsage The total daily usage in milliseconds.
     * @param showTimeLimit True if the time limit should be shown, false otherwise.
     * @param timeLimit The time limit in milliseconds.
     */
    private fun updateInfoText(totalDailyUsage: Long, showTimeLimit: Boolean, timeLimit: Long) {
        if (showTimeLimit) {
            binding.trackTime.text = getString(
                R.string.time_track_limit,
                totalDailyUsage.formatTime(),
                timeLimit.formatTime(),
            )
        } else {
            binding.trackTime.text = getString(
                R.string.time_track,
                totalDailyUsage.formatTime(),
            )
        }
    }

    /**
     * Update the UI for the block option.
     * @param blockOption The block option.
     */
    private fun updateUIForBlockOption(blockOption: BlockOption) {
        resetButtons()
        when (blockOption) {
            BlockOption.BlockAll -> applyButtonEffect(binding.blockAllButton, true)
            BlockOption.DailyLimit -> {
                binding.configDailyLimitButton.visibility = View.VISIBLE
                applyButtonEffect(binding.dailyLimitButton, true)
            }

            BlockOption.IntervalTimer -> applyButtonEffect(binding.intervalTimerButton, true)
            BlockOption.NothingSelected -> Unit // No action needed
        }
    }

    /**
     * Shows the help dialog with instructions on how to use the app
     */
    private fun showHelpDialog() {
        navigationProvider.launchHelpDialog(childFragmentManager)
    }

    /**
     * Show the app review dialog.
     * If app is in dev mode, it will open the Play Store.
     * Otherwise it will show the review dialog.
     */
    private fun reviewApp() {
        // If app is in dev mode, open the play store instead
        if (appConfig.isDev()) {
            Timber.d("Debug mode, skipping review flow, opening Play Store instead")
            openPlayStore()
            return
        }

        showReviewPopup()
    }

    /**
     * Show the review popup.
     *
     * If the review manager fails (as itself as api quotas limits), it will open the PlayStore url
     */
    private fun showReviewPopup() {
        val reviewManager = ReviewManagerFactory.create(requireContext())

        Timber.d("Requesting review flow")
        reviewManager.requestReviewFlow().addOnCompleteListener { request ->

            if (request.isSuccessful) {
                Timber.d("Review flow request successful")
                val reviewInfo = request.result
                val flow = reviewManager.launchReviewFlow(requireActivity(), reviewInfo)
                flow.addOnCompleteListener { _ ->
                    Timber.d("Review flow completed")
                }
            } else {
                // If the request failed, open the Play Store instead
                Timber.d("Review flow request failed")
                openPlayStore()
            }
        }
    }

    /**
     * Open the Play Store page for the app.
     */
    private fun openPlayStore() {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = appConfig.getPlayStoreUrl(requireContext())?.toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    /**
     * This method sets up a broadcast receiver to listen for the event when the accessibility service is enabled.
     */
    private fun setupBroadcastReceiver() {
        serviceEnabledReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Timber.d("Accessibility service enabled, reopening app to front.")
                if (intent?.action == ScrollessBlockAccessibilityService.ACTION_ACCESSIBILITY_SERVICE_ENABLE) {
                    context?.let {
                        // Launch MainActivity to bring app to foreground
                        val mainIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(EXTRA_SHOW_ACCESSIBILITY_PERMISSION_GRANTED, true)
                        }
                        it.startActivity(mainIntent)
                    }
                }
            }
        }
    }

    /**
     * Shows a bottom sheet dialog celebrating successful setup of accessibility permissions
     * and providing guidance on next steps.
     */
    private fun showAccessibilitySuccessDialog() {
        navigationProvider.launchAccessibilityGrantedDialog(childFragmentManager)
    }
}
