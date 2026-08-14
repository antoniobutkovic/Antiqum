package com.strive.antiqum.network

import com.strive.antiqum.BuildConfig

internal actual fun apiBaseUrl(): String = "https://${BuildConfig.ANTIQUM_API_HOST}"
