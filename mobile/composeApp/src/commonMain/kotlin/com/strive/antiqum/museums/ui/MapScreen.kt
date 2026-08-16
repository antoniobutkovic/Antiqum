package com.strive.antiqum.museums.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.strive.antiqum.designsystem.AntiqumCard
import com.strive.antiqum.designsystem.AntiqumColors
import com.strive.antiqum.designsystem.AntiqumFilterChip
import com.strive.antiqum.designsystem.AntiqumPrimaryButton
import com.strive.antiqum.designsystem.MuseumImage
import com.strive.antiqum.museums.data.Museum
import com.strive.antiqum.museums.data.MuseumCategory

@Composable
fun MapScreen(
    uiState: MuseumsUiState,
    appState: AntiqumAppState,
    onSelectMapMuseum: (String?) -> Unit,
    onOpenMuseum: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCategorySelected: (MuseumCategory) -> Unit,
    onUserLocationResolved: (MuseumMapLocation?) -> Unit,
    onRecenter: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        MuseumsUiState.Loading -> MuseumsLoading(modifier)
        is MuseumsUiState.Error -> MuseumsError(uiState.message, onRetry, modifier)
        is MuseumsUiState.Success -> Box(modifier = modifier.fillMaxSize()) {
            val mapMuseums = if (appState.mapUserLocation == null) {
                (uiState.nearbyMuseums + uiState.allMuseums).distinctBy(Museum::id)
            } else {
                uiState.nearbyMuseums
            }
            MuseumMap(
                markers = mapMuseums
                    .filter { museum ->
                        appState.selectedCategory == MuseumCategory.All || museum.category == appState.selectedCategory
                    }
                    .map { museum ->
                        MuseumMapMarker(
                            id = museum.id,
                            name = museum.name,
                            category = museum.category.label,
                            latitude = museum.latitude,
                            longitude = museum.longitude
                        )
                    },
                selectedMuseumId = appState.mapSelectedMuseumId,
                darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f,
                userLocation = appState.mapUserLocation,
                requestUserLocation = appState.mapLocationAccess == MapLocationAccess.Unknown,
                cameraRequestId = appState.mapCameraRequestId,
                onMuseumSelected = onSelectMapMuseum,
                onUserLocationResolved = onUserLocationResolved,
                modifier = Modifier.fillMaxSize()
            )

            Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        listOf(
                            MuseumCategory.All,
                            MuseumCategory.Art,
                            MuseumCategory.History,
                            MuseumCategory.Science,
                            MuseumCategory.Archaeology
                        )
                    ) { category ->
                        AntiqumFilterChip(
                            label = category.label,
                            selected = appState.selectedCategory == category,
                            onClick = { onCategorySelected(category) }
                        )
                    }
                }
            }

            val selected = mapMuseums.firstOrNull { it.id == appState.mapSelectedMuseumId }
            MapToolButton(
                icon = Icons.Outlined.MyLocation,
                contentDescription = if (appState.mapUserLocation == null) {
                    "Show world map"
                } else {
                    "Recenter on current location"
                },
                onClick = onRecenter,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 18.dp,
                        bottom = if (selected == null) 18.dp else 126.dp
                    )
            )
            if (selected != null) {
                SelectedMuseumCard(
                    museum = selected,
                    isFavorite = selected.id in appState.favoriteIds,
                    onToggleFavorite = { onToggleFavorite(selected.id) },
                    onOpen = { onOpenMuseum(selected.id) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun MapToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(46.dp)) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}

@Composable
private fun SelectedMuseumCard(
    museum: Museum,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    AntiqumCard(modifier = modifier) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            MuseumImage(
                imageUrl = museum.imageUrl,
                contentDescription = museum.name,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(13.dp))
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(museum.name, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${museum.category.label} · ${museum.distanceKm.formatDistance()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    museum.locationLabel.ifBlank { "Location on map" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = AntiqumColors.Bronze
                    )
                }
                AntiqumPrimaryButton(
                    label = "View",
                    onClick = onOpen,
                    modifier = Modifier.width(86.dp)
                )
            }
        }
    }
}
