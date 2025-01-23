/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.app.features.splash

import com.scrolless.framework.core.base.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor() : MviViewModel<SplashContract.State, SplashContract.Event>() {
    override fun onTriggerEvent(eventType: SplashContract.Event) {
    }
}
