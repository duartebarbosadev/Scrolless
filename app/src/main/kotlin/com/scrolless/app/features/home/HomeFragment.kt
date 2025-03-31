/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.home

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
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
        @JvmStatic
        fun newInstance() = HomeFragment()

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
    }

    /**
     * Runs an action only if the accessibility service is enabled,
     * otherwise shows the accessibility explainer dialog.
     *
     * @param action The action to run if the accessibility service is enabled.
     */
    private inline fun safeguardedAction(crossinline action: () -> Unit) {
        if (requireContext().isAccessibilityServiceEnabled(ScrollessBlockAccessibilityService::class.java)) {
            action()
        } else {
            showAccessibilityExplainerDialog()
        }
    }

    /**
     * Shows the accessibility explainer dialog to guide the user through enabling
     * the accessibility service required for the app to function properly.
     */
    private fun showAccessibilityExplainerDialog() {
        val dialog = AccessibilityExplainerDialog.newInstance()
        dialog.show(childFragmentManager, AccessibilityExplainerDialog.TAG)
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
                val newTimeLimit = durationTimeInSeconds * 1000
                val updatedConfig = appProvider.blockConfig.copy(
                    timeLimit = newTimeLimit,
                    blockOption = BlockOption.DailyLimit,
                )
                appProvider.blockConfig = updatedConfig
                onSuccess()
            }
            onNegative { onCancel() }
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
            if (appProvider.blockConfig.blockOption == BlockOption.DailyLimit) usageTracker.dailyUsageInMemory else 0L

        val targetProgress = calculateTargetProgress(currentUsage, timeLimit)
        animateProgressTo(targetProgress)
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
                ((currentUsage.toDouble() / timeLimit.toDouble()) * maxProgress).toInt()
            } else { // The limit is reached or exceeded
                maxProgress
            }
        } else {
            0
        }

    /**
     * Animate the progress indicator
     * @param targetProgress The target progress to animate to.
     * @param duration The duration of the animation.
     */

    private fun animateProgressTo(targetProgress: Int, duration: Long = 1000) {
        val startProgress = lastProgress
        val valueAnimator = ValueAnimator.ofInt(startProgress, targetProgress)
        valueAnimator.apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
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
        binding.circleProgress.setIndicatorColor(calculateColorForProgress(progress))
    }

    /**
     *
     * Calculates the color for the progress indicator based on the current progress.
     *
     * @param progress The current progress value (0-100).
     * @return The color to be used for the progress indicator.
     */
    private fun calculateColorForProgress(progress: Int): Int {
        val startColor = ContextCompat.getColor(requireContext(), R.color.green)
        val middleColor = ContextCompat.getColor(requireContext(), R.color.orangeDark)
        val endColor = ContextCompat.getColor(requireContext(), R.color.red)

        val middleProgress = 75

        return when {
            progress < middleProgress -> blendColors(
                startColor,
                middleColor,
                progress / middleProgress.toFloat(),
            )

            else -> blendColors(
                middleColor,
                endColor,
                (progress - middleProgress) / (100f - middleProgress),
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
    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int =
        android.graphics.Color.rgb(
            (android.graphics.Color.red(color1) * (1 - ratio) + android.graphics.Color.red(color2) * ratio).toInt(),
            (
                android.graphics.Color.green(color1) * (1 - ratio) + android.graphics.Color.green(
                    color2,
                ) * ratio
                ).toInt(),
            (android.graphics.Color.blue(color1) * (1 - ratio) + android.graphics.Color.blue(color2) * ratio).toInt(),
        )

    /**
     * Start the gradient animation.
     */
    private fun startGradientAnimation() {
        val layout = binding.layout
        val animationDrawable = layout.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(6000)
        animationDrawable.setExitFadeDuration(2000)
        animationDrawable.start()
    }

    /**
     * Sets up the listener for the timer overlay checkbox.
     *
     * Initializes the checkbox state from appProvider.timerOverlayEnabled
     *  and updates it when the checkbox is toggled.
     */
    private fun setTimerOverlayCheckBoxListener() {
        binding.switchTimerOverlay.isChecked = appProvider.timerOverlayEnabled

        binding.switchTimerOverlay.setOnCheckedChangeListener { _, isChecked ->
            appProvider.timerOverlayEnabled = isChecked
        }
    }

    override fun onResume() {
        super.onResume()
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
        val reviewManager =
            ReviewManagerFactory.create(requireContext())

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
}
