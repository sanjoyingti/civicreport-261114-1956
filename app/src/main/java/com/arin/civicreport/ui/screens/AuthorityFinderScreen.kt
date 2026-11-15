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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.Authority
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
    var error by remember { mutableStateOf<String?>(null) }
    var matches by remember { mutableStateOf<List<Authority>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city) {
        loading = true
        error = null
        try {
            val found = linkedMapOf<String, Authority>()
            val city = draft.city.lowercase(Locale.US)
            draft.incidentTypes.forEach { tag ->
                repo.getAuthoritiesForIncident(tag, draft.state, city).forEach { found[it.id] = it }
            }
            draft.incidentTypes.forEach { tag ->
                repo.getAuthoritiesForIncident(tag, draft.state, "").forEach { found[it.id] = it }
            }
            matches = found.values.sortedWith(compareBy({ levelRank(it.level) }, { it.order }))
            selected = found.keys
            if (found.isEmpty()) {
                error = "No officials matched automatically. You can add contacts in Settings."
            }
        } catch (e: Exception) {
            error = e.message ?: "Authority lookup failed"
        }
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Matched on ${draft.incidentTypes.joinToString(", ").replace('_', ' ')}" +
                    (if (draft.state.isNotBlank()) " in ${draft.city.ifBlank { "" }} ${draft.state}" else ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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

            var lastLevel = ""
            matches.forEach { authority ->
                if (authority.level != lastLevel) {
                    lastLevel = authority.level
                    Text(
                        when (authority.level) {
                            "Local" -> "Local offices"
                            "State" -> "State level"
                            else -> "Central level"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                val isSelected = selected.contains(authority.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            selected = if (isSelected) selected - authority.id
                            else selected + authority.id
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = {
                            selected = if (isSelected) selected - authority.id
                            else selected + authority.id
                        })
                        Column(modifier = Modifier.weight(1f)) {
                            Text(authority.name, fontWeight = FontWeight.Bold)
                            if (authority.email.isNotBlank()) {
                                Text(authority.email, style = MaterialTheme.typography.bodySmall)
                            }
                            if (authority.phone.isNotBlank()) {
                                Text(authority.phone, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (authority.phone.isNotBlank()) {
                            IconButton(onClick = {
                                val intent = Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:" + authority.phone.replace(" ", ""))
                                )
                                runCatching { context.startActivity(intent) }
                            }) { Icon(Icons.Filled.Phone, contentDescription = "Call") }
                        }
                        if (authority.email.isNotBlank()) {
                            IconButton(onClick = {
                                val intent = Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.parse("mailto:" + authority.email)
                                )
                                runCatching { context.startActivity(intent) }
                            }) { Icon(Icons.Filled.Email, contentDescription = "Email") }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    reportViewModel.setAuthorities(matches, selected.toList())
                    onContinue()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading && selected.isNotEmpty()
            ) {
                Text("Continue with ${selected.size} official(s)")
            }
            OutlinedButton(
                onClick = { selected = matches.map { it.id }.toSet() },
                modifier = Modifier.fillMaxWidth(),
                enabled = matches.isNotEmpty()
            ) { Text("Select everyone on the list") }
            TextButton(onClick = { selected = emptySet() }, enabled = matches.isNotEmpty()) {
                Text("Clear selection")
            }
        }
    }
}

private fun levelRank(level: String): Int = when (level) {
    "Local" -> 0
    "State" -> 1
    else -> 2
}
