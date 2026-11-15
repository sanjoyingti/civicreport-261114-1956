=== ABOUT: The report detail screen, rewritten so its parameters match how the app opens it and every import exists ===
package com.arin.civicreport.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import com.arin.civicreport.ui.components.StatusBadge
import com.arin.civicreport.ui.components.formatDate
import com.arin.civicreport.ui.util.PermissionManager
import com.arin.civicreport.ui.util.Prefs
import com.arin.civicreport.ui.util.ReportSender
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportViewScreen(
    reportId: Long,
    repo: Repository,
    prefs: Prefs,
    permissionManager: PermissionManager,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reports by repo.getReports().collectAsState(initial = emptyList())
    val report = reports.firstOrNull { it.id == reportId }

    var authorities by remember { mutableStateOf<List<Authority>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reportId) {
        val loaded = mutableListOf<Authority>()
        report?.sentAuthorities?.forEach { id ->
            repo.getAuthority(id)?.let { loaded.add(it) }
        }
        if (loaded.isEmpty() && report != null) {
            report.incidentTypes.forEach { tag ->
                repo.getAuthoritiesForIncident(tag, report.state, report.city.lowercase())
                    .forEach { candidate ->
                        if (report.sentAuthorities.contains(candidate.id)) loaded.add(candidate)
                    }
            }
        }
        authorities = loaded.distinctBy { it.id }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Report details", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete report")
                }
            }
        )
    }) { padding ->
        if (report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading report...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        report.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        report.address.ifBlank { "Location unknown" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Filed: ${formatDate(report.timestamp)}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "GPS: ${"%.6f".format(report.latitude)}, ${"%.6f".format(report.longitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Media: ${report.mediaType}", style = MaterialTheme.typography.bodySmall)
                    if (report.description.isNotBlank()) {
                        Text("Notes: ${report.description}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(report.status)
                Text("Method: ${report.sendMethod}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Notified offices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (authorities.isEmpty()) {
                Text(
                    report.sentAuthorities.joinToString(", ").ifBlank { "None recorded" },
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                authorities.forEach { authority ->
                    Text(
                        "${authority.name} • ${authority.email} • ${authority.phone}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (notice != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(notice ?: "", color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = {
                    val targets = authorities
                    if (targets.isEmpty()) {
                        notice = "No saved contacts for this report, so nothing could be resent."
                    } else {
                        permissionManager.requestNotificationsPermission()
                        ReportSender.send(context, targets, report, report.sendMethod)
                        notice = "Complaint re-opened in email/SMS for the same offices."
                        scope.launch {
                            repo.updateStatus(report.id, report.status)
                        }
                    }
                }
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.height(0.dp))
                Text("Resend to same offices")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete report?") },
            text = { Text("The saved record is removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        repo.deleteReport(reportId)
                        onDeleted()
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
