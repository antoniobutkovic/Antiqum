package com.strive.antiqum.network

import platform.Foundation.NSBundle

internal actual fun apiBaseUrl(): String {
    val host =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("AntiqumApiHost") as? String)
            ?.takeIf { it.isNotBlank() && !it.contains("\$(") }
            ?: error("Missing AntiqumApiHost in Info.plist")
    return "https://$host"
}
