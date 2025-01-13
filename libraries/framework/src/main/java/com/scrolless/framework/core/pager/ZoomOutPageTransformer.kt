/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.pager

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.max

private const val MIN_SCALE = 0.85f
private const val MIN_ALPHA = 0.5f

/**
 * A PageTransformer implementation for ViewPager2 that creates a zoom-out effect on the pages
 * during page transitions. It scales down the pages and applies alpha fading to create a visually
 * appealing transition effect.
 *
 * This transformer is intended for use with ViewPager2 to customize the animation of pages as they
 * are swiped left or right.
 *
 * @property MIN_SCALE The minimum scaling factor for pages during the transition. The default is 0.85f.
 * @property MIN_ALPHA The minimum alpha value for pages during the transition. The default is 0.5f.
 */
class ZoomOutPageTransformer : ViewPager2.PageTransformer {

    /**
     * Applies the zoom-out effect to a given page during a page transition.
     *
     * @param view The View that represents the current page.
     * @param position The position of the page relative to the current page being transformed.
     *      When `position` is 0, the page is in the center of the ViewPager2. When it's -1, the page
     *      is off-screen to the left, and when it's 1, the page is off-screen to the right.
     */
    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            val pageHeight = height
            when {
                position < -1 -> { // [-Infinity, -1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                }

                position <= 1 -> { // [-1, 1]
                    // Modify the default slide transition to shrink the page as well.
                    val scaleFactor = max(MIN_SCALE, 1 - abs(position))
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horzMargin = pageWidth * (1 - scaleFactor) / 2
                    translationX = if (position < 0) {
                        horzMargin - vertMargin / 2
                    } else {
                        horzMargin + vertMargin / 2
                    }

                    // Scale the page down (between MIN_SCALE and 1).
                    scaleX = scaleFactor
                    scaleY = scaleFactor

                    // Fade the page relative to its size.
                    alpha = (
                            MIN_ALPHA +
                                    (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA))
                            )
                }

                else -> { // (1, +Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }
}
