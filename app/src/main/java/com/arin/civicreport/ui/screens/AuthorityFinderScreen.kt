package com.arin.civicreport.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.ui.screens.report.ReportFlowViewModel
import com.arin.civicreport.ui.util.ReportSender
import java.util.Locale

private fun levelRank(level: String): Int = when (level.lowercase(Locale.US)) {
    "local" -> 0
    "state" -> 1
    "central" -> 2
    else -> 3
}

private fun levelTitle(level: String): String = when (level.lowercase(Locale.US)) {
    "local" -> "Local offices"
    "state" -> "State offices"
    "central" -> "Central offices"
    else -> "Other offices"
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
    var matches by remember { mutableStateOf<List<Authority>>(emptyList()) }
    var problem by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableStateOf(0) }

    LaunchedEffect(attempt, draft.incidentTypes, draft.state, draft.city) {
        loading = true
        problem = null
        val types = draft.incidentTypes
        if (types.isEmpty()) {
            matches = emptyList()
            loading = false
            problem = "No incident type was chosen, so no office could be matched."
            return@LaunchedEffect
        }
        val city = draft.city.lowercase(Locale.US)
        val direct = mutableListOf<Authority>()
        types.forEach { tag ->
            direct += repo.getAuthoritiesForIncident(tag, draft.state, city)
        }
        var distinct = direct.distinctBy { it.id }
        if (distinct.isEmpty()) {
            val relaxed = mutableListOf<Authority>()
            types.forEach { tag ->
                relaxed += repo.getAuthoritiesForIncident(tag, "", "")
            }
            distinct = relaxed.distinctBy { it.id }
        }
        val sorted = distinct.sortedWith(
            compareBy({ levelRank(it.level) }, { it.order }, { it.name })
        )
        matches = sorted
        reportViewModel.setAuthorities(sorted, sorted.map { it.id })
        loading = false
        if (sorted.isEmpty()) {
            problem = "No matching office was found in the built-in contact list. " +
                "You can add one in Settings and then retry."
        }
    }

    val selected = draft.selectedAuthorityIds
    val subject = "Civic complaint"
    val body = "Please see the attached civic complaint filed from DuarBagori Reports."

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Concerned authorities", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { attempt++ }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Search again")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onContinue,
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selected.isEmpty()) "Select at least one office"
                        else "Continue with ${selected.size} office(s)"
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (loading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Matching offices to this location...")
                }
                return@Box
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val jurisdiction = listOf(draft.city, draft.district, draft.state)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    Column {
                        Text(
                            draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Detected area: ${jurisdiction.ifBlank { draft.address.ifBlank { "not available" } }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "These offices were matched automatically. Untick any office you do not want to notify.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (problem != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                problem ?: "",
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    item {
                        TextButton(onClick = { attempt++ }) { Text("Try the search again") }
                    }
                }

                if (matches.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    reportViewModel.setAuthorities(matches, matches.map { it.id })
                                }
                            ) { Text("Select all") }
                            TextButton(
                                onClick = { reportViewModel.setAuthorities(matches, emptyList()) }
                            ) { Text("Clear all") }
                        }
                    }

                    var lastLevel = ""
                    matches.forEach { authority ->
                        if (authority.level != lastLevel) {
                            lastLevel = authority.level
                            item {
                                Text(
                                    levelTitle(authority.level),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        item {
                            AuthorityRow(
                                authority = authority,
                                checked = selected.contains(authority.id),
                                onToggle = { reportViewModel.toggleAuthority(authority.id) },
                                onCall = { ReportSender.call(context, authority.phone) },
                                onEmail = {
                                    ReportSender.emailOne(context, authority.email, subject, body)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorityRow(
    authority: Authority,
    checked: Boolean,
    onToggle: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit
) {
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
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
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
            if (authority.email.isNotBlank()) {
                IconButton(onClick = onEmail) {
                    Icon(Icons.Filled.Email, contentDescription = "Email ${authority.name}")
                }
            }
            if (authority.phone.isNotBlank()) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Filled.Call, contentDescription = "Call ${authority.name}")
                }
            }
        }
    }
}
