package com.arin.civicreport.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.IncidentStatus
import com.arin.civicreport.ui.util.Prefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportViewScreen(
    reportId: Long,
    repo: Repository,
    prefs: Prefs,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val report by remember { repo.getReports().collectAsState(initial = emptyList()).value.firstOrNull { it.id == reportId } }
        ?: run {
            LaunchedEffect(Unit) { onBack() }
            return@Composable {}
        }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Details", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* resend handled in menu */ },
                modifier = Modifier.clip(CircleShape),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Send, contentDescription = "Resend report")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            var showMenu by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showMenu = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(report.incidentTypes.joinToString(", "), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${report.address.ifBlank { "Location unknown" }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Date: ${formatDate(report.timestamp)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Description: ${report.description.ifBlank { "No description provided" }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Latitude: ${"%.6f".format(report.latitude)}  Longitude: ${"%.6f".format(report.longitude)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Media: ${report.mediaUris.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Sent to: ${report.sentAuthorities.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Send method: ${report.sendMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box {
                val badgeColor = when (report.status) {
                    IncidentStatus.Sent -> MaterialTheme.colorScheme.primary
                    IncidentStatus.Pending -> Color(0xFFFFA600)
                    IncidentStatus.Failed -> MaterialTheme.colorScheme.error
                    IncidentStatus.Draft -> MaterialTheme.colorScheme.secondaryContainer
                }
                Surface(color = badgeColor, shape = MaterialTheme.shapes.small) {
                    Text(
                        report.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (report.status == IncidentStatus.Failed) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Report options")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn {
                items(report.sentAuthorities) { it ->
                    Text("Notified: $it - ${formatDate(report.lastUpdated)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun formatDate(epochMs: Long): String {
    val d = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return d.java.util.Date(epochMs).toString()
}
