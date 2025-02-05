/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.provider

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class UsageTrackerImpl @Inject constructor(
    private val appProvider: AppProvider
) : UsageTracker {

    override var dailyUsageInMemory: Long
        get() = _dailyUsageInMemoryFlow.value
        set(value) {
            if (value != _dailyUsageInMemoryFlow.value) {
                _dailyUsageInMemoryFlow.value = value
            }
        }

    private val _dailyUsageInMemoryFlow = MutableStateFlow(0L)
    override val dailyUsageInMemoryFlow: StateFlow<Long> = _dailyUsageInMemoryFlow

    init {
        load()
        checkDailyReset()
    }

    override fun load() {
        dailyUsageInMemory = appProvider.totalDailyUsage
    }

    override fun getDailyUsage(): Long = dailyUsageInMemory

    override fun addToDailyUsage(sessionTime: Long) {
        dailyUsageInMemory += sessionTime
    }

    override fun checkDailyReset() {
        val currentDay = getCurrentDayStamp()
        val lastDay = appProvider.lastResetDay
        if (currentDay != lastDay) {
            dailyUsageInMemory = 0L
            appProvider.lastResetDay = currentDay
            save()
        }
    }

    override fun save() {
        appProvider.totalDailyUsage = dailyUsageInMemory
    }

    private fun getCurrentDayStamp(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
}
