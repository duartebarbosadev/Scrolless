package com.scrolless.framework.core.pref

import androidx.preference.PreferenceDataStore

class CachePreferenceDataStore(
    private val cacheManager: CacheManager,
    private val listener: OnPreferenceDataStoreChangeListener? = null
) : PreferenceDataStore() {
    override fun putString(key: String, value: String?) {
        cacheManager.write(key, value ?: "")
        listener?.onPreferenceChanged(key, value)
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        cacheManager.write(key, values?.joinToString(",") ?: "")
        listener?.onPreferenceChanged(key, values)
    }

    override fun putInt(key: String, value: Int) {
        cacheManager.write(key, value)
        listener?.onPreferenceChanged(key, value)
    }

    override fun putLong(key: String, value: Long) {
        cacheManager.write(key, value)
        listener?.onPreferenceChanged(key, value)
    }

    override fun putFloat(key: String, value: Float) {
        cacheManager.write(key, value)
        listener?.onPreferenceChanged(key, value)
    }

    override fun putBoolean(key: String, value: Boolean) {
        cacheManager.write(key, value)
        listener?.onPreferenceChanged(key, value)
    }

    override fun getString(key: String, defValue: String?): String? =
        cacheManager.read(key, defValue ?: "")

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        val value = cacheManager.read(key, "")
        return if (value.isNotEmpty()) value.split(",").toSet() else defValues
    }

    override fun getInt(key: String, defValue: Int): Int = cacheManager.read(key, defValue)

    override fun getLong(key: String, defValue: Long): Long = cacheManager.read(key, defValue)

    override fun getFloat(key: String, defValue: Float): Float = cacheManager.read(key, defValue)

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        cacheManager.read(key, defValue)
}
