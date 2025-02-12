/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.home

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import com.maxkeppeler.sheets.duration.DurationSheet
import com.maxkeppeler.sheets.duration.DurationTimeFormat
import com.scrolless.app.R
import com.scrolless.app.base.BaseFragment
import com.scrolless.app.databinding.FragmentHomeBinding
import com.scrolless.app.provider.AppProvider
import com.scrolless.app.provider.UsageTracker
import com.scrolless.app.services.ScrollessBlockAccessibilityService
import com.scrolless.framework.extensions.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    @Inject
    lateinit var appProvider: AppProvider

    @Inject
    lateinit var usageTracker: UsageTracker

    override fun onViewReady(bundle: Bundle?) {
        observeFlow(appProvider.blockConfigFlow) { config ->
            updateUIForBlockOption(config.blockOption)
            updateInfoText(usageTracker.dailyUsageInMemory, config.timeLimit)
        }

        observeFlow(usageTracker.dailyUsageInMemoryFlow) { config ->
            updateInfoText(config, appProvider.blockConfig.timeLimit)
        }

        binding.btnSettings.setOnClickListener {
            appProvider.totalDailyUsage = 0L
        }

        binding.blockAllButton.setOnClickListener {
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

        binding.dayLimitButton.setOnClickListener {
            val currentConfig = appProvider.blockConfig

            // Toggle selection
            val newBlockOption = if (currentConfig.blockOption == BlockOption.DayLimit) {
                BlockOption.NothingSelected
            } else {
                BlockOption.DayLimit
            }

            appProvider.blockConfig = currentConfig.copy(blockOption = newBlockOption)
            updateUIForBlockOption(newBlockOption)

            if (newBlockOption == BlockOption.DayLimit) {
                DurationSheet().show(requireContext(), childFragmentManager) {
                    title(R.string.duration)
                    format(DurationTimeFormat.HH_MM)
                    onPositive { durationTimeInSeconds: Long ->
                        val newTimeLimit = durationTimeInSeconds * 1000
                        val updatedConfig = appProvider.blockConfig.copy(timeLimit = newTimeLimit)
                        appProvider.blockConfig = updatedConfig
                    }
                }
            }
        }

        binding.temporaryUnblockButton.setOnClickListener {
            val currentConfig = appProvider.blockConfig

            // Toggle selection
            val newBlockOption = if (currentConfig.blockOption == BlockOption.TemporaryUnblock) {
                BlockOption.NothingSelected
            } else {
                BlockOption.TemporaryUnblock
            }

            appProvider.blockConfig = currentConfig.copy(blockOption = newBlockOption)

            updateUIForBlockOption(newBlockOption)
        }

        binding.intervalTimerButton.setOnClickListener {
            val currentConfig = appProvider.blockConfig

            // Toggle selection
            val newBlockOption = if (currentConfig.blockOption == BlockOption.IntervalTimer) {
                BlockOption.NothingSelected
            } else {
                BlockOption.IntervalTimer
            }

            appProvider.blockConfig = currentConfig.copy(blockOption = newBlockOption)
            updateUIForBlockOption(newBlockOption)
        }

        // TODO ON First run APP TUTORIAL WITH THE EXPLANATION OF WHY THE ACCESSIBILITY SERVICE IS NEEDED
        // OR if this is disabled, ask it to enable on a popup
        if (!isAccessibilityServiceEnabled(requireContext())) {
            openAccessibilitySettings(requireContext())
        }

        setTimerOverlayCheckBoxListener()
    }

    /**
     * Sets up the listener for the timer overlay checkbox.
     *
     * Initializes the checkbox state from appProvider.timerOverlayEnabled
     *  and updates it when the checkbox is toggled.
     */
    private fun setTimerOverlayCheckBoxListener() {
        binding.checkBoxTimerOverlay.isChecked = appProvider.timerOverlayEnabled

        binding.checkBoxTimerOverlay.setOnCheckedChangeListener { _, isChecked ->
            appProvider.timerOverlayEnabled = isChecked
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun resetButtons() {
        applyButtonEffect(binding.blockAllButton, false)
        applyButtonEffect(binding.dayLimitButton, false)
        applyButtonEffect(binding.temporaryUnblockButton, false)
        applyButtonEffect(binding.intervalTimerButton, false)
    }

    private fun applyButtonEffect(
        button: MaterialButton,
        activated: Boolean
    ) {
        button.apply {
            if (activated) {
                strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke)
                strokeColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            context,
                            R.color.green,
                        ),
                    )
                icon = ResourcesCompat.getDrawable(resources, R.drawable.book_cancel_outline, null)
            } else {
                strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke)
                strokeColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            context,
                            R.color.gray_600,
                        ),
                    )
                icon = ResourcesCompat.getDrawable(resources, R.drawable.book_cancel_outline, null)
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        val serviceName =
            "${context.packageName}/${ScrollessBlockAccessibilityService::class.java.name}"
        return enabledServices?.contains(serviceName) == true
    }

    private fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
        Toast.makeText(context, getString(R.string.accessibility_settings_toast), Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun updateInfoText(totalDailyUsage: Long, timeLimit: Long) {
        binding.tvTime.text = totalDailyUsage.formatTime() + "/" + timeLimit.formatTime()
    }

    private fun updateUIForBlockOption(blockOption: BlockOption) {
        resetButtons()
        when (blockOption) {
            BlockOption.BlockAll -> applyButtonEffect(binding.blockAllButton, true)
            BlockOption.DayLimit -> applyButtonEffect(binding.dayLimitButton, true)
            BlockOption.TemporaryUnblock -> applyButtonEffect(binding.temporaryUnblockButton, true)
            BlockOption.IntervalTimer -> applyButtonEffect(binding.intervalTimerButton, true)
            BlockOption.NothingSelected -> Unit // No action needed
        }
    }
}
