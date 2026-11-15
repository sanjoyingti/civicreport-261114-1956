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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Surface
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
import java.util.Locale

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

    var authorities by remember { mutableStateOf<List<Authority>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city, reloadKey) {
        loading = true
        error = null
        try {
            val found = mutableListOf<Authority>()
            draft.incidentTypes.forEach { tag ->
                found.addAll(
                    repo.getAuthoritiesForIncident(
                        typeTag = tag,
                        state = draft.state,
                        city = draft.city.lowercase(Locale.US)
                    )
                )
            }
            val distinct = found.distinctBy { it.id }.sortedBy { it.order }
            authorities = distinct
            reportViewModel.setAuthorities(distinct, distinct.map { it.id })
        } catch (e: Exception) {
            error = e.message ?: "The contact list could not be loaded."
        }
        loading = false
    }

    val selected = draft.selectedAuthorityIds

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relevant officials", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selected.isNotEmpty()
                    ) {
                        Text("Continue with ${selected.size} selected")
                    }
                }
            }
        }
    ) { padding ->
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
                "Matched for ${draft.city.ifBlank { "your area" }}${if (draft.state.isNotBlank()) ", ${draft.state}" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            error ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = { reloadKey++ }) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Try again")
                        }
                    }
                }

                authorities.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No officials matched this area", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "You can add a local contact in Settings and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { reloadKey++ }) { Text("Reload") }
                    }
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(authorities) { authority ->
                            AuthorityRow(
                                authority = authority,
                                checked = selected.contains(authority.id),
                                onToggle = { reportViewModel.toggleAuthority(authority.id) },
                                onCall = {
                                    val number = authority.phone.filter { it.isDigit() || it == '+' }
                                    if (number.isNotBlank()) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                            )
                                        }
                                    }
                                },
                                onEmail = {
                                    if (authority.email.isNotBlank()) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_SENDTO,
                                                    Uri.parse("mailto:" + authority.email)
                                                )
                                            )
                                        }
                                    }
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
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    authority.level,
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
            IconButton(onClick = onEmail, enabled = authority.email.isNotBlank()) {
                Icon(Icons.Filled.Email, contentDescription = "Email this office")
            }
            IconButton(onClick = onCall, enabled = authority.phone.isNotBlank()) {
                Icon(Icons.Filled.Call, contentDescription = "Call this office")
            }
        }
    }
}
