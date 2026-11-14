package com.arin.civicreport.ui.screens

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.Authority as AuthorityModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorityFinderScreen(
    incidentTypes: List<String>,
    latitude: Double,
    longitude: Double,
    address: String,
    state: String,
    onContinue: (List<AuthorityModel>) -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<List<AuthorityModel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var authorities by remember { mutableStateOf<List<AuthorityModel>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(incidentTypes, state) {
        try {
            loading = true
            val repo = Repository(com.arin.civicreport.data.CivicDatabase.getInstance(context))
            repo.seedAuthorities()
            val tag = incidentTypes.firstOrNull() ?: "pothole"
            authorities = repo.getAuthoritiesForIncident(tag, state, "")
        } catch (e: Exception) {
            error = "Could not load authorities: ${e.message}"
        } finally {
            loading = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Concern Authorities", fontWeight = FontWeight.Bold) })
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text("Auto-detected jurisdiction: $state (local)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("App matches ${incidentTypes.joinToString(" or ")} incidents to relevant authorities. Select who you want to notify.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (error != null) {
                Text("Error: $error", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            } else if (authorities.isEmpty()) {
                Text("No authorities found for this incident and location. Add custom authorities in Settings.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn {
                    items(authorities) { a ->
                        val isSelected = selected.contains(a.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = if (isSelected) selected - a.id else selected + a.id }
                                .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.CircleOutline,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(a.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Level: ${a.level}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Phone: ${a.phone}  Email: ${a.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (selected.isEmpty()) {
                        AlertDialog(onDismissRequest = { }, title = { Text("Select authorities") }, text = { Text("Choose at least one authority to notify.") }, confirmButton = { TextButton(onClick = { }) { Text("OK") } })
                    } else {
                        onContinue(selected)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selected.isNotEmpty()
            ) {
                Text("Continue")
            }
        }
    }
}
