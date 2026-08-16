package com.strive.antiqum.museums.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapColorScheme
import com.google.android.gms.maps.model.MarkerOptions
import com.strive.antiqum.BuildConfig
import com.strive.antiqum.designsystem.AntiqumColors

private const val WORLD_LATITUDE = 20.0
private const val WORLD_LONGITUDE = 0.0
private const val WORLD_ZOOM = 1.5f
private const val USER_LOCATION_ZOOM = 12f

@Composable
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
    if (BuildConfig.MAPS_API_KEY.startsWith("YOUR_")) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add MAPS_API_KEY to mobile/secrets.properties to load Google Maps.",
                modifier = Modifier.padding(32.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentSelectionHandler by rememberUpdatedState(onMuseumSelected)
    val currentLocationHandler by rememberUpdatedState(onUserLocationResolved)
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            resolveCurrentLocation(context, currentLocationHandler)
        } else {
            currentLocationHandler(null)
        }
    }
    val mapView = remember(context) {
        MapView(
            context,
            GoogleMapOptions()
                .mapColorScheme(if (darkTheme) MapColorScheme.DARK else MapColorScheme.LIGHT)
        ).also { view ->
            view.onCreate(null)
            view.getMapAsync { map ->
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
                map.uiSettings.apply {
                    isCompassEnabled = false
                    isMapToolbarEnabled = false
                    isMyLocationButtonEnabled = false
                    isZoomControlsEnabled = false
                }
                map.setOnMarkerClickListener { marker ->
                    (marker.tag as? String)?.let(currentSelectionHandler)
                    true
                }
                googleMap = map
            }
        }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = MuseumMapLifecycleObserver(mapView)
        lifecycle.addObserver(observer)
        observer.syncTo(lifecycle.currentState)
        onDispose {
            lifecycle.removeObserver(observer)
            observer.destroy()
        }
    }

    val regularColor = if (darkTheme) AntiqumColors.ForestDark.toArgb() else AntiqumColors.Forest.toArgb()
    val selectedColor = AntiqumColors.Bronze.toArgb()
    val regularIcon = remember(darkTheme) { museumMarkerIcon(regularColor, selected = false, context.resources.displayMetrics.density) }
    val selectedIcon = remember(darkTheme) { museumMarkerIcon(selectedColor, selected = true, context.resources.displayMetrics.density) }

    LaunchedEffect(requestUserLocation) {
        if (!requestUserLocation) return@LaunchedEffect
        if (context.hasLocationPermission()) {
            resolveCurrentLocation(context, currentLocationHandler)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(googleMap, markers, selectedMuseumId, darkTheme) {
        googleMap?.let { map ->
            map.setMapColorScheme(if (darkTheme) MapColorScheme.DARK else MapColorScheme.LIGHT)
            val density = context.resources.displayMetrics.density
            map.setPadding(0, (116 * density).toInt(), 0, ((if (selectedMuseumId == null) 68 else 178) * density).toInt())
            map.clear()
            markers.forEach { marker ->
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(marker.latitude, marker.longitude))
                        .title(marker.name)
                        .snippet(marker.category)
                        .anchor(0.5f, 0.5f)
                        .icon(if (marker.id == selectedMuseumId) selectedIcon else regularIcon)
                        .zIndex(if (marker.id == selectedMuseumId) 2f else 1f)
                )?.tag = marker.id
            }
        }
    }

    LaunchedEffect(googleMap, userLocation, cameraRequestId) {
        val location = userLocation
        val target = if (location == null) {
            LatLng(WORLD_LATITUDE, WORLD_LONGITUDE) to WORLD_ZOOM
        } else {
            LatLng(location.latitude, location.longitude) to USER_LOCATION_ZOOM
        }
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(target.first, target.second))
    }

    LaunchedEffect(googleMap, userLocation) {
        googleMap?.let { map ->
            try {
                map.isMyLocationEnabled = userLocation != null && context.hasLocationPermission()
            } catch (_: SecurityException) {
                map.isMyLocationEnabled = false
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

private fun Context.hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
    this,
    Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun resolveCurrentLocation(
    context: Context,
    onResolved: (MuseumMapLocation?) -> Unit
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
        onResolved(null)
        return
    }
    val provider = when {
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        else -> null
    }
    if (provider == null) {
        onResolved(null)
        return
    }
    try {
        val lastKnownLocation = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER
        ).mapNotNull { candidate ->
            if (locationManager.isProviderEnabled(candidate)) {
                locationManager.getLastKnownLocation(candidate)
            } else {
                null
            }
        }.maxByOrNull { it.time }
        if (lastKnownLocation != null) {
            onResolved(MuseumMapLocation(lastKnownLocation.latitude, lastKnownLocation.longitude))
            return
        }

        @Suppress("DEPRECATION")
        locationManager.requestSingleUpdate(
            provider,
            object : android.location.LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    onResolved(MuseumMapLocation(location.latitude, location.longitude))
                }
            },
            Looper.getMainLooper()
        )
    } catch (_: SecurityException) {
        onResolved(null)
    }
}

private fun museumMarkerIcon(
    fillColor: Int,
    selected: Boolean,
    density: Float
): BitmapDescriptor {
    val size = ((if (selected) 50 else 42) * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val radius = center - 3f * density
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = Color.argb(55, 0, 0, 0)
    canvas.drawCircle(center, center + 2f * density, radius, paint)
    paint.color = fillColor
    canvas.drawCircle(center, center, radius, paint)
    if (selected) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f * density
        paint.color = Color.WHITE
        canvas.drawCircle(center, center, radius - 1.25f * density, paint)
        paint.style = Paint.Style.FILL
    }

    paint.color = Color.WHITE
    val iconWidth = 20f * density
    val iconLeft = center - iconWidth / 2f
    val roofTop = center - 9f * density
    val roofBottom = center - 3f * density
    val roof = Path().apply {
        moveTo(center, roofTop)
        lineTo(iconLeft, roofBottom)
        lineTo(iconLeft + iconWidth, roofBottom)
        close()
    }
    canvas.drawPath(roof, paint)
    paint.strokeWidth = 2.2f * density
    paint.strokeCap = Paint.Cap.SQUARE
    repeat(3) { index ->
        val x = iconLeft + (4f + index * 6f) * density
        canvas.drawLine(x, roofBottom + 2f * density, x, center + 7f * density, paint)
    }
    canvas.drawRect(iconLeft, center + 7f * density, iconLeft + iconWidth, center + 10f * density, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private class MuseumMapLifecycleObserver(private val mapView: MapView) : LifecycleEventObserver {
    private var started = false
    private var resumed = false
    private var destroyed = false

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> start()
            Lifecycle.Event.ON_RESUME -> resume()
            Lifecycle.Event.ON_PAUSE -> pause()
            Lifecycle.Event.ON_STOP -> stop()
            Lifecycle.Event.ON_DESTROY -> destroy()
            else -> Unit
        }
    }

    fun syncTo(state: Lifecycle.State) {
        if (state.isAtLeast(Lifecycle.State.STARTED)) start()
        if (state.isAtLeast(Lifecycle.State.RESUMED)) resume()
    }

    private fun start() {
        if (!started && !destroyed) {
            mapView.onStart()
            started = true
        }
    }

    private fun resume() {
        start()
        if (!resumed && !destroyed) {
            mapView.onResume()
            resumed = true
        }
    }

    private fun pause() {
        if (resumed) {
            mapView.onPause()
            resumed = false
        }
    }

    private fun stop() {
        pause()
        if (started) {
            mapView.onStop()
            started = false
        }
    }

    fun destroy() {
        if (!destroyed) {
            stop()
            mapView.onDestroy()
            destroyed = true
        }
    }
}
