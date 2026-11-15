package com.arin.civicreport.ui.location

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
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
        config.load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
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
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(DEFAULT_ZOOM)
                val start = if (initialPoint != null) {
                    OsmGeoPoint(initialPoint.latitude, initialPoint.longitude)
                } else {
                    OsmGeoPoint(DEFAULT_LAT, DEFAULT_LON)
                }
                controller.setCenter(start)
                if (initialPoint != null) {
                    addMarker(this, markerState, initialPoint)
                }
                overlays.add(
                    MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: OsmGeoPoint): Boolean {
                            val point = GeoPoint(p.latitude, p.longitude)
                            val existing = markerState.value
                            if (existing != null) {
                                existing.position = p
                                invalidate()
                            } else {
                                addMarker(this@apply, markerState, point)
                            }
                            currentCallback.value(point)
                            return true
                        }

                        override fun longPressHelper(p: OsmGeoPoint): Boolean = false
                    })
                )
                mapState.value = this
            }
        }
    )
}

private fun addMarker(map: MapView, markerState: MutableState<Marker?>, point: GeoPoint) {
    val marker = Marker(map).apply {
        position = OsmGeoPoint(point.latitude, point.longitude)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = "Reported spot"
    }
    map.overlays.add(marker)
    markerState.value = marker
    map.invalidate()
}
