package com.arin.civicreport.ui.screens

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository

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
    var lat by remember { mutableStateOf(latitude) }
    var lon by remember { mutableStateOf(longitude) }
    var addr by remember { mutableStateOf(address) }
    var description by remember { mutableStateOf("") }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Location & Details", fontWeight = FontWeight.Bold) })
    }, floatingActionButton = {
        FloatingActionButton(onClick = { onLocationConfirmed(lat, lon, addr) }) {
            Icon(Icons.Default.Check, contentDescription = "Confirm location")
        }
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Card(modifier = Modifier
                .aspectRatio(1f)
                .clickable {
                    lat = 19.0760 + (Math.random() * 0.1 - 0.05); lon = 72.8777 + (Math.random() * 0.1 - 0.05)
                }
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Map not available in this demo; tap to adjust pin", style = MaterialTheme.typography.bodyMedium)
                        Text("Lat: ${"%.6f".format(lat)}  Lon: ${"%.6f".format(lon)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Detected address: ${addr.ifBlank { "unknown" }}", style = MaterialTheme.typography.bodyLarge)
                    Text("Lat: ${"%.6f".format(lat)}", style = MaterialTheme.typography.bodySmall)
                    Text("Lon: ${"%.6f".format(lon)}", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Add description or notes...") })
                }
            }
        }
    }
}
