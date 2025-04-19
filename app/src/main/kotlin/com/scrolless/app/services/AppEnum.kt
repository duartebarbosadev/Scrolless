/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.services

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME

enum class AppEnum(private val viewId: String, private val getawayStrategy: Int) {
    REELS("com.instagram.android:id/clips_viewer_view_pager", GLOBAL_ACTION_BACK),
    SHORTS("com.google.android.youtube:id/reel_player_page_container", GLOBAL_ACTION_BACK),
    TIKTOK("com.zhiliaoapp.musically:id/ulz", GLOBAL_ACTION_HOME);

    fun getViewId(): String = viewId

    fun getGetawayStrategy(): Int = getawayStrategy
}
