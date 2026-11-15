package com.arin.civicreport.ui.screens

import android.content.ActivityNotFoundException
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
    var found by remember { mutableStateOf<List<Authority>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city, reloadKey) {
        loading = true
        val collected = mutableListOf<Authority>()
        draft.incidentTypes.forEach { tag ->
            collected += repo.getAuthoritiesForIncident(
                tag,
                draft.state,
                draft.city.lowercase(Locale.getDefault())
            )
        }
        val unique = collected.distinctBy { it.id }.sortedWith(
            compareBy({ levelOrder(it.level) }, { it.order }, { it.name })
        )
        found = unique
        reportViewModel.setAuthorities(unique, unique.map { it.id })
        loading = false
    }

    fun call(phone: String) {
        if (phone.isBlank()) return
        val intent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })
        )
        try {
            context.startActivity(intent)
        } catch (ignored: ActivityNotFoundException) {
        }
    }

    fun mail(email: String) {
        if (email.isBlank()) return
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        try {
            context.startActivity(intent)
        } catch (ignored: ActivityNotFoundException) {
        }
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
                IconButton(onClick = { reloadKey++ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Search again")
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Matched to " + listOfNotNull(
                    draft.city.takeIf { it.isNotBlank() },
                    draft.district.takeIf { it.isNotBlank() },
                    draft.state.takeIf { it.isNotBlank() }
                ).joinToString(", ").ifBlank { "the detected area" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Matching offices to this spot...")
                        }
                    }
                }

                found.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No office matched", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "The state or town may not be recognised. You can still continue and add contacts in Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }

                else -> {
                    val selected = draft.selectedAuthorityIds
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(found) { authority ->
                            val isSelected = selected.contains(authority.id)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reportViewModel.toggleAuthority(authority.id) },
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
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            reportViewModel.toggleAuthority(authority.id)
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(authority.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            authority.level + " level",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    if (authority.email.isNotBlank()) {
                                        IconButton(onClick = { mail(authority.email) }) {
                                            Icon(Icons.Filled.Email, contentDescription = "Email")
                                        }
                                    }
                                    if (authority.phone.isNotBlank()) {
                                        IconButton(onClick = { call(authority.phone) }) {
                                            Icon(Icons.Filled.Call, contentDescription = "Call")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${draft.selectedAuthorityIds.size} selected. Chief Minister offices and the PMO are included for every complaint.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = draft.selectedAuthorityIds.isNotEmpty()
            ) {
                Text("Review and send")
            }
        }
    }
}

private fun levelOrder(level: String): Int = when (level) {
    "Local" -> 0
    "State" -> 1
    else -> 2
}
