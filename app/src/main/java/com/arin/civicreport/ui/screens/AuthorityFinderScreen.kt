package com.arin.civicreport.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.ui.screens.report.ReportFlowViewModel
import java.util.Locale

private fun levelRank(level: String): Int = when (level.lowercase(Locale.US)) {
    "local" -> 0
    "state" -> 1
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
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var authorities by remember { mutableStateOf<List<Authority>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city, reloadKey) {
        loading = true
        error = null
        try {
            val found = mutableListOf<Authority>()
            draft.incidentTypes.forEach { tag ->
                repo.getAuthoritiesForIncident(
                    tag,
                    draft.state,
                    draft.city.lowercase(Locale.US)
                ).forEach { found.add(it) }
            }
            val distinct = found
                .distinctBy { it.id }
                .sortedWith(compareBy({ levelRank(it.level) }, { it.order }, { it.name }))
            authorities = distinct
            selected = distinct.map { it.id }.toSet()
            if (distinct.isEmpty()) {
                error = "No saved contact matches this incident in this area. Add contacts in Settings."
            }
        } catch (e: Exception) {
            error = e.message ?: "The authority list could not be loaded."
            authorities = emptyList()
        }
        loading = false
    }

    fun dial(number: String) {
        if (number.isBlank()) return
        val intent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:" + number.replace(" ", ""))
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            error = "No dialer app found on this device."
        }
    }

    fun mail(address: String) {
        if (address.isBlank()) return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            error = "No email app found on this device."
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Officials to notify", fontWeight = FontWeight.Bold) },
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
            Text(
                draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Matched automatically for ${draft.state.ifBlank { "your state" }}" +
                    draft.city.ifBlank { "" }.let { if (it.isBlank()) "" else ", $it" } +
                    ". Local, then state, then central offices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    if (error != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    error ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                OutlinedButton(
                                    onClick = { reloadKey++ },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) { Text("Try again") }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(authorities, key = { it.id }) { authority ->
                            AuthorityRow(
                                authority = authority,
                                checked = selected.contains(authority.id),
                                onToggle = {
                                    selected = if (selected.contains(authority.id))
                                        selected - authority.id else selected + authority.id
                                },
                                onCall = { dial(authority.phone) },
                                onMail = { mail(authority.email) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${selected.size} of ${authorities.size} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            reportViewModel.setAuthorities(
                                authorities,
                                authorities.filter { selected.contains(it.id) }.map { it.id }
                            )
                            onContinue()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        enabled = authorities.isNotEmpty() && selected.isNotEmpty()
                    ) {
                        Text("Continue to review")
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
    onMail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(authority.name, fontWeight = FontWeight.Bold)
                Text(
                    "Level: ${authority.level}",
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
            if (authority.phone.isNotBlank()) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Filled.Call, contentDescription = "Call")
                }
            }
            if (authority.email.isNotBlank()) {
                IconButton(onClick = onMail) {
                    Icon(Icons.Filled.Email, contentDescription = "Email")
                }
            }
        }
    }
}
