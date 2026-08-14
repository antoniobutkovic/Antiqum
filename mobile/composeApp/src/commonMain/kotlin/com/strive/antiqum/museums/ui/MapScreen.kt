package com.strive.antiqum.museums.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        MuseumsUiState.Loading -> MuseumsLoading(modifier)
        is MuseumsUiState.Error -> MuseumsError(uiState.message, onRetry, modifier)
        is MuseumsUiState.Success -> BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFE9E4DA))
        ) {
            MuseumMapArtwork()

            uiState.visibleMuseums.take(16).forEachIndexed { index, museum ->
                val horizontal = ((museum.longitude - MuseumsViewModel.ZAGREB_LONGITUDE + 0.5) / 1.0)
                    .toFloat()
                    .coerceIn(0.08f, 0.9f)
                val vertical = ((MuseumsViewModel.ZAGREB_LATITUDE - museum.latitude + 0.5) / 1.0)
                    .toFloat()
                    .coerceIn(0.2f, 0.76f)
                MuseumMapPin(
                    selected = museum.id == appState.mapSelectedMuseumId,
                    onClick = { onSelectMapMuseum(museum.id) },
                    modifier = Modifier.offset(
                        x = maxWidth * horizontal - 21.dp,
                        y = maxHeight * vertical - 21.dp
                    ),
                    accent = index % 5 == 0
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        shadowElevation = 3.dp
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text("Antiqum", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Zagreb, Croatia",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MapToolButton(icon = Icons.Outlined.Search, contentDescription = "Search")
                        MapToolButton(icon = Icons.Outlined.MyLocation, contentDescription = "Recenter")
                    }
                }
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

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (appState.mapSelectedMuseumId == null) 22.dp else 176.dp)
            ) {
                Text(
                    "Search this area",
                    modifier = Modifier.padding(horizontal = 17.dp, vertical = 11.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            val selected = uiState.allMuseums.firstOrNull { it.id == appState.mapSelectedMuseumId }
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
private fun MuseumMapArtwork() {
    val road = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
    val minorRoad = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    val park = Color(0xFFDDE1DA)
    val primary = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        drawRect(park, topLeft = Offset(size.width * 0.34f, 0f), size = size.copy(width = size.width * 0.28f))
        val diagonal = Path().apply {
            moveTo(-40f, size.height * 0.22f)
            cubicTo(size.width * 0.25f, size.height * 0.32f, size.width * 0.52f, size.height * 0.58f, size.width + 40f, size.height * 0.48f)
        }
        drawPath(diagonal, road, style = Stroke(width = 15f, cap = StrokeCap.Round))
        drawPath(diagonal, Color(0xFFD4CEC3), style = Stroke(width = 2f, cap = StrokeCap.Round))
        repeat(5) { index ->
            val y = size.height * (0.2f + index * 0.14f)
            drawLine(
                color = minorRoad,
                start = Offset(-20f, y),
                end = Offset(size.width + 20f, y - size.height * 0.17f),
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )
        }
        repeat(4) { index ->
            val x = size.width * (0.16f + index * 0.22f)
            drawLine(
                color = minorRoad,
                start = Offset(x, 0f),
                end = Offset(x + size.width * 0.18f, size.height),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
        drawCircle(
            color = primary.copy(alpha = 0.15f),
            radius = 28f,
            center = Offset(size.width * 0.5f, size.height * 0.5f)
        )
        drawCircle(
            color = Color(0xFF6C8C83),
            radius = 10f,
            center = Offset(size.width * 0.5f, size.height * 0.5f)
        )
        drawCircle(
            color = Color.White,
            radius = 6f,
            center = Offset(size.width * 0.5f, size.height * 0.5f),
            style = Stroke(width = 4f)
        )
    }
}

@Composable
private fun MuseumMapPin(
    selected: Boolean,
    onClick: () -> Unit,
    accent: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(if (selected) 48.dp else 42.dp),
        shape = CircleShape,
        color = if (accent) AntiqumColors.Bronze else MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = if (selected) 8.dp else 4.dp,
        border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.surface) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = "Museum marker",
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun MapToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        IconButton(onClick = {}, modifier = Modifier.size(46.dp)) {
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
                    museum.locationLabel.ifBlank { "Zagreb area" },
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
