package com.strive.antiqum.profile.data

import android.content.Context
import android.content.SharedPreferences

private lateinit var sharedPreferences: SharedPreferences

fun initializeProfilePreferences(context: Context) {
    sharedPreferences = context.applicationContext.getSharedPreferences(
        "antiqum_profile",
        Context.MODE_PRIVATE
    )
}

actual class PlatformPreferencesStore actual constructor() {
    private val preferences: SharedPreferences
        get() {
            check(::sharedPreferences.isInitialized) {
                "Profile preferences must be initialized before the app component."
            }
            return sharedPreferences
        }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean = preferences.getBoolean(key, defaultValue)

    actual fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    actual fun getString(key: String): String? = preferences.getString(key, null)

    actual fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    actual fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}

actual val supportsAppleSignIn: Boolean = false
