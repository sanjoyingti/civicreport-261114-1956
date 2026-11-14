package com.arin.civicreport.ui.location

import android.util.TypedValue
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapViewProvider(
    initialPoint: GeoPoint? = null,
    initialAddress: String = "",
    onPointChanged: (GeoPoint) -> Unit
): MapView? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Ensure osmdroid Configuration is initialized with context
    Configuration.getInstance().load(context, context.applicationContext.getSharedPreferences("osmdroid", 0))

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var selectedPoint by remember(initialPoint) { mutableStateOf(initialPoint) }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                val center = selectedPoint ?: GeoPoint(19.0760, 72.8777)
                controller.setCenter(center)
                overlayManager.clear()
                overlayManager.add(Marker(this).apply {
                    position = center
                    title = "Pin"
                    setInfoWindow(null)
                })
                setOnMapListener(object : MapView.OnMapListener {
                    override fun onMapMove() {
                        onPointChanged(controller.viewportCenter)
                    }

                    override fun onMapZoom() {}
                    override fun onMarkerDragStart(marker: Marker?) {}
                    override fun onMarkerDrag(marker: Marker?) {}
                    override fun onMarkerDragEnd(marker: Marker?) {}
                    override fun onSingleTapConfirmed(geometryPoint: org.osmdroid.util.GeometryPoint?) {}
                })
            }
        },
        update = { view ->
            mapView = view
            view.controller.setZoom(16.0)
            val center = selectedPoint ?: GeoPoint(19.0760, 72.8777)
            view.controller.setCenter(center)
            view.overlayManager.clear()
            view.overlayManager.add(Marker(view).apply {
                position = center
                title = "Pin"
                setInfoWindow(null)
            })
        },
        onReset = {}
    )

    return mapView
}
