package com.strive.antiqum.museums.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strive.antiqum.designsystem.AntiqumColors
import com.strive.antiqum.designsystem.AntiqumPrimaryButton
import com.strive.antiqum.designsystem.AntiqumSecondaryButton

@Composable
fun AntiqumScreen(viewModel: MuseumsViewModel) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (!appState.hasEnteredApp) {
        LocationPermissionScreen(onContinue = viewModel::enterApp)
        return
    }

    val selectedMuseum = (uiState as? MuseumsUiState.Success)
        ?.allMuseums
        ?.firstOrNull { it.id == appState.selectedMuseumId }

    if (selectedMuseum != null) {
        MuseumDetailScreen(
            museum = selectedMuseum,
            isFavorite = selectedMuseum.id in appState.favoriteIds,
            isVisited = selectedMuseum.id in appState.visitedIds,
            onBack = { viewModel.selectMuseum(null) },
            onToggleFavorite = { viewModel.toggleFavorite(selectedMuseum.id) },
            onToggleVisited = { viewModel.toggleVisited(selectedMuseum.id) }
        )
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
                onToggleFavorite = viewModel::toggleFavorite,
                onSelectMuseum = viewModel::selectMuseum,
                onRetry = viewModel::reload,
                modifier = Modifier.padding(contentPadding)
            )
            MainTab.Settings -> SettingsScreen(
                appState = appState,
                onThemeSelected = viewModel::setThemeMode,
                modifier = Modifier.padding(contentPadding)
            )
        }
    }
}

@Composable
private fun LocationPermissionScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Antiqum", style = MaterialTheme.typography.headlineLarge)
            Text(
                "CULTURAL DISCOVERY",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.height(64.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            AntiqumColors.Bronze.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Museum,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(76.dp)
                )
            }
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = AntiqumColors.Bronze,
                modifier = Modifier.align(Alignment.TopEnd).padding(40.dp).size(42.dp)
            )
        }

        Spacer(Modifier.height(30.dp))
        Text(
            "Museums\naround you",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Allow Antiqum to use your location to discover museums nearby and show their distance from you.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.weight(1f))
        AntiqumPrimaryButton(
            label = "Use Zagreb for now",
            onClick = onContinue,
            icon = Icons.Outlined.LocationOn,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        AntiqumSecondaryButton(
            label = "Choose Location Manually",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
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
