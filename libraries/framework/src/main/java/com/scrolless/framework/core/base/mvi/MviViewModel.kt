/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.base.mvi

import com.scrolless.framework.core.base.mvvm.MvvmViewModel
import com.scrolless.framework.core.flow.MutableEventFlow
import com.scrolless.framework.core.flow.asEventFlow

abstract class MviViewModel<STATE, EVENT> : MvvmViewModel() {

    private val _stateFlow = MutableEventFlow<STATE>()
    val stateFlow = _stateFlow.asEventFlow()

    abstract fun onTriggerEvent(eventType: EVENT)

    protected fun setState(state: STATE) = safeLaunch {
        _stateFlow.emit(state)
    }
}
