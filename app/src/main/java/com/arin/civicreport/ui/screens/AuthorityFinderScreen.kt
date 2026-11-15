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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    var query by remember { mutableStateOf("") }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city) {
        loading = true
        error = null
        try {
            val found = LinkedHashMap<String, Authority>()
            val cityKey = draft.city.lowercase(Locale.US)
            draft.incidentTypes.forEach { tag ->
                repo.getAuthoritiesForIncident(tag, draft.state, cityKey)
                    .forEach { candidate -> if (!found.containsKey(candidate.id)) found[candidate.id] = candidate }
            }
            val list = found.values.sortedWith(compareBy({ levelRank(it.level) }, { it.order }))
            authorities = list
            selected = list.map { it.id }.toSet()
            reportViewModel.setAuthorities(list, selected.toList())
        } catch (e: Exception) {
            error = "The contact list could not be loaded. ${e.message ?: ""}".trim()
        }
        loading = false
    }

    fun toggle(id: String) {
        selected = if (selected.contains(id)) selected - id else selected + id
        reportViewModel.setAuthorities(authorities, selected.toList())
    }

    val visible = authorities.filter { authority ->
        query.isBlank() ||
            authority.name.contains(query, ignoreCase = true) ||
            authority.email.contains(query, ignoreCase = true) ||
            authority.phone.contains(query, ignoreCase = true)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Concerned authorities", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                TextButton(onClick = {
                    selected = if (selected.size == authorities.size) emptySet()
                    else authorities.map { it.id }.toSet()
                    reportViewModel.setAuthorities(authorities, selected.toList())
                }) {
                    Text(if (selected.size == authorities.size) "Clear" else "All")
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
            Text(
                draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Matched from: ${draft.address.ifBlank { "location unknown" }}" +
                    if (draft.state.isNotBlank()) " ($draft.state${if (draft.city.isNotBlank()) ", ${draft.city}" else ""})" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search officials") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
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

            Box(modifier = Modifier.weight(1f)) {
                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Finding the right offices...")
                        }
                    }

                    visible.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("No matching office found", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Go back and confirm the location, or add a contact in Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val groups = visible.groupBy { it.level }
                        groups.forEach { (level, group) ->
                            item(key = "header_$level") {
                                Text(
                                    levelLabel(level),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(group, key = { it.id }) { authority ->
                                AuthorityRow(
                                    authority = authority,
                                    checked = selected.contains(authority.id),
                                    onToggle = { toggle(authority.id) }
                                )
                            }
                        }
                        item(key = "footer") { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }

            Button(
                onClick = {
                    reportViewModel.setAuthorities(authorities, selected.toList())
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                enabled = selected.isNotEmpty() && !loading
            ) {
                Text("Continue with ${selected.size} official(s)")
            }
        }
    }
}

@Composable
private fun AuthorityRow(authority: Authority, checked: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(authority.name, fontWeight = FontWeight.Bold)
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
        }
    }
}

private fun levelRank(level: String): Int = when (level) {
    "Local" -> 0
    "State" -> 1
    "Central" -> 2
    else -> 3
}

private fun levelLabel(level: String): String = when (level) {
    "Local" -> "Local offices"
    "State" -> "State offices (including Chief Minister)"
    "Central" -> "Central offices (including Prime Minister's Office)"
    else -> level
}
