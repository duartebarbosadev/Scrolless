/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.navigation

import com.scrolless.libraries.framework.R

enum class AnimationType {
    NO_ANIM,
    DEFAULT,
    ENTER_FROM_LEFT,
    ENTER_FROM_LEFT_WITH_SCALE,
    ENTER_FROM_RIGHT,
    ENTER_FROM_RIGHT_WITH_SCALE,
    ENTER_FROM_BOTTOM
    ;

    companion object {
        fun getAnimation(type: AnimationType): List<Int> {
            when (type) {
                DEFAULT -> return listOf(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right,
                )

                ENTER_FROM_LEFT -> return listOf(
                    R.anim.anim_fragment_in_from_pop,
                    R.anim.anim_fragment_out_from_pop,
                    R.anim.anim_fragment_in,
                    R.anim.anim_fragment_out,
                )

                ENTER_FROM_LEFT_WITH_SCALE -> return listOf(
                    R.anim.anim_scale_fragment_in_from_pop,
                    R.anim.anim_scale_fragment_out_from_pop,
                    R.anim.anim_scale_fragment_in,
                    R.anim.anim_scale_fragment_out,
                )

                ENTER_FROM_RIGHT -> return listOf(
                    R.anim.anim_fragment_in,
                    R.anim.anim_fragment_out,
                    R.anim.anim_fragment_in_from_pop,
                    R.anim.anim_fragment_out_from_pop,
                )

                ENTER_FROM_RIGHT_WITH_SCALE -> return listOf(
                    R.anim.anim_scale_fragment_in,
                    R.anim.anim_scale_fragment_out,
                    R.anim.anim_scale_fragment_in_from_pop,
                    R.anim.anim_scale_fragment_out_from_pop,
                )

                ENTER_FROM_BOTTOM -> return listOf(
                    R.anim.anim_vertical_fragment_in_long,
                    R.anim.anim_vertical_fragment_out_long,
                    R.anim.anim_vertical_fragment_in_from_pop_long,
                    R.anim.anim_vertical_fragment_out_from_pop_long,
                )

                NO_ANIM -> return listOf()
            }
        }
    }
}
