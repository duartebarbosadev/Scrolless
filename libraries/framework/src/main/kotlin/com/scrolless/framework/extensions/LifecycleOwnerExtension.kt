/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <T> LifecycleOwner.observeFlow(property: Flow<T>, block: (T) -> Unit) {
    lifecycleScope.launch {
        property.collect { block(it) }
    }
}

fun <T> LifecycleOwner.observeFlowStart(property: Flow<T>, block: (T) -> Unit) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            property.collect { block(it) }
        }
    }
}

fun <T> LifecycleOwner.observeLiveData(liveData: LiveData<T>, block: (T) -> Unit) {
    liveData.observe(this) { block(it) }
}

fun <T : Any, L : MutableLiveData<T>> LifecycleOwner.observeLiveData(
    liveData: L,
    block: (T) -> Unit
) {
    liveData.observe(this) { block(it) }
}

fun LifecycleOwner.repeatOnStared(block: suspend CoroutineScope.() -> Unit) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED, block)
    }
}
