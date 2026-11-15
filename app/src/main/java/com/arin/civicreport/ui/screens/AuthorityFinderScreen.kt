package com.arin.civicreport.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentStatus
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import com.arin.civicreport.ui.screens.report.ReportFlowViewModel
import com.arin.civicreport.ui.util.ReportSender
import java.util.Locale

private fun levelRank(level: String): Int = when (level) {
    "Local" -> 0
    "District" -> 1
    "State" -> 2
    else -> 3
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorityFinderScreen(
    repo: Repository,
    reportViewModel: ReportFlowViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val draft by reportViewModel.draft.collectAsState()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var authorities by remember { mutableStateOf<List<Authority>>(emptyList()) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city) {
        loading = true
        error = null
        try {
            val city = draft.city.lowercase(Locale.getDefault())
            val found = LinkedHashMap<String, Authority>()
            draft.incidentTypes.forEach { tag ->
                repo.getAuthoritiesForIncident(tag, draft.state, city).forEach { found[it.id] = it }
            }
            // The CM office and the PMO must always be reachable, as asked.
            repo.getAuthoritiesForIncident("all", draft.state, city).forEach { found[it.id] = it }
            val sorted = found.values.sortedWith(
                compareBy({ levelRank(it.level) }, { it.order }, { it.name })
            )
            authorities = sorted
            val stillValid = draft.selectedAuthorityIds.filter { id -> sorted.any { it.id == id } }
            val selected = if (stillValid.isEmpty()) sorted.map { it.id } else stillValid
            reportViewModel.setAuthorities(sorted, selected)
            if (sorted.isEmpty()) {
                error = "No office matched this spot. You can still continue and choose later."
            }
        } catch (e: Exception) {
            error = "The office list could not be loaded: ${e.message}"
        } finally {
            loading = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Concerned offices", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Matched automatically for " +
                    (listOf(draft.address, draft.city, draft.state).filter { it.isNotBlank() }
                        .joinToString(", ").ifBlank { "this location" }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Finding the responsible offices...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                if (error != null) {
                    Text(
                        error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                val previewReport = Report(
                    incidentTypes = draft.incidentTypes,
                    mediaUris = listOfNotNull(draft.mediaUri),
                    mediaType = draft.mediaType,
                    timestamp = System.currentTimeMillis(),
                    latitude = draft.latitude,
                    longitude = draft.longitude,
                    address = draft.address,
                    city = draft.city,
                    district = draft.district,
                    state = draft.state,
                    pincode = draft.pincode,
                    description = draft.description,
                    status = IncidentStatus.Pending,
                    sentAuthorities = emptyList(),
                    lastUpdated = System.currentTimeMillis(),
                    sendMethod = draft.sendMethod
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(authorities) { authority ->
                        val selected = draft.selectedAuthorityIds.contains(authority.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportViewModel.toggleAuthority(authority.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = {
                                        reportViewModel.toggleAuthority(authority.id)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(authority.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        authority.level,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (authority.email.isNotBlank()) {
                                        Text(
                                            authority.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (authority.phone.isNotBlank()) {
                                        Text(
                                            authority.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { ReportSender.call(context, authority.phone) }
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = "Call")
                                }
                                IconButton(
                                    onClick = {
                                        ReportSender.emailTo(context, authority, previewReport)
                                    }
                                ) {
                                    Icon(Icons.Filled.Email, contentDescription = "Email")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val all = authorities.map { it.id }
                            reportViewModel.setAuthorities(authorities, all)
                        }
                    ) { Text("Select all") }
                    OutlinedButton(
                        onClick = { reportViewModel.setAuthorities(authorities, emptyList()) }
                    ) { Text("Clear") }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onContinue,
                        enabled = draft.selectedAuthorityIds.isNotEmpty()
                    ) {
                        Text("Continue (${draft.selectedAuthorityIds.size})")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
