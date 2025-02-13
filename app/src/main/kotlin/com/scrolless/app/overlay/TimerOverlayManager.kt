/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.overlay

import android.content.Context

interface TimerOverlayManager {
    fun attachServiceContext(context: Context)
    fun show()
    fun hide()
}
