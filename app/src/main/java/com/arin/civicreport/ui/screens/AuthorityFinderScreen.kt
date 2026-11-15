package com.arin.civicreport.ui.screens

import android.content.Intent
import android.net.Uri
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
    var authorities by remember { mutableStateOf<List<Authority>>(emptyList()) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city) {
        loading = true
        val found = LinkedHashMap<String, Authority>()
        draft.incidentTypes.forEach { tag ->
            repo.getAuthoritiesForIncident(
                tag,
                draft.state,
                draft.city.lowercase(Locale.getDefault())
            ).forEach { found[it.id] = it }
        }
        val sorted = found.values.sortedWith(
            compareBy<Authority> { levelRank(it.level) }.thenBy { it.order }
        )
        authorities = sorted
        reportViewModel.setAuthorities(sorted, sorted.map { it.id })
        loading = false
    }

    fun openEmail(address: String) {
        if (address.isBlank()) return
        try {
            context.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address))
            )
        } catch (e: Exception) {
            // ignore, phone simply has no mail app
        }
    }

    fun callNumber(number: String) {
        val clean = number.replace(Regex("[^+0-9]"), "")
        if (clean.isBlank()) return
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + clean)))
        } catch (e: Exception) {
            // ignore, phone cannot dial
        }
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
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Matched automatically for: " +
                    draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Place: " +
                    listOf(draft.city, draft.district, draft.state)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                        .ifBlank { "not detected - local and central offices still listed" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Searching the right offices...")
                    }
                }
            } else if (authorities.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No office matched this issue and place. Go back and add a note, or add a contact in Settings.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(authorities) { authority ->
                        val checked = draft.selectedAuthorityIds.contains(authority.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportViewModel.toggleAuthority(authority.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (checked)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        reportViewModel.toggleAuthority(authority.id)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        authority.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        authority.level,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
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
                                    IconButton(onClick = { openEmail(authority.email) }) {
                                        Icon(
                                            Icons.Filled.Email,
                                            contentDescription = "Email this office"
                                        )
                                    }
                                }
                                if (authority.phone.isNotBlank()) {
                                    IconButton(onClick = { callNumber(authority.phone) }) {
                                        Icon(
                                            Icons.Filled.Call,
                                            contentDescription = "Call this office"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = draft.selectedAuthorityIds.isNotEmpty()
                ) {
                    Text(
                        "Continue to review (" +
                            draft.selectedAuthorityIds.size + " selected)"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
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
