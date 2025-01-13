package com.scrolless.app.provider

import kotlinx.coroutines.flow.StateFlow

interface UsageTracker {

    var dailyUsageInMemory: Long
    val dailyUsageInMemoryFlow: StateFlow<Long>


    /**
     * Load the current daily usage into memory.
     */
    fun load()

    /**
     * Get the current daily usage from in-memory cache.
     * @return The daily usage in milliseconds.
     */
    fun getDailyUsage(): Long

    /**
     * Add a session time to the daily usage (in memory).
     * @param sessionTime The session time in milliseconds to be added.
     */
    fun addToDailyUsage(sessionTime: Long)

    /**
     * Check if a daily reset is needed and perform it if necessary.
     */
    fun checkDailyReset()

    /**
     * Persist the in-memory daily usage to the cache.
     */
    fun save()
}
