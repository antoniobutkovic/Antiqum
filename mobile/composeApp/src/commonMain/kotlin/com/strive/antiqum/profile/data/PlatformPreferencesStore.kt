package com.strive.antiqum.profile.data

expect class PlatformPreferencesStore() {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)

    fun getString(key: String): String?

    fun putString(key: String, value: String)

    fun remove(key: String)
}

expect val supportsAppleSignIn: Boolean

private const val SET_SEPARATOR = "\u001F"

internal fun PlatformPreferencesStore.getStringSet(key: String): Set<String> = getString(key)
    ?.takeIf(String::isNotEmpty)
    ?.split(SET_SEPARATOR)
    ?.toSet()
    .orEmpty()

internal fun PlatformPreferencesStore.putStringSet(key: String, values: Set<String>) {
    putString(key, values.sorted().joinToString(SET_SEPARATOR))
}
