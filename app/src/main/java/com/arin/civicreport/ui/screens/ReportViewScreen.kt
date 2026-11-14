package com.arin.civicreport.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.Authority as AuthorityModel
import com.arin.civicreport.model.IncidentStatus
import com.arin.civicreport.ui.util.Prefs
import com.arin.civicreport.ui.util.PermissionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportViewScreen(
    reportId: Long,
    repo: Repository,
    prefs: Prefs,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    permissionManager: PermissionManager
) {
    val report = repo.getReports().collectAsState(initial = emptyList()).value.firstOrNull { it.id == reportId }
        ?: run {
            LaunchedEffect(Unit) { onBack() }
            return@Composable {}
        }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var showResendConfirm by remember { mutableStateOf(false) }

    var authoritiesById by remember { mutableStateOf<MutableMap<String, AuthorityModel>>(mutableMapOf()) }

    LaunchedEffect(report.sentAuthorities) {
        val map = mutableMapOf<String, AuthorityModel>()
        repo.getAuthoritiesForIncident(report.incidentTypes.firstOrNull() ?: "pothole", report.state, report.city)
            .forEach { map[it.id] = it }
        authoritiesById = map
    }

    fun formatAuthorities(): String {
        return report.sentAuthorities.joinToString(", ") { id ->
            authoritiesById[id]?.name ?: id
        }
    }

    fun sendToAuthority(ctx: Context, a: AuthorityModel, method: String, body: String, mediaUri: String?) {
        val subject = "Civic Incident Report: ${report.incidentTypes.firstOrNull() ?: "Incident"} at ${report.address}"
        if (method in listOf("email", "both") && a.email.isNotBlank()) {
            val mail = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${a.email}")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                mediaUri?.let {
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(it))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            try { ctx.startActivity(mail) } catch (e: Exception) {
                android.app.AlertDialog.Builder(ctx)
                    .setTitle("No email app")
                    .setMessage("Install an email app to send by email.")
                    .setPositiveButton("OK") { _, _ -> }
                    .show()
            }
        }
        if (method in listOf("sms", "both") && a.phone.isNotBlank()) {
            val sms = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${a.phone}")
                putExtra(Intent.EXTRA_TEXT, body)
            }
            try { ctx.startActivity(sms) } catch (e: Exception) {
                android.app.AlertDialog.Builder(ctx)
                    .setTitle("No SMS app")
                    .setMessage("Install an SMS app to send by SMS.")
                    .setPositiveButton("OK") { _, _ -> }
                    .show()
            }
        }
    }

    fun resendReport(report: com.arin.civicreport.model.Report) {
        val method = report.sendMethod
        authoritiesById.values.forEach { a ->
            if (report.sentAuthorities.contains(a.id)) {
                sendToAuthority(context, a, method, buildReportBody(report), report.mediaUris.firstOrNull())
            }
        }
    }

    fun buildReportBody(report: com.arin.civicreport.model.Report): String {
        return buildString {
            append("Civic Incident Report\n\n")
            append("Types: ${report.incidentTypes.joinToString(", ")}\n")
            append("Location: ${report.address.ifBlank { "unknown" }}\n")
            append("Latitude: ${"%.6f".format(report.latitude)}\n")
            append("Longitude: ${"%.6f".format(report.longitude)}\n")
            append("Media: ${report.mediaUris.joinToString(", ")} ($report.mediaType)\n")
            append("Description: ${report.description.ifBlank { "No description provided" }}\n")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Details", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showResendConfirm = true },
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
                    Text("Sent to: ${formatAuthorities()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Notified: ${authoritiesById[it]?.name ?: it} - ${formatDate(report.lastUpdated)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    showMenu?.let {
        DropdownMenu(expanded = it, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(text = { Text("View") }, onClick = { showMenu = false })
            DropdownMenuItem(text = { Text("Resend") }, onClick = { showMenu = false })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false })
        }
    }

    if (showResendConfirm) {
        AlertDialog(
            onDismissRequest = { showResendConfirm = false },
            title = { Text("Resend report?") },
            text = { Text("This will open email/SMS to notify the same authorities again.") },
            confirmButton = {
                TextButton(onClick = {
                    showResendConfirm = false
                    sending = true
                    resendReport(report)
                    sending = false
                    android.app.AlertDialog.Builder(context)
                        .setTitle("Resent")
                        .setMessage("Report ${report.id} sent again.")
                        .setPositiveButton("OK") { _, _ -> }
                        .show()
                }) { Text("Resend") }
            },
            dismissButton = { TextButton(onClick = { showResendConfirm = false }) { Text("Cancel") } }
        )
    }
}

private fun formatDate(epochMs: Long): String {
    val d = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return d.java.util.Date(epochMs).toString()
}
