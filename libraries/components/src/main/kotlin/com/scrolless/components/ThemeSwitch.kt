/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.components

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.switchmaterial.SwitchMaterial
import com.scrolless.libraries.components.R

class ThemeSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwitchMaterial(context, attrs) {

    init {
        thumbDrawable = AppCompatResources.getDrawable(context, R.drawable.libraries_components_selector_dark_light)
        trackDrawable = AppCompatResources.getDrawable(context, R.drawable.libraries_components_selector_bg_dark_light)
    }
}
