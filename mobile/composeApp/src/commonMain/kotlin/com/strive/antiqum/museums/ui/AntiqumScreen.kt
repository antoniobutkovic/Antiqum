package com.strive.antiqum.museums.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strive.antiqum.designsystem.AntiqumPrimaryButton
import com.strive.antiqum.louvre.data.LOUVRE_MUSEUM_ID
import com.strive.antiqum.louvre.ui.LouvreIndoorScreen
import com.strive.antiqum.louvre.ui.LouvreIndoorViewModel
import com.strive.antiqum.onboarding.ui.OnboardingScreen
import com.strive.antiqum.profile.data.supportsAppleSignIn

@Composable
fun AntiqumScreen(
    viewModel: MuseumsViewModel,
    louvreIndoorViewModel: LouvreIndoorViewModel
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val museumDetails by viewModel.museumDetails.collectAsStateWithLifecycle()
    var isLouvreGuideVisible by rememberSaveable { mutableStateOf(false) }

    if (appState.isOnboardingVisible) {
        OnboardingScreen(
            showAppleSignIn = supportsAppleSignIn,
            onSignIn = viewModel::signInFromOnboarding,
            onSkip = viewModel::completeOnboarding
        )
        return
    }

    val selectedMuseum = museumDetails?.takeIf { it.id == appState.selectedMuseumId }
        ?: (uiState as? MuseumsUiState.Success)
            ?.let { state -> state.nearbyMuseums + state.allMuseums }
            ?.firstOrNull { it.id == appState.selectedMuseumId }

    if (isLouvreGuideVisible) {
        LouvreIndoorScreen(
            viewModel = louvreIndoorViewModel,
            onBack = { isLouvreGuideVisible = false }
        )
        return
    }

    if (selectedMuseum != null) {
        MuseumDetailScreen(
            museum = selectedMuseum,
            isFavorite = selectedMuseum.id in appState.favoriteIds,
            isVisited = selectedMuseum.id in appState.visitedIds,
            onBack = { viewModel.selectMuseum(null) },
            onToggleFavorite = { viewModel.toggleFavorite(selectedMuseum.id) },
            onToggleVisited = { viewModel.toggleVisited(selectedMuseum.id) },
            onOpenIndoorGuide = if (selectedMuseum.id == LOUVRE_MUSEUM_ID) {
                { isLouvreGuideVisible = true }
            } else {
                null
            }
        )
        appState.signInPrompt?.let { prompt ->
            MuseumSignInSheet(
                action = prompt.action,
                showAppleSignIn = supportsAppleSignIn,
                onSignIn = viewModel::signIn,
                onDismiss = viewModel::dismissSignInPrompt
            )
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AntiqumBottomBar(
                selectedTab = appState.selectedTab,
                onSelected = viewModel::selectTab
            )
        }
    ) { contentPadding ->
        when (appState.selectedTab) {
            MainTab.Map -> MapScreen(
                uiState = uiState,
                appState = appState,
                onSelectMapMuseum = viewModel::selectMapMuseum,
                onOpenMuseum = viewModel::selectMuseum,
                onToggleFavorite = viewModel::toggleFavorite,
                onCategorySelected = viewModel::selectCategory,
                onUserLocationResolved = viewModel::resolveMapLocation,
                onRecenter = viewModel::recenterMap,
                onRetry = viewModel::reload,
                modifier = Modifier.padding(contentPadding)
            )
            MainTab.Museums -> MuseumsScreen(
                uiState = uiState,
                appState = appState,
                onSearchChange = viewModel::updateSearch,
                onCategorySelected = viewModel::selectCategory,
                onPersonalFilterSelected = viewModel::selectPersonalFilter,
                onSortSelected = viewModel::selectSort,
                onToggleFilters = { viewModel.setFiltersVisible(!appState.showFilters) },
                onLoadMore = viewModel::loadNextMuseumPage,
                onToggleFavorite = viewModel::toggleFavorite,
                onSelectMuseum = viewModel::selectMuseum,
                onRetry = viewModel::reload,
                modifier = Modifier.padding(contentPadding)
            )
            MainTab.Settings -> SettingsScreen(
                appState = appState,
                onThemeSelected = viewModel::setThemeMode,
                showAppleSignIn = supportsAppleSignIn,
                onSignIn = viewModel::signIn,
                onSignOut = viewModel::signOut,
                onShowTutorial = viewModel::showOnboarding,
                modifier = Modifier.padding(contentPadding)
            )
        }
    }

    appState.signInPrompt?.let { prompt ->
        MuseumSignInSheet(
            action = prompt.action,
            showAppleSignIn = supportsAppleSignIn,
            onSignIn = viewModel::signIn,
            onDismiss = viewModel::dismissSignInPrompt
        )
    }
}

@Composable
private fun AntiqumBottomBar(
    selectedTab: MainTab,
    onSelected: (MainTab) -> Unit
) {
    val items = listOf(
        Triple(MainTab.Map, "Map", Icons.Outlined.Map),
        Triple(MainTab.Museums, "Museums", Icons.Outlined.Museum),
        Triple(MainTab.Settings, "Settings", Icons.Outlined.Settings)
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelected(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
internal fun MuseumsLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                "Finding museums on Wikidata…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun MuseumsError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Museum,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(18.dp))
            Text("The gallery is temporarily quiet", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(22.dp))
            AntiqumPrimaryButton(label = "Try again", onClick = onRetry)
        }
    }
}
