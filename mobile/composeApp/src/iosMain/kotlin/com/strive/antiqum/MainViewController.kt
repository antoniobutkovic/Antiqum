package com.strive.antiqum

import androidx.compose.ui.window.ComposeUIViewController
import com.strive.antiqum.di.IosAppComponent

@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController {
    IosAppComponent.instance
    App()
}
