/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.core.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import javax.crypto.AEADBadTagException

/**
 * @param fileName Name of the Shared Preferences
 * @return SharedPreferences
 */
fun Context.getPrefs(fileName: String? = null): SharedPreferences {
    /**
     * Master key for the encryption/decryption of SharedPreferences.
     */
    val masterKey = MasterKey
        .Builder(this, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val name = if (fileName.isNullOrEmpty()) {
        getDefaultSharedPrefName()
    } else {
        fileName.toString()
    }

    return try {
        // Attempt to create encrypted SharedPreferences
        EncryptedSharedPreferences.create(
            this,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: AEADBadTagException) {
        Timber.e("Failed to create EncryptedSharedPreferences: ${e.localizedMessage}")

        // Handle corrupted or incompatible SharedPreferences
        deleteSharedPreferences(name) // Clear the corrupted data
        EncryptedSharedPreferences.create(
            this,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: IllegalArgumentException) { // Rethrow any unexpected exceptions
        Timber.e(e)

        throw IllegalStateException(
            "Failed to create EncryptedSharedPreferences: ${e.localizedMessage}",
            e,
        )
    }
}

/**
 * @return Default SharedPreferences filename
 */
fun Context.getDefaultSharedPrefName(): String = this.packageName + "_pref"
