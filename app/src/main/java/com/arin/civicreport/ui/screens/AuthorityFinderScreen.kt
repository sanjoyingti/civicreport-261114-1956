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
    onContinue: (List<AuthorityModel>) -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<List<AuthorityModel>>(emptyList()) }
    val authorities = remember { loadAuthorities(context, incidentTypes, "Maharashtra") }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Concern Authorities", fontWeight = FontWeight.Bold) })
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text("Auto-detected jurisdiction: Maharashtra (local)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("App matches ${incidentTypes.joinToString(" or ")} incidents to relevant authorities. Select who you want to notify.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
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

private fun loadAuthorities(context: Context, types: List<String>, state: String): List<com.arin.civicreport.model.Authority> {
    // Use the Repository when an activity is available; otherwise return a small default list.
    return try {
        val repo = com.arin.civicreport.data.CivicDatabase.getInstance(context).let {
            com.arin.civicreport.data.Repository(it)
        }
        val tag = types.firstOrNull() ?: "pothole"
        repo.getAuthoritiesForIncident(tag, state, "").also { repo.seedAuthorities() }
    } catch (e: Exception) {
        // fallback default authorities
        listOf(
            com.arin.civicreport.model.Authority("mh_cm_office", "Chief Minister Office, Maharashtra", "020 2612 8060", "cmo@maharashtra.gov.in", "State", emptyList(), listOf("Maharashtra"), emptyList(), 5),
            com.arin.civicreport.model.Authority("pmo_india", "Prime Minister's Office (PMO)", "1553", "pmo@pmindia.gov.in", "Central", emptyList(), emptyList(), emptyList(), 10),
            com.arin.civicreport.model.Authority("maharashtra_mumbai_municipal", "BMC Engineering Dept", "15331", "complaints@bmcgov.com", "Local", emptyList(), listOf("Maharashtra"), listOf("mumbai"), 2)
        )
    }
}
