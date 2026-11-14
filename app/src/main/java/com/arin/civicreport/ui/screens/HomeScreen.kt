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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.IncidentStatus
import com.arin.civicreport.ui.util.Prefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repo: Repository,
    prefs: Prefs,
    onOpenSettings: () -> Unit,
    onReportNew: () -> Unit,
    onOpenReport: (Long) -> Unit
) {
    val reports = repo.getReports().collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DuarBagori Reports", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onReportNew,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Report Incident") },
                modifier = Modifier.clip(CircleShape)
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp)) {
            Text("Recent Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (reports.value.isEmpty()) {
                Text("No reports yet", style = MaterialTheme.typography.bodyLarge)
                Text("Tap the + button to report your first incident", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(reports.value) { report ->
                        ReportCard(
                            report = report,
                            onClick = { onOpenReport(report.id) },
                            onDelete = { showDeleteDialog = report.id }
                        )
                    }
                }
            }
        }
    }

    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete report") },
            text = { Text("Delete this report? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.deleteReport(id) }; showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ReportCard(
    report: com.arin.civicreport.model.Report,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(report.incidentTypes.joinToString(", "), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("${report.address.ifBlank { "Location unknown" }} • ${formatDate(report.timestamp)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
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
        }
    }

    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(text = { Text("View") }, onClick = { showMenu = false })
        DropdownMenuItem(text = { Text("Resend") }, onClick = { showMenu = false })
        DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
    }
}

private fun formatDate(epochMs: Long): String {
    val d = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return d.java.util.Date(epochMs).toString()
}
