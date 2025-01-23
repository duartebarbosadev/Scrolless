/*
 * Copyright (C) 2025, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.pref

interface OnPreferenceDataStoreChangeListener {
    fun onPreferenceChanged(key: String, newValue: Any?)
}
