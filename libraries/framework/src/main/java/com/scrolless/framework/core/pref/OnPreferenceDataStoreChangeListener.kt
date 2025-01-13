package com.scrolless.framework.core.pref

interface OnPreferenceDataStoreChangeListener {
    fun onPreferenceChanged(key: String, newValue: Any?)
}
