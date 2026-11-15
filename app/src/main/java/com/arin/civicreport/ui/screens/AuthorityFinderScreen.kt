package com.arin.civicreport.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
    val context = LocalContext.current
    val draft by reportViewModel.draft.collectAsState()

    var loading by remember { mutableStateOf(true) }
    var contacts by remember { mutableStateOf<List<Authority>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun dial(phone: String) {
        if (phone.isBlank()) return
        try {
            context.startActivity(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.replace(" ", "")))
            )
        } catch (_: Exception) {
        }
    }

    fun mail(email: String) {
        if (!email.contains("@")) return
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city) {
        loading = true
        val found = LinkedHashMap<String, Authority>()
        draft.incidentTypes.forEach { tag ->
            repo.getAuthoritiesForIncident(tag, draft.state, draft.city.lowercase(Locale.US))
                .forEach { found[it.id] = it }
        }
        val sorted = found.values.sortedWith(compareBy({ levelRank(it.level) }, { it.order }))
        contacts = sorted
        selected = sorted.map { it.id }.toSet()
        loading = false
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
                "Based on " +
                    draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) } +
                    " in " + draft.state.ifBlank { "your area" } +
                    (if (draft.city.isNotBlank()) " (${draft.city})" else "") +
                    ", the app matched the offices below. Local office, state head office, the Chief Minister's office and the PMO are all included where they apply.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Matching offices for this location...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (contacts.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("No saved contact matched this type and place.", fontWeight = FontWeight.Bold)
                    Text(
                        "Go back and adjust the pin, or add your own authority email/phone from Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contacts) { authority ->
                        val isSelected = selected.contains(authority.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (isSelected) selected - authority.id
                                    else selected + authority.id
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(authority.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${authority.level} office",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (authority.email.isNotBlank()) {
                                        Text(authority.email, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (authority.phone.isNotBlank()) {
                                        Text(authority.phone, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                IconButton(
                                    onClick = { dial(authority.phone) },
                                    enabled = authority.phone.isNotBlank()
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = "Call office")
                                }
                                IconButton(
                                    onClick = { mail(authority.email) },
                                    enabled = authority.email.contains("@")
                                ) {
                                    Icon(Icons.Filled.Email, contentDescription = "Email office")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "${selected.size} of ${contacts.size} selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        reportViewModel.setAuthorities(contacts, selected.toList())
                        onContinue()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selected.isNotEmpty()
                ) {
                    Text("Continue to review")
                }
            }
        }
    }
}
