package com.arin.civicreport.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.IncidentStatus
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import com.arin.civicreport.ui.screens.report.ReportFlowViewModel
import com.arin.civicreport.ui.util.NetworkChecker
import com.arin.civicreport.ui.util.Prefs
import com.arin.civicreport.ui.util.ReportSender
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewAndSendScreen(
    repo: Repository,
    prefs: Prefs,
    reportViewModel: ReportFlowViewModel,
    onBack: () -> Unit,
    onSent: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val draft by reportViewModel.draft.collectAsState()
    var method by remember { mutableStateOf(prefs.defaultSendMethod) }
    var sending by remember { mutableStateOf(false) }
    var online by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        online = NetworkChecker.isOnline(context)
    }

    val chosen = draft.authorities.filter { draft.selectedAuthorityIds.contains(it.id) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Review and send", fontWeight = FontWeight.Bold) },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Location: ${draft.address.ifBlank { "unknown" }}")
                    Text(
                        "Lat/Lon: ${"%.6f".format(draft.latitude)} / ${"%.6f".format(draft.longitude)}"
                    )
                    Text("Media: ${draft.mediaUri ?: "none"} (${draft.mediaType})")
                    if (draft.description.isNotBlank()) {
                        Text("Notes: ${draft.description}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Officials to notify (${chosen.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyColumn(modifier = Modifier.height(180.dp)) {
                items(chosen) { authority ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(authority.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "Level: ${authority.level}  ${authority.email}  ${authority.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { reportViewModel.toggleAuthority(authority.id) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Contact method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MethodChip(method == "email", "Email") { method = "email"; prefs.defaultSendMethod = "email" }
                MethodChip(method == "sms", "SMS") { method = "sms"; prefs.defaultSendMethod = "sms" }
                MethodChip(method == "both", "Both") { method = "both"; prefs.defaultSendMethod = "both" }
            }

            if (!online) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        "You look offline. The report is saved anyway and the compose screens open so you can send it later.",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (chosen.isEmpty() || sending) return@Button
                    sending = true
                    scope.launch {
                        val now = System.currentTimeMillis()
                        val report = Report(
                            incidentTypes = draft.incidentTypes,
                            mediaUris = listOfNotNull(draft.mediaUri),
                            mediaType = draft.mediaType,
                            timestamp = now,
                            latitude = draft.latitude,
                            longitude = draft.longitude,
                            address = draft.address,
                            city = draft.city,
                            district = draft.district,
                            state = draft.state,
                            pincode = draft.pincode,
                            description = draft.description,
                            status = IncidentStatus.Sent,
                            sentAuthorities = chosen.map { it.id },
                            lastUpdated = now,
                            sendMethod = method
                        )
                        val id = repo.saveReport(report)
                        ReportSender.send(context, chosen, report, method)
                        sending = false
                        onSent(id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = chosen.isNotEmpty() && !sending
            ) {
                Text(if (sending) "Sending..." else "Save and send to ${chosen.size} official(s)")
            }
        }
    }
}

@Composable
private fun MethodChip(selected: Boolean, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
