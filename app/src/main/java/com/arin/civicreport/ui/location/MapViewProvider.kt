package com.arin.civicreport.ui.location

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

private const val DEFAULT_LAT = 19.0760
private const val DEFAULT_LON = 72.8777
private const val DEFAULT_ZOOM = 16.0

/** A simple OpenStreetMap view: shows a pin and reports the spot the user taps. */
@Composable
fun MapViewProvider(
    initialPoint: GeoPoint?,
    onPointChanged: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapState = remember { mutableStateOf<MapView?>(null) }
    val markerState = remember { mutableStateOf<Marker?>(null) }
    val currentCallback = rememberUpdatedState(onPointChanged)

    DisposableEffect(Unit) {
        val config = Configuration.getInstance()
        config.userAgentValue = context.packageName
        config.load(context, context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE))
        onDispose {
            markerState.value = null
            mapState.value?.onDetach()
            mapState.value = null
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapState.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapState.value?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val map = MapView(ctx)
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)
            map.setBuiltInZoomControls(false)
            map.controller.setZoom(DEFAULT_ZOOM)
            val start = initialPoint?.let { OsmGeoPoint(it.latitude, it.longitude) }
                ?: OsmGeoPoint(DEFAULT_LAT, DEFAULT_LON)
            map.controller.setCenter(start)
            val marker = Marker(map).apply {
                position = start
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Reported spot"
                isDraggable = false
            }
            map.overlays.add(marker)
            markerState.value = marker
            val events = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: OsmGeoPoint): Boolean {
                    marker.position = p
                    map.invalidate()
                    currentCallback.value(GeoPoint(p.latitude, p.longitude))
                    return true
                }

                override fun longPressHelper(p: OsmGeoPoint): Boolean = false
            })
            map.overlays.add(events)
            mapState.value = map
            map
        }
    )

    LaunchedEffect(initialPoint?.latitude, initialPoint?.longitude) {
        val point = initialPoint ?: return@LaunchedEffect
        val map = mapState.value ?: return@LaunchedEffect
        val marker = markerState.value ?: return@LaunchedEffect
        val target = OsmGeoPoint(point.latitude, point.longitude)
        marker.position = target
        map.controller.setCenter(target)
        map.invalidate()
    }
}
