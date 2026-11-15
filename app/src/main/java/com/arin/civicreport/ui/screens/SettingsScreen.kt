=== ABOUT: The settings screen, fixed to use the shared permission helper instead of a parameter that was never passed ===
package com.arin.civicreport.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.Authority
import com.arin.civicreport.ui.util.PermissionManager
import com.arin.civicreport.ui.util.Prefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repo: Repository,
    prefs: Prefs,
    permissionManager: PermissionManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editMode by remember { mutableStateOf(false) }
    var authorityName by remember { mutableStateOf("") }
    var authorityPhone by remember { mutableStateOf("") }
    var authorityEmail by remember { mutableStateOf("") }
    var authorityLevel by remember { mutableStateOf("Local") }
    var notice by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {
            if (notice != null) {
                Text(notice ?: "", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }
            SwitchListRow(
                title = "Location services",
                subtitle = "Auto-detect GPS coordinates of a spot",
                checked = prefs.enableLocationServices
            ) { prefs.enableLocationServices = it }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Default contact method",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                SegmentedButtonRow(selected = prefs.defaultSendMethod, onChange = {
                    prefs.defaultSendMethod = it
                })
            }
            Spacer(modifier = Modifier.height(8.dp))
            SwitchListRow(
                title = "Notifications",
                subtitle = "Show send confirmations",
                checked = prefs.notificationsEnabled
            ) {
                prefs.notificationsEnabled = it
                if (it) permissionManager.requestNotificationsPermission()
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Permission help",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Grant camera, location and SMS access here if you skipped the prompts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PermissionButton("Request camera", permissionManager, Manifest.permission.CAMERA)
                    PermissionButton("Request location", permissionManager, Manifest.permission.ACCESS_FINE_LOCATION)
                    PermissionButton("Request SMS", permissionManager, Manifest.permission.SEND_SMS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionButton("Request notifications", permissionManager, Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Add an authority",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Added contacts are kept on this phone and appear in matching lookups.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (editMode) {
                        OutlinedTextField(
                            value = authorityName,
                            onValueChange = { authorityName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Name") }
                        )
                        OutlinedTextField(
                            value = authorityPhone,
                            onValueChange = { authorityPhone = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Phone") }
                        )
                        OutlinedTextField(
                            value = authorityEmail,
                            onValueChange = { authorityEmail = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email") }
                        )
                        OutlinedTextField(
                            value = authorityLevel,
                            onValueChange = { authorityLevel = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Level (Local / State / Central)") }
                        )
                        Button(
                            onClick = {
                                if (authorityName.isBlank()) {
                                    notice = "Enter at least a name for the authority."
                                    return@Button
                                }
                                if (authorityPhone.isBlank() && authorityEmail.isBlank()) {
                                    notice = "Add a phone number or an email address."
                                    return@Button
                                }
                                scope.launch {
                                    repo.addAuthority(
                                        Authority(
                                            id = "custom_${System.currentTimeMillis()}",
                                            name = authorityName,
                                            phone = authorityPhone,
                                            email = authorityEmail,
                                            level = authorityLevel.ifBlank { "Local" },
                                            activityTags = listOf("all"),
                                            applicableStates = emptyList(),
                                            applicableCities = emptyList(),
                                            order = 99
                                        )
                                    )
                                    notice = "$authorityName saved."
                                    authorityName = ""
                                    authorityPhone = ""
                                    authorityEmail = ""
                                    authorityLevel = "Local"
                                    editMode = false
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Save authority") }
                    } else {
                        Button(
                            onClick = { editMode = true; notice = null },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Add authority") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("DuarBagori Reports • version 1.0.0", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Everything you file stays on this device. Contacts can be edited above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Exception) {
                            notice = "System location settings could not be opened."
                        }
                    }) { Text("Open system location settings") }
                }
            }
        }
    }
}

@Composable
private fun PermissionButton(
    label: String,
    permissionManager: PermissionManager,
    permission: String
) {
    Button(
        onClick = {
            if (permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                permissionManager.requestLocationPermissions()
            } else {
                permissionManager.requestPermission(permission)
            }
        },
        modifier = Modifier.padding(top = 8.dp)
    ) { Text(label) }
}

@Composable
private fun SwitchListRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            .border(
                2.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
