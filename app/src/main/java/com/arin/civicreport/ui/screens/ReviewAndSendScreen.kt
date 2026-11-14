package com.arin.civicreport.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.arin.civicreport.model.IncidentStatus
import com.arin.civicreport.ui.util.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewAndSendScreen(
    incidentTypes: List<String>,
    mediaUri: String?,
    mediaType: String,
    latitude: Double,
    longitude: Double,
    address: String,
    authorities: List<com.arin.civicreport.model.Authority>,
    sendMethod: String,
    onBack: () -> Unit,
    onSaveAndSend: (List<String>, String) -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(authorities.map { it.id }) }
    var method by remember { mutableStateOf(sendMethod) }
    var sending by remember { mutableStateOf(false) }

    fun buildBody(): String {
        return buildString {
            append("Civic Incident Report\n\n")
            append("Types: ${incidentTypes.joinToString(", ")}\n")
            append("Location: ${address.ifBlank { "unknown" }}\n")
            append("Latitude: ${"%.6f".format(latitude)}\n")
            append("Longitude: ${"%.6f".format(longitude)}\n")
            append("Media: $mediaUri ($mediaType)\n")
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Review & Send", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Incident types: ${incidentTypes.joinToString(", ")}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Location: ${address.ifBlank { "unknown" }}", style = MaterialTheme.typography.bodyMedium)
                    Text("Lat/Lon: ${"%.6f".format(latitude)} / ${"%.6f".format(longitude)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Media: $mediaUri ($mediaType)", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Selected authorities (${selected.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn {
                items(selected.map { id -> authorities.find { a -> a.id == id }!! }) { a ->
                    AuthorityRow(a = a, onDeselect = { selected = selected - a.id })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Contact method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SendMethodChip(method == "email", "Email") { method = "email" }
                SendMethodChip(method == "sms", "SMS") { method = "sms" }
                SendMethodChip(method == "both", "Both") { method = "both" }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    sending = true
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        authorities.filter { selected.contains(it.id) }.forEach { a ->
                            sendToAuthority(context, a, method, buildBody(), mediaUri)
                        }
                        onSaveAndSend(selected.sorted(), method)
                        sending = false
                        android.app.AlertDialog.Builder(context).setTitle("Sent").setMessage("Report sent to ${selected.size} authority(ies).").setPositiveButton("OK") { _, _ -> }.show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending && selected.isNotEmpty()
            ) {
                Text(if (sending) "Sending…" else "Send All")
            }
        }
    }
}

@Composable
private fun AuthorityRow(a: com.arin.civicreport.model.Authority, onDeselect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(a.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Level: ${a.level}  Phone: ${a.phone}  Email: ${a.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDeselect) {
                Icon(Icons.Default.Close, contentDescription = "Deselect", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun SendMethodChip(selected: Boolean, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(label, modifier = Modifier.padding(12.dp), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun sendToAuthority(context: Context, a: com.arin.civicreport.model.Authority, method: String, body: String, mediaUri: String?) {
    val subject = "Civic Incident Report: ${a.name} - ${a.level}"
    val mail = if (method in listOf("email", "both")) {
        Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:${a.email}")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            mediaUri?.let {
                putExtra(Intent.EXTRA_STREAM, Uri.parse(it))
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    } else null
    val sms = if (method in listOf("sms", "both")) {
        Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:${a.phone}")
            putExtra(Intent.EXTRA_TEXT, body)
        }
    } else null

    mail?.let {
        try { context.startActivity(it) } catch (e: Exception) { android.app.AlertDialog.Builder(context).setTitle("No email app").setMessage("Install an email app to send by email.").setPositiveButton("OK") { _, _ -> }.show() }
    }
    sms?.let {
        try { context.startActivity(it) } catch (e: Exception) { android.app.AlertDialog.Builder(context).setTitle("No SMS app").setMessage("Install an SMS app to send by SMS.").setPositiveButton("OK") { _, _ -> }.show() }
    }
}
