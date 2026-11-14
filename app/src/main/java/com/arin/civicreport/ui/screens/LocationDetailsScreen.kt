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
import com.arin.civicreport.ui.location.GeoPoint
import com.arin.civicreport.ui.screens.report.ReportViewModel
import com.arin.civicreport.ui.util.Prefs
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

    var lat by remember { mutableStateOf(latitude) }
    var lon by remember { mutableStateOf(longitude) }
    var addr by remember { mutableStateOf(address) }
    var description by remember { mutableStateOf("") }
    var mapPoint by remember { mutableStateOf<GeoPoint?>(null) }

    val locationManager = remember { LocationManager(context) }

    LaunchedEffect(Unit) {
        if (prefs.enableLocationServices) {
            val loc = locationManager.getCurrentLocation()
            lat = loc.latitude
            lon = loc.longitude
            addr = loc.address
        }
    }

    fun saveToBackStack() {
        reportViewModel.setReportId(-1L)
    }

    fun onConfirm() {
        saveToBackStack()
        onLocationConfirmed(lat, lon, addr.ifBlank { "$lat, $lon" })
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Location & Details", fontWeight = FontWeight.Bold) })
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
                    MapViewProvider(
                        initialPoint = mapPoint ?: GeoPoint(latitude, longitude),
                        initialAddress = addr
                    ) { point ->
                        mapPoint = point
                        lat = point.latitude
                        lon = point.longitude
                        addr = "${point.latitude}, ${point.longitude}"
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Tap Confirm to save this location. You can move the map pin to refine it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
