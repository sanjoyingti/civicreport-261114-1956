package com.arin.civicreport.ui.location

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class MapViewProviderState(
    val mapView: MapView
)

@Composable
fun MapViewProvider(
    initialPoint: GeoPoint? = null,
    initialAddress: String = "",
    onPointChanged: (GeoPoint) -> Unit
): MapViewProviderState? {
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var selectedPoint by remember(initialPoint) { mutableStateOf(initialPoint) }
    var state by remember { mutableStateOf<MapViewProviderState?>(null) }

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
            val center = selectedPoint ?: GeoPoint(19.0760, 72.8777)
            controller.setCenter(center)
            overlayManager.add(Marker(this).apply {
                position = center
                title = "Pin"
                setInfoWindow(null)
            })
        }
        mapView = m
        state = MapViewProviderState(m)
        registry.currentState = Lifecycle.State.STARTED
        registry.currentState = Lifecycle.State.RESUMED
        onDispose {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    return state
}
