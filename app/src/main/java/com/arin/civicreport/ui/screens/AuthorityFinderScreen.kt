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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

private fun levelRank(level: String): Int = when (level.lowercase(Locale.US)) {
    "local" -> 0
    "state" -> 1
    "central" -> 2
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
    val draft by reportViewModel.draft.collectAsState()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var authorities by remember { mutableStateOf<List<Authority>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var warning by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city, reloadKey) {
        loading = true
        error = null
        warning = null
        try {
            val city = draft.city.lowercase(Locale.US)
            val found = mutableListOf<Authority>()
            draft.incidentTypes.forEach { tag ->
                found += repo.getAuthoritiesForIncident(tag, draft.state, city)
            }
            val ordered = found
                .distinctBy { it.id }
                .sortedWith(compareBy({ levelRank(it.level) }, { it.order }, { it.name }))
            authorities = ordered
            selected = ordered.map { it.id }.toSet()
        } catch (e: Exception) {
            error = e.message ?: "The office list could not be loaded."
        }
        loading = false
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Matching offices", fontWeight = FontWeight.Bold) },
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
                "Based on the problem type and the detected place, these are the offices that should be told.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Problems: " + draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Place: " + draft.address.ifBlank { "location on map" },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error ?: "", color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { reloadKey++ }) { Text("Try again") }
                        }
                    }
                }

                authorities.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No matching office found for this place.")
                            Text(
                                "You can still continue and add contacts later in Settings.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { reloadKey++ }) { Text("Search again") }
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${selected.size} of ${authorities.size} selected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = {
                            selected = if (selected.size == authorities.size) emptySet()
                            else authorities.map { it.id }.toSet()
                        }) {
                            Text(if (selected.size == authorities.size) "Clear all" else "Select all")
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(authorities) { authority ->
                            val checked = selected.contains(authority.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            selected = if (checked) selected - authority.id
                                            else selected + authority.id
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(authority.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Level: ${authority.level}",
                                            style = MaterialTheme.typography.labelSmall
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
                        }
                    }
                    if (warning != null) {
                        Text(warning ?: "", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (selected.isEmpty()) {
                                warning = "Tick at least one office to continue."
                            } else {
                                warning = null
                                reportViewModel.setAuthorities(authorities, selected.toList())
                                onContinue()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue to review")
                    }
                }
            }
        }
    }
}
