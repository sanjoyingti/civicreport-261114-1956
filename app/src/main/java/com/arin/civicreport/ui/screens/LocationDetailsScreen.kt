package com.arin.civicreport.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.ui.location.CurrentLocation
import com.arin.civicreport.ui.location.GeoPoint
import com.arin.civicreport.ui.location.LocationManager
import com.arin.civicreport.ui.location.MapViewProvider
import com.arin.civicreport.ui.screens.report.ReportFlowViewModel
import com.arin.civicreport.ui.util.PermissionManager
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailsScreen(
    reportViewModel: ReportFlowViewModel,
    permissionManager: PermissionManager,
    onBack: () -> Unit,
    onConfirmed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationManager = remember { LocationManager(context) }
    val draft by reportViewModel.draft.collectAsState()

    var latitude by remember { mutableStateOf(draft.latitude) }
    var longitude by remember { mutableStateOf(draft.longitude) }
    var address by remember { mutableStateOf(draft.address) }
    var city by remember { mutableStateOf(draft.city) }
    var district by remember { mutableStateOf(draft.district) }
    var state by remember { mutableStateOf(draft.state) }
    var pincode by remember { mutableStateOf(draft.pincode) }
    var description by remember { mutableStateOf(draft.description) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun applyLocation(loc: CurrentLocation) {
        latitude = loc.latitude
        longitude = loc.longitude
        address = loc.address
        city = loc.city
        district = loc.district
        state = loc.state
        pincode = loc.pincode
    }

    LaunchedEffect(Unit) {
        if (!draft.hasLocation && locationManager.hasLocationPermission()) {
            busy = true
            val loc = locationManager.getCurrentLocation()
            busy = false
            if (loc.hasFix) applyLocation(loc)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            scope.launch {
                busy = true
                val loc = locationManager.getCurrentLocation()
                busy = false
                if (loc.hasFix) applyLocation(loc)
                else error = "No GPS fix yet. Tap the map to place the pin manually."
            }
        } else {
            error = "Location permission denied. Tap the map to place the pin manually."
        }
    }

    fun confirm() {
        if (latitude == 0.0 && longitude == 0.0) {
            error = "Place the pin on the map or use your current position first."
            return
        }
        val finalAddress = address.ifBlank {
            String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
        }
        reportViewModel.setLocation(latitude, longitude, finalAddress, city, district, state, pincode)
        reportViewModel.setDescription(description)
        onConfirmed()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Location & details", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        error ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Card(modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))) {
                MapViewProvider(
                    initialPoint = if (latitude != 0.0 || longitude != 0.0)
                        GeoPoint(latitude, longitude) else null
                ) { point ->
                    scope.launch {
                        val loc = locationManager.reverseGeocode(point.latitude, point.longitude)
                        applyLocation(loc)
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    if (locationManager.hasLocationPermission()) {
                        scope.launch {
                            busy = true
                            val loc = locationManager.getCurrentLocation()
                            busy = false
                            if (loc.hasFix) applyLocation(loc)
                            else error = "No GPS fix yet. Tap the map instead."
                        }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Finding your position..." else "Use my current position")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Detected address: ${address.ifBlank { "unknown - tap the map" }}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        String.format(Locale.US, "Lat %.6f  Lon %.6f", latitude, longitude),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (state.isNotBlank()) {
                        Text(
                            "State: $state${if (district.isNotBlank()) "  District: $district" else ""}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description or notes") },
                minLines = 3
            )

            Button(onClick = { confirm() }, modifier = Modifier.fillMaxWidth()) {
                Text("Confirm location and continue")
            }
            Text(
                "Tap anywhere on the map to move the pin to the exact spot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
