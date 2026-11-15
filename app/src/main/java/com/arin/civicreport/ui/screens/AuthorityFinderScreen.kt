package com.arin.civicreport.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    var message by remember { mutableStateOf<String?>(null) }

    fun dial(phone: String) {
        val cleaned = phone.replace(" ", "")
        if (cleaned.isBlank()) return
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
        } catch (e: Exception) {
            message = "No dialer app found for $phone"
        }
    }

    fun mail(email: String) {
        if (email.isBlank()) return
        try {
            val intent = Intent(
                Intent.ACTION_SENDTO,
                Uri.parse("mailto:$email")
            )
            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Complaint: " + draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }
            )
            intent.putExtra(
                Intent.EXTRA_TEXT,
                "Location: ${draft.address}\nGPS: ${draft.latitude}, ${draft.longitude}\n\n${draft.description}"
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            message = "No email app found for $email"
        }
    }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city) {
        loading = true
        message = null
        val state = draft.state.lowercase(Locale.US)
        val city = draft.city.lowercase(Locale.US)
        val found = mutableListOf<Authority>()
        draft.incidentTypes.forEach { tag ->
            repo.getAuthoritiesForIncident(tag, state, city).forEach { candidate ->
                if (found.none { it.id == candidate.id }) found.add(candidate)
            }
        }
        if (found.isEmpty()) {
            repo.seedAuthorities()
            draft.incidentTypes.forEach { tag ->
                repo.getAuthoritiesForIncident(tag, state, city).forEach { candidate ->
                    if (found.none { it.id == candidate.id }) found.add(candidate)
                }
            }
        }
        val sorted = found.sortedWith(compareBy({ levelRank(it.level) }, { it.order }))
        reportViewModel.setAuthorities(sorted, sorted.map { it.id })
        if (sorted.isEmpty()) {
            message = "No matching office was found for this location. Add contacts in Settings and come back."
        }
        loading = false
    }

    val authorities = draft.authorities
    val selected = draft.selectedAuthorityIds

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Concerned officials", fontWeight = FontWeight.Bold) },
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
                "Auto-matched for: " + draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Jurisdiction: ${draft.city.ifBlank { "city unknown" }}, ${draft.state.ifBlank { "state unknown" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (message != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        message ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Matching authorities...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(authorities, key = { it.id }) { authority ->
                        val isSelected = selected.contains(authority.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportViewModel.toggleAuthority(authority.id) }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(authority.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${authority.level} level",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (authority.email.isNotBlank()) {
                                        Text(authority.email, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (authority.phone.isNotBlank()) {
                                        Text(authority.phone, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                IconButton(onClick = { dial(authority.phone) }) {
                                    Icon(Icons.Filled.Call, contentDescription = "Call ${authority.name}")
                                }
                                IconButton(onClick = { mail(authority.email) }) {
                                    Icon(Icons.Filled.Email, contentDescription = "Email ${authority.name}")
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (selected.size == authorities.size) {
                            reportViewModel.setAuthorities(authorities, emptyList())
                        } else {
                            reportViewModel.setAuthorities(authorities, authorities.map { it.id })
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = authorities.isNotEmpty()
                ) {
                    Text(if (selected.size == authorities.size) "Clear all" else "Select all")
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                    enabled = selected.isNotEmpty()
                ) {
                    Text("Continue (${selected.size})")
                }
            }
        }
    }
}

private fun levelRank(level: String): Int = when (level) {
    "Local" -> 0
    "State" -> 1
    else -> 2
}
