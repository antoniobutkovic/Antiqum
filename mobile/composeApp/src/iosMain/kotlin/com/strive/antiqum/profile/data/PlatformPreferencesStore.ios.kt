package com.strive.antiqum.profile.data

import platform.Foundation.NSUserDefaults

actual class PlatformPreferencesStore actual constructor() {
    private val preferences = NSUserDefaults.standardUserDefaults

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean = if (preferences.objectForKey(key) == null) defaultValue else preferences.boolForKey(key)

    actual fun putBoolean(key: String, value: Boolean) {
        preferences.setBool(value, forKey = key)
    }

    actual fun getString(key: String): String? = preferences.stringForKey(key)

    actual fun putString(key: String, value: String) {
        preferences.setObject(value, forKey = key)
    }

    actual fun remove(key: String) {
        preferences.removeObjectForKey(key)
    }
}

actual val supportsAppleSignIn: Boolean = true
