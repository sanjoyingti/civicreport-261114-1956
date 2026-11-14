package com.arin.civicreport.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.arin.civicreport.ui.location.MapViewProviderState
import com.arin.civicreport.ui.screens.report.ReportViewModel
import com.arin.civicreport.ui.util.Prefs
import org.osmdroid.util.GeoPoint
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
    val prefs = remember { Prefs(context) }
    val reportViewModel: ReportViewModel = viewModel()
    val locationManager = remember { LocationManager(context) }

    var lat by remember { mutableStateOf(latitude) }
    var lon by remember { mutableStateOf(longitude) }
    var addr by remember { mutableStateOf(address) }
    var description by remember { mutableStateOf("") }
    var currentState by remember { mutableStateOf<LocationManager.LocationState?>(null) }
    var mapPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var pointState by remember { mutableStateOf<MapViewProviderState?>(null) }

    LaunchedEffect(Unit) {
        if (prefs.enableLocationServices) {
            locationManager.getCurrentLocation().let { loc ->
                lat = loc.latitude
                lon = loc.longitude
                addr = loc.address
            }
        }
        locationManager.state.collect { state ->
            currentState = state
        }
    }

    fun saveToBackStack() {
        reportViewModel.setReportId(-1L)
    }

    fun onConfirm() {
        saveToBackStack()
        onLocationConfirmed(lat, lon, addr)
    }

    fun updateMapFromLocation(loc: CurrentLocation) {
        lat = loc.latitude
        lon = loc.longitude
        addr = loc.address
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Location &amp; Details", fontWeight = FontWeight.Bold) })
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = { onConfirm() },
            content = { Icon(Icons.Default.Check, contentDescription = "Confirm location") }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .aspectRatio(0.7f)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    pointState = MapViewProvider(
                        initialPoint = if (mapPoint != null) mapPoint!! else GeoPoint(latitude, longitude),
                        initialAddress = addr
                    ) { point ->
                        mapPoint = point
                        lat = point.latitude
                        lon = point.longitude
                        addr = "${point.latitude}, ${point.longitude}"
                    }
                    if (pointState == null) {
                        CircularProgressIndicator()
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
