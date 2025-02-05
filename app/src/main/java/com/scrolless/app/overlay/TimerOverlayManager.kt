package com.scrolless.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.scrolless.app.R
import com.scrolless.app.provider.AppProvider
import com.scrolless.app.provider.UsageTracker
import com.scrolless.framework.extensions.getReadableTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface TimerOverlayManager {
    fun attachServiceContext(context: Context)
    fun show()
    fun hide()
}
