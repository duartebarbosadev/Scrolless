/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import android.os.Build
import android.os.Parcelable
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// companion object {
//    const val EXTRA_CHARACTER_PREVIEW = "extra_character_preview"
//    const val TAG = "CharacterDetailFragment"
//
//    fun newInstance(characterPreview: CharacterPreview): CharacterDetailFragment {
//        return CharacterDetailFragment().apply {
//            arguments = bundleOf(EXTRA_CHARACTER_PREVIEW to characterPreview)
//        }
//    }
// }
//
// private val characterPreview: CharacterPreview by argument(EXTRA_CHARACTER_PREVIEW)

@SuppressWarnings("deprecation")
inline fun <reified T : Any> Fragment.argument(key: String, default: T? = null): Lazy<T> = lazy {
    val value: Any? = when (T::class) {
        String::class -> arguments?.getString(key)
        Int::class -> arguments?.getInt(key)
        Boolean::class -> arguments?.getBoolean(key)
        Float::class -> arguments?.getFloat(key)
        Double::class -> arguments?.getDouble(key)
        Long::class -> arguments?.getLong(key)
        else -> handleParcelable(key, default)
    }
    requireNotNull(if (value is T) value else default) { key }
}

inline fun <reified T : Any> Fragment.handleParcelable(
    key: String,
    default: T?
): Any? =
    if ((default != null) && Parcelable::class.java.isAssignableFrom(default.javaClass)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(key, default.javaClass)
        } else {
            @Suppress("DEPRECATION")
            arguments?.get(key)
        }
    } else {
        throw IllegalArgumentException("Unsupported argument type: ${T::class}")
    }

// launchAndRepeatWithViewLifecycle {
//    launch {
//        viewModel.onViewState
//            .filterNotNull()
//            .collect(::bindViewState)
//    }
// }

/**
 * Launches a new coroutine and repeats `block` every time the Fragment's viewLifecycleOwner
 * is in and out of `minActiveState` lifecycle state.
 * @see https://github.com/google/iosched/blob/main/mobile/src/main/kotlin/com/google/samples/apps/iosched/util/UiUtils.kt#L60
 */
@Suppress("unused")
inline fun Fragment.launchAndRepeatWithViewLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}

fun Fragment.setStatusBarColor(@ColorInt color: Int) {
    val window: Window = requireActivity().window
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = color
}
