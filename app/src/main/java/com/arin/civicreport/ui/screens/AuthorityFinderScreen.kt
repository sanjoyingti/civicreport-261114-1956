=== ABOUT: New screen that looks up the right officials for the chosen problem and place, and lets you tick who to notify ===
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    var authorities by remember { mutableStateOf<List<Authority>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city, reloadKey) {
        loading = true
        error = null
        try {
            val found = linkedMapOf<String, Authority>()
            draft.incidentTypes.forEach { tag ->
                repo.getAuthoritiesForIncident(tag, draft.state, draft.city.lowercase())
                    .forEach { found[it.id] = it }
            }
            val list = found.values.sortedWith(compareBy({ levelRank(it.level) }, { it.order }, { it.name }))
            authorities = list
            val preset = list.map { it.id }.toSet()
            selected = preset
            reportViewModel.setAuthorities(list, preset.toList())
            if (list.isEmpty()) error = "No matching office in the saved contact list for this place yet."
        } catch (e: Exception) {
            error = e.message ?: "The contact list could not be read."
        }
        loading = false
    }

    fun updateSelection(newSet: Set<String>) {
        selected = newSet
        reportViewModel.setAuthorities(authorities, newSet.toList())
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Concerned authorities", fontWeight = FontWeight.Bold) },
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
        ) {
            Text(
                "Detected area: ${draft.address.ifBlank { "unknown" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Types: ${draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        error ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                authorities.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nothing to notify", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { reloadKey++ }) { Text("Try again") }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { updateSelection(authorities.map { it.id }.toSet()) }) {
                            Text("Select all")
                        }
                        TextButton(onClick = { updateSelection(emptySet()) }) { Text("Clear") }
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(authorities, key = { it.id }) { authority ->
                            val checked = selected.contains(authority.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        updateSelection(
                                            if (checked) selected - authority.id
                                            else selected + authority.id
                                        )
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (checked)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = null)
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(authority.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${authority.level} • ${authority.email} • ${authority.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { reloadKey++ },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Search again") }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            reportViewModel.setAuthorities(authorities, selected.toList())
                            onContinue()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selected.isNotEmpty()
                    ) { Text("Continue with ${selected.size} selected") }
                }
            }
        }
    }
}

private fun levelRank(level: String): Int = when (level) {
    "Local" -> 0
    "State" -> 1
    "Central" -> 2
    else -> 3
}
