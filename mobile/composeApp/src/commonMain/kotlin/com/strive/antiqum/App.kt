package com.strive.antiqum

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strive.antiqum.designsystem.AntiqumTheme
import com.strive.antiqum.di.getAppComponent
import com.strive.antiqum.museums.ui.AntiqumScreen

@Composable
fun App() {
    val museumsViewModel = viewModel { getAppComponent().museumsViewModel }
    val appState = museumsViewModel.appState.collectAsStateWithLifecycle()

    AntiqumTheme(themeMode = appState.value.themeMode) {
        AntiqumScreen(viewModel = museumsViewModel)
    }
}
