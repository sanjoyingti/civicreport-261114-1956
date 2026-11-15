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
    var notice by remember { mutableStateOf<String?>(null) }

    val authorities = draft.authorities
    val selectedIds = draft.selectedAuthorityIds

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city) {
        loading = true
        error = null
        try {
            val found = mutableListOf<Authority>()
            draft.incidentTypes.forEach { tag ->
                found.addAll(
                    repo.getAuthoritiesForIncident(
                        tag,
                        draft.state,
                        draft.city.lowercase(Locale.US)
                    )
                )
            }
            val sorted = found
                .distinctBy { it.id }
                .sortedWith(compareBy({ levelRank(it.level) }, { it.order }, { it.name }))
            reportViewModel.setAuthorities(sorted, sorted.map { it.id })
            notice = when {
                draft.state.isBlank() -> "State not detected: only nationwide offices are listed."
                sorted.isEmpty() -> "No saved contact matches this place yet. Add one in Settings."
                else -> null
            }
        } catch (e: Exception) {
            error = e.message ?: "Authorities could not be loaded"
        }
        loading = false
    }

    fun call(phone: String) {
        val number = phone.filter { it.isDigit() || it == '+' }
        if (number.isBlank()) {
            notice = "This office has no phone number."
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        } catch (e: Exception) {
            notice = "No dialler app available."
        }
    }

    fun mail(email: String) {
        if (email.isBlank()) {
            notice = "This office has no email address."
            return
        }
        try {
            context.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
                    putExtra(Intent.EXTRA_SUBJECT, "Civic complaint")
                }
            )
        } catch (e: Exception) {
            notice = "No email app installed."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Officials to notify", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedIds.isNotEmpty() && !loading
                    ) {
                        Text("Continue with ${selectedIds.size} selected")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                error != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Could not load officials", fontWeight = FontWeight.Bold)
                        Text(
                            error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                        TextButton(onClick = {
                            val keys = draft.incidentTypes
                            reportViewModel.setAuthorities(emptyList(), emptyList())
                            loading = true
                            error = null
                            reportViewModel.start(keys)
                        }) { Text("Try again") }
                    }
                }

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Matched automatically for: " +
                                draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (draft.state.isNotBlank() || draft.city.isNotBlank()) {
                            Text(
                                "Area: ${draft.city.ifBlank { "unknown" }} ${draft.state}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (notice != null) {
                            Text(
                                notice ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = {
                                reportViewModel.setAuthorities(authorities, authorities.map { it.id })
                            }) { Text("Select all") }
                            TextButton(onClick = {
                                reportViewModel.setAuthorities(authorities, emptyList())
                            }) { Text("Clear") }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(authorities) { authority ->
                            val checked = selectedIds.contains(authority.id)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            reportViewModel.toggleAuthority(authority.id)
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(authority.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${authority.level} level",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            authority.email,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            authority.phone,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(onClick = { mail(authority.email) }) {
                                        Icon(
                                            Icons.Filled.Email,
                                            contentDescription = "Email this office"
                                        )
                                    }
                                    IconButton(onClick = { call(authority.phone) }) {
                                        Icon(
                                            Icons.Filled.Call,
                                            contentDescription = "Call this office"
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Local offices are listed first, then state, then central offices such as the CM and the PMO. Untick any you do not want.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
