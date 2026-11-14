package com.arin.civicreport.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.ui.util.Prefs
import com.arin.civicreport.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repo: Repository,
    prefs: Prefs,
    onBack: () -> Unit,
    onRequestPermission: (String) -> Unit,
    onAddAuthority: () -> Unit
) {
    val context = LocalContext.current
    var editMode by remember { mutableStateOf(false) }
    var authorityName by remember { mutableStateOf("") }
    var authorityPhone by remember { mutableStateOf("") }
    var authorityEmail by remember { mutableStateOf("") }
    var authorityLevel by remember { mutableStateOf("Local") }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            SwitchListRow(title = "Location services", subtitle = "Auto-detect GPS coordinates", checked = prefs.enableLocationServices) {
                prefs.enableLocationServices = it
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Default contact method", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                SegmentedButtonRow(selected = prefs.defaultSendMethod, onChange = { prefs.defaultSendMethod = it })
            }
            Spacer(modifier = Modifier.height(4.dp))
            SwitchListRow(title = "Notifications", subtitle = "Show send confirmations", checked = prefs.notificationsEnabled) {
                prefs.notificationsEnabled = it
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Permission help", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap to request camera/location/SMS permissions if the app asks for them.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onRequestPermission(android.Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 8.dp)) { Text("Request Camera") }
                    Button(onClick = { onRequestPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) }, modifier = Modifier.padding(top = 8.dp)) { Text("Request Location") }
                    Button(onClick = { onRequestPermission(android.Manifest.permission.SEND_SMS) }, modifier = Modifier.padding(top = 8.dp)) { Text("Request SMS") }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Edit authorities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (editMode) {
                        OutlinedTextField(value = authorityName, onValueChange = { authorityName = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Name") })
                        OutlinedTextField(value = authorityPhone, onValueChange = { authorityPhone = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Phone") })
                        OutlinedTextField(value = authorityEmail, onValueChange = { authorityEmail = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Email") })
                        OutlinedTextField(value = authorityLevel, onValueChange = { authorityLevel = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Local/State/Central") })
                        Button(onClick = {
                            val a = com.arin.civicreport.model.Authority("custom_${System.currentTimeMillis()}", authorityName, authorityPhone, authorityEmail, authorityLevel, emptyList(), emptyList(), emptyList(), 99)
                            repo.addAuthority(a)
                            authorityName = ""; authorityPhone = ""; authorityEmail = ""; authorityLevel = "Local"
                            editMode = false
                            android.app.AlertDialog.Builder(context).setTitle("Added").setMessage("Authority saved locally.").setPositiveButton("OK") { _, _ -> }.show()
                        }, modifier = Modifier.padding(top = 8.dp)) { Text("Add authority") }
                    } else {
                        Button(onClick = { editMode = true }, modifier = Modifier.padding(top = 8.dp)) { Text("Add authority") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("App version: ${BuildConfig.VERSION_NAME ?: "1.0"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Developer: CivicReport team. All data is stored locally on your device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Exception) {}
                    }) {
                        Text("Open system location settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchListRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SegmentedButtonRow(selected: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SendChip(selected == "email", "Email") { onChange("email") }
        SendChip(selected == "sms", "SMS") { onChange("sms") }
        SendChip(selected == "both", "Both") { onChange("both") }
    }
}

@Composable
private fun SendChip(selected: Boolean, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
