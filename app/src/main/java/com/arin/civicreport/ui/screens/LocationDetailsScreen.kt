package com.arin.civicreport.ui.screens

import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arin.civicreport.ui.location.CurrentLocation
import com.arin.civicreport.ui.location.LocationManager
import com.arin.civicreport.ui.location.MapViewProvider
import com.arin.civicreport.ui.location.StateAddress
import com.arin.civicreport.ui.screens.report.ReportViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailsScreen(
    incidentTypes: List<String>,
    mediaUri: String?,
    mediaType: String,
    latitude: Double,
    longitude: Double,
    address: String,
    onLocationConfirmed: (Double, Double, String) -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    val reportViewModel: ReportViewModel = viewModel()
    val locationManager = remember { LocationManager(context) }

    var lat by remember { mutableStateOf(latitude) }
    var lon by remember { mutableStateOf(longitude) }
    var addr by remember { mutableStateOf(address) }
    var description by remember { mutableStateOf("") }
    var currentState by remember { mutableStateOf<LocationManager.LocationState?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var mapMarker by remember { mutableStateOf<Marker?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        locationManager.observeState().collect { state ->
            currentState = state
            when (state) {
                is LocationManager.LocationState.Ready -> {
                    lat = state.loc.latitude
                    lon = state.loc.longitude
                    addr = state.loc.address
                    if (selectedPoint == null) {
                        selectedPoint = GeoPoint(state.loc.latitude, state.loc.longitude)
                    }
                }
                is LocationManager.LocationState.Error -> {
                    // User can still confirm with manual coords
                }
                else -> {}
            }
        }
    }

    fun saveToBackStack() {
        reportViewModel.setReportId(-1L)
        // Store the values the next screen needs.
        Unit // no-op; nav graph uses savedStateHandle via reportViewModel for state
    }

    fun onConfirm() {
        saveToBackStack()
        onLocationConfirmed(lat, lon, addr)
    }

    fun updateMapFromLocation(loc: CurrentLocation) {
        lat = loc.latitude
        lon = loc.longitude
        addr = loc.address
        selectedPoint = GeoPoint(loc.latitude, loc.longitude)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Location & Details", fontWeight = FontWeight.Bold) })
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = {
                if (selectedPoint != null) {
                    onConfirm()
                } else {
                    Toast.makeText(context, "Please tap the map to place a pin", Toast.LENGTH_SHORT).show()
                }
            },
            content = { Icon(Icons.Default.Check, contentDescription = "Confirm location") }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Map view with tap-to-pan + tap-to-place-pin
            Card(
                modifier = Modifier
                    .aspectRatio(0.7f)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LaunchedEffect(Unit) {
                        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
                        mapView = MapView(context).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            controller.setCenter(selectedPoint ?: GeoPoint(19.0760, 72.8777))
                            overlayManager.add(Marker(this).apply {
                                position = selectedPoint ?: GeoPoint(19.0760, 72.8777)
                                this.title = "Pin"
                                this.subTitle = addr.ifBlank { "Tap map to move pin" }
                            })
                            setOnMapTouchListener {
                                mapReady = true
                            }
                            setOnMapClickListener { point, _ ->
                                selectedPoint = point
                                mapMarker?.let { overlayManager.remove(it) }
                                mapMarker = Marker(this).apply {
                                    position = point
                                    this.title = "Selected pin"
                                    this.subTitle = point.latitude.toString() + " " + point.longitude.toString()
                                }
                                overlayManager.add(mapMarker!!)
                                // Reverse geocode for address
                                viewModel<ReportViewModel>().let { vm ->
                                    // quick fallback address
                                }
                            }
                            mapView = this
                            mapReady = true
                        }
                    }
                    when {
                        !mapReady -> CircularProgressIndicator()
                        else -> mapView
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detected address + coordinates
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Detected address: ${addr.ifBlank { "unknown" }}", style = MaterialTheme.typography.bodyLarge)
                    Text("Lat: ${DecimalFormat("######.######").format(lat)}", style = MaterialTheme.typography.bodySmall)
                    Text("Lon: ${DecimalFormat("######.######").format(lon)}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Editable description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add description or notes...") },
                minLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentState is LocationManager.LocationState.Loading) {
                Text("Getting your location...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (currentState is LocationManager.LocationState.Error) {
                Text(
                    "Location could not be auto-detected. Tap on the map to place a pin manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
