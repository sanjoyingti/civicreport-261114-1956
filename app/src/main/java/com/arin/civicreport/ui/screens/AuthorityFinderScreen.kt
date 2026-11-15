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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.ui.screens.report.ReportFlowViewModel
import java.util.Locale

private fun levelRank(level: String): Int = when (level) {
    "Local" -> 0
    "State" -> 1
    else -> 2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorityFinderScreen(
    repo: Repository,
    reportViewModel: ReportFlowViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val draft by reportViewModel.draft.collectAsState()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var showEmptyDialog by remember { mutableStateOf(false) }
    var showAllLevels by remember { mutableStateOf(true) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city, draft.district, reloadKey) {
        loading = true
        error = null
        val found = LinkedHashMap<String, Authority>()
        try {
            val city = draft.city.lowercase(Locale.getDefault()).trim()
            draft.incidentTypes.forEach { tag ->
                repo.getAuthoritiesForIncident(tag, draft.state.trim(), city).forEach { hit ->
                    found[hit.id] = hit
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "The authority lookup failed."
        }
        val sorted = found.values.sortedWith(compareBy({ levelRank(it.level) }, { it.order }))
        reportViewModel.setAuthorities(sorted, sorted.map { it.id })
        loading = false
    }

    val authorities = draft.authorities
    val selectedIds = draft.selectedAuthorityIds
    val shown = if (showAllLevels) authorities else authorities.filter { it.level != "Central" }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Concerned officials", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { reloadKey++ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Search again")
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            val kindText = draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
            Text(
                "Matched offices for: $kindText",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (draft.state.isBlank())
                    "State not detected yet - showing all-India offices. You can still tick the ones you want."
                else
                    "Jurisdiction: ${draft.district.ifBlank { draft.city.ifBlank { draft.state } }}, ${draft.state}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = { reloadKey++ }) { Text("Try again") }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Searching the right departments...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (authorities.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("No office matched this incident.", fontWeight = FontWeight.Bold)
                        Text(
                            "Move the pin to a spot with a known state, or add a contact for your area in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onBack) { Text("Go back and fix the location") }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAllLevels = !showAllLevels }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = showAllLevels, onCheckedChange = { showAllLevels = it })
                    Text(
                        "Include Chief Minister office and PMO",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shown.size) { index ->
                        val authority = shown[index]
                        val checked = selectedIds.contains(authority.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportViewModel.toggleAuthority(authority.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (checked)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { reportViewModel.toggleAuthority(authority.id) }
                                )
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Text(authority.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        authority.level,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (authority.email.isNotBlank()) {
                                        Text(
                                            authority.email,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    if (authority.phone.isNotBlank()) {
                                        Text(
                                            authority.phone,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(0.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${selectedIds.size} office(s) selected",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = {
                    if (selectedIds.isEmpty()) showEmptyDialog = true else onContinue()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                Text("Continue to review")
            }
        }
    }

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("Nobody selected") },
            text = { Text("Tick at least one office so the complaint has a recipient.") },
            confirmButton = {
                TextButton(onClick = { showEmptyDialog = false }) { Text("OK") }
            }
        )
    }
}
