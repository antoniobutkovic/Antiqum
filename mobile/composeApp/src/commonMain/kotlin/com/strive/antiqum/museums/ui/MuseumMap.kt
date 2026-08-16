package com.strive.antiqum.museums.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Platform-neutral marker data consumed by the native Android and iOS maps. */
data class MuseumMapMarker(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double
)

data class MuseumMapLocation(
    val latitude: Double,
    val longitude: Double
)

@Composable
internal expect fun MuseumMap(
    markers: List<MuseumMapMarker>,
    selectedMuseumId: String?,
    darkTheme: Boolean,
    userLocation: MuseumMapLocation?,
    requestUserLocation: Boolean,
    cameraRequestId: Int,
    onMuseumSelected: (String) -> Unit,
    onUserLocationResolved: (MuseumMapLocation?) -> Unit,
    modifier: Modifier = Modifier
)
