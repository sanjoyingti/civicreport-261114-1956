package com.arin.civicreport.ui.location

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
private const val MOVE_EPSILON = 0.000001

/** A simple OpenStreetMap view: shows a pin and reports the spot the user taps. */
@Composable
fun MapViewProvider(
    initialPoint: GeoPoint?,
    onPointChanged: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val markerState = remember { mutableStateOf<Marker?>(null) }
    val currentCallback = rememberUpdatedState(onPointChanged)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewState.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewState.value?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            markerState.value = null
            mapViewState.value?.onDetach()
            mapViewState.value = null
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            Configuration.getInstance()
                .load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

            val startPoint = initialPoint?.let { OsmGeoPoint(it.latitude, it.longitude) }
                ?: OsmGeoPoint(DEFAULT_LAT, DEFAULT_LON)

            val map = MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                setUseDataConnection(true)
                minZoomLevel = 3.0
                maxZoomLevel = 20.0
                controller.setZoom(16.0)
                controller.setCenter(startPoint)
            }

            val marker = Marker(map).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                isDraggable = false
                position = startPoint
            }
            map.overlays.add(marker)

            val events = MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: OsmGeoPoint): Boolean {
                    marker.position = p
                    map.controller.animateTo(p)
                    map.invalidate()
                    currentCallback.value(GeoPoint(p.latitude, p.longitude))
                    return true
                }

                override fun longPressHelper(p: OsmGeoPoint): Boolean = false
            })
            map.overlays.add(events)

            mapViewState.value = map
            markerState.value = marker
            map
        },
        update = { map ->
            val point = initialPoint ?: return@AndroidView
            val marker = markerState.value ?: return@AndroidView
            val target = OsmGeoPoint(point.latitude, point.longitude)
            val currentPosition = marker.position
            val moved = currentPosition == null ||
                Math.abs(currentPosition.latitude - target.latitude) > MOVE_EPSILON ||
                Math.abs(currentPosition.longitude - target.longitude) > MOVE_EPSILON
            if (moved) {
                marker.position = target
                map.controller.animateTo(target)
                map.invalidate()
            }
        }
    )
}
