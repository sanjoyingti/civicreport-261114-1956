package com.arin.civicreport.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city) {
        loading = true
        error = null
        val found = LinkedHashMap<String, Authority>()
        var failure: String? = null
        draft.incidentTypes.forEach { tag ->
            try {
                repo.getAuthoritiesForIncident(
                    typeTag = tag,
                    state = draft.state,
                    city = draft.city.lowercase(Locale.US)
                ).forEach { found[it.id] = it }
            } catch (e: Exception) {
                failure = e.message ?: "Lookup failed"
            }
        }
        error = failure
        reportViewModel.setAuthorities(found.values.sortedBy { it.order })
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
                "Matched automatically for " +
                    draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) } +
                    (if (draft.state.isNotBlank()) " in ${draft.state}" else ""),
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

                draft.authorities.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No matching office found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Add a contact in Settings, or continue and send to the national offices below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onContinue) { Text("Continue anyway") }
                        }
                    }
                }

                else -> {
                    if (error != null) {
                        Card(
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${draft.selectedAuthorityIds.size} of ${draft.authorities.size} selected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row {
                            TextButton(onClick = { reportViewModel.selectAll(true) }) {
                                Text("All")
                            }
                            TextButton(onClick = { reportViewModel.selectAll(false) }) {
                                Text("None")
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(draft.authorities) { authority ->
                            AuthorityRow(
                                authority = authority,
                                checked = draft.selectedAuthorityIds.contains(authority.id),
                                onToggle = { reportViewModel.toggleAuthority(authority.id) },
                                onCall = { dial(context, authority) },
                                onMail = { mail(context, authority) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = draft.selectedAuthorityIds.isNotEmpty()
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
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(authority.name, fontWeight = FontWeight.Bold)
                Text(
                    authority.level,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    authority.email.ifBlank { "no email" },
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    authority.phone.ifBlank { "no phone" },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onCall, enabled = authority.phone.isNotBlank()) {
                Icon(Icons.Filled.Call, contentDescription = "Call")
            }
            IconButton(onClick = onMail, enabled = authority.email.isNotBlank()) {
                Icon(Icons.Filled.Email, contentDescription = "Email")
            }
        }
    }
}

private fun dial(context: android.content.Context, authority: Authority) {
    try {
        val uri = Uri.parse("tel:" + authority.phone.replace(" ", ""))
        context.startActivity(Intent(Intent.ACTION_DIAL, uri))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open the dialler", Toast.LENGTH_SHORT).show()
    }
}

private fun mail(context: android.content.Context, authority: Authority) {
    try {
        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse("mailto:" + authority.email)
        )
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open an email app", Toast.LENGTH_SHORT).show()
    }
}
