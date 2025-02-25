/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

fun Bitmap.getSquareSize(): Int = width.coerceAtMost(height)

// Loop an animated vector drawable indefinitely
fun ImageView.applyLoopingAnimatedVectorDrawable(
    @DrawableRes animatedVector: Int,
    endDelay: Long = 0,
    disableLooping: Boolean = false
) {
    val animated = AnimatedVectorDrawableCompat.create(context, animatedVector)
    // Ability to disable the loop, for a future option
    if (!disableLooping) {
        animated?.registerAnimationCallback(
            object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            this@applyLoopingAnimatedVectorDrawable.post { animated.start() }
                        },
                        endDelay,
                    )
                }
            },
        )
    }
    this.setImageDrawable(animated)
    animated?.start()
}

fun ByteArray.byteArrayToBitmap(): Bitmap {
    // If the string is empty, just return an empty white bitmap
    if (isEmpty()) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    return BitmapFactory.decodeByteArray(this, 0, size)
}

// Get the smallest dimension in a non-square image to crop and resize it
fun Bitmap.getBitmapSquareSize(): Int = width.coerceAtMost(height)

fun Bitmap.toByteArray(): ByteArray? {
    val imgConverted: ByteArray = byteArrayOf()
    return try {
        toByteArray()
    } catch (e: Exception) {
        imgConverted
    }
}
