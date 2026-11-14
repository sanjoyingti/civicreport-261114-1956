package com.arin.civicreport.ui.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapViewProvider(
    initialPoint: GeoPoint? = null,
    initialAddress: String = "",
    onPointChanged: (GeoPoint) -> Unit
): MapView {
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var selectedPoint by remember(initialPoint) { mutableStateOf(initialPoint) }

    val registry = remember {
        LifecycleRegistry(lifecycleOwner).apply {
            currentState = Lifecycle.State.CREATED
        }
    }

    DisposableEffect(Unit) {
        val m = MapView(view.context).apply {
            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(selectedPoint ?: GeoPoint(19.0760, 72.8777))
            overlayManager.add(Marker(this).apply {
                position = selectedPoint ?: GeoPoint(19.0760, 72.8777)
                title = "Pin"
                setInfoWindow(null)
            })
            setOnMapClickListener { _, _ ->
                // Map click handled per-MapView instance in LocationDetailsScreen
            }
        }
        mapView = m
        registry.currentState = Lifecycle.State.STARTED
        registry.currentState = Lifecycle.State.RESUMED
        onDispose {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    return mapView!!
}
