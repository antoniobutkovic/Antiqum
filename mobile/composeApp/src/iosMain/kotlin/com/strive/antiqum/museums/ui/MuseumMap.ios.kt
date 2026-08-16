package com.strive.antiqum.museums.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

interface IosMuseumMapViewFactory {
    fun makeMapView(
        onMuseumSelected: (String) -> Unit,
        onUserLocationResolved: (MuseumMapLocation?) -> Unit
    ): UIView

    fun updateMapView(
        view: UIView,
        markers: List<MuseumMapMarker>,
        selectedMuseumId: String?,
        darkTheme: Boolean,
        userLocation: MuseumMapLocation?,
        requestUserLocation: Boolean,
        cameraRequestId: Int
    )
}

object IosMuseumMapBridge {
    var factory: IosMuseumMapViewFactory? = null
}

@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
internal actual fun MuseumMap(
    markers: List<MuseumMapMarker>,
    selectedMuseumId: String?,
    darkTheme: Boolean,
    userLocation: MuseumMapLocation?,
    requestUserLocation: Boolean,
    cameraRequestId: Int,
    onMuseumSelected: (String) -> Unit,
    onUserLocationResolved: (MuseumMapLocation?) -> Unit,
    modifier: Modifier
) {
    val factory = IosMuseumMapBridge.factory
    val currentSelectionHandler = rememberUpdatedState(onMuseumSelected)
    val currentLocationHandler = rememberUpdatedState(onUserLocationResolved)
    if (factory == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add GOOGLE_MAPS_API_KEY to Secrets.xcconfig to load Google Maps.",
                modifier = Modifier.padding(32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    UIKitView(
        factory = {
            factory.makeMapView(
                onMuseumSelected = { museumId ->
                    currentSelectionHandler.value(museumId)
                },
                onUserLocationResolved = { location ->
                    currentLocationHandler.value(location)
                }
            )
        },
        modifier = modifier,
        update = { view ->
            factory.updateMapView(
                view = view,
                markers = markers,
                selectedMuseumId = selectedMuseumId,
                darkTheme = darkTheme,
                userLocation = userLocation,
                requestUserLocation = requestUserLocation,
                cameraRequestId = cameraRequestId
            )
        },
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
            isNativeAccessibilityEnabled = true
        )
    )
}
