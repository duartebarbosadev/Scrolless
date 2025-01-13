/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.font

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import java.util.Hashtable

object FontCache {
    private val fontCache: Hashtable<Int, Typeface> = Hashtable<Int, Typeface>()
    operator fun get(font: Int, context: Context): Typeface? {
        if (fontCache.contains(font).not()) {
            try {
                fontCache[font] = ResourcesCompat.getFont(context, font)
            } catch (e: Exception) {
                return null
            }
        }
        return fontCache[font]
    }
}
