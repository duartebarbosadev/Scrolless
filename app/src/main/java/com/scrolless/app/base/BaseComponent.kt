/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.base

import android.content.Context
import android.util.AttributeSet
import androidx.viewbinding.ViewBinding
import com.scrolless.framework.core.base.core.CoreComponent

abstract class BaseComponent<VB : ViewBinding>(
    context: Context,
    attrs: AttributeSet? = null
) : CoreComponent<VB>(context, attrs)
