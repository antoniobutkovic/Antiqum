package com.strive.antiqum.museums.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strive.antiqum.designsystem.AntiqumColors
import com.strive.antiqum.designsystem.AntiqumPrimaryButton
import com.strive.antiqum.designsystem.AntiqumSecondaryButton
import com.strive.antiqum.designsystem.MuseumImage
import com.strive.antiqum.museums.data.Museum

@Composable
fun MuseumDetailScreen(
    museum: Museum,
    isFavorite: Boolean,
    isVisited: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleVisited: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Box(Modifier.fillMaxWidth().height(350.dp)) {
                    MuseumImage(
                        imageUrl = museum.imageUrl,
                        contentDescription = museum.name,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), androidx.compose.ui.graphics.Color.Transparent)
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailToolButton(
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailToolButton(
                                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) AntiqumColors.Bronze else MaterialTheme.colorScheme.onSurface,
                                onClick = onToggleFavorite
                            )
                            DetailToolButton(
                                icon = Icons.Outlined.Share,
                                contentDescription = "View Wikidata source",
                                onClick = { uriHandler.openUri("https://www.wikidata.org/wiki/${museum.id}") }
                            )
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Text(
                        museum.category.label.uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(museum.name, style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${museum.locationLabel.ifBlank { "Location unavailable" }} · ${museum.distanceKm.formatDistance()} from Zagreb",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        AntiqumSecondaryButton(
                            label = if (isFavorite) "Saved" else "Favorite",
                            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            onClick = onToggleFavorite,
                            modifier = Modifier.weight(1f)
                        )
                        AntiqumPrimaryButton(
                            label = if (isVisited) "Visited" else "Mark visited",
                            icon = if (isVisited) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                            onClick = onToggleVisited,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AnimatedVisibility(isVisited) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Added to your visited museums",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(30.dp))
                    Text("About", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        museum.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        MuseumStat(
                            value = museum.foundedYear ?: "—",
                            label = "Founded",
                            modifier = Modifier.weight(1f)
                        )
                        MuseumStat(value = museum.category.label, label = "Collection", modifier = Modifier.weight(1f))
                        MuseumStat(value = "Wikidata", label = "Source", modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(28.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column {
                            DetailRow(
                                label = "Address",
                                value = museum.address ?: museum.locationLabel.ifBlank { "Location on Wikidata" },
                                icon = Icons.Outlined.LocationOn
                            )
                            museum.website?.let { website ->
                                DetailRow(
                                    label = "Official website",
                                    value = website.substringAfter("://").substringBefore('/'),
                                    icon = Icons.Outlined.Language,
                                    onClick = { uriHandler.openUri(website) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Surface(
                        onClick = {
                            uriHandler.openUri(
                                "https://www.google.com/maps/search/?api=1&query=${museum.latitude},${museum.longitude}"
                            )
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(Modifier.height(7.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Open in Maps", style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.width(5.dp))
                                    Icon(
                                        Icons.AutoMirrored.Outlined.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 2.dp
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
            Icon(icon, contentDescription = contentDescription, tint = tint)
        }
    }
}

@Composable
private fun MuseumStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clip(RoundedCornerShape(12.dp)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (onClick != null) {
            IconButton(onClick = onClick) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open")
            }
        }
    }
}
