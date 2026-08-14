package com.strive.battleships

import androidx.compose.ui.window.ComposeUIViewController
import com.strive.battleships.di.IosAppComponent

@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController {
    IosAppComponent.instance
    App()
}
