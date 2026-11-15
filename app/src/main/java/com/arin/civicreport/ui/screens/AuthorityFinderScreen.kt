package com.arin.civicreport.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    var error by remember { mutableStateOf<String?>(null) }
    val selected = draft.selectedAuthorityIds.toSet()

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city) {
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
            val unique = found
                .distinctBy { it.id }
                .sortedWith(compareBy({ levelRank(it.level) }, { it.order }, { it.name }))
            reportViewModel.setAuthorities(unique, unique.map { it.id })
        } catch (e: Exception) {
            error = e.message ?: "The contact list could not be loaded."
        } finally {
            loading = false
        }
    }

    fun call(phone: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.replace(" ", ""))))
        } catch (e: Exception) {
            error = "No dialer app found."
        }
    }

    fun mail(email: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
        } catch (e: Exception) {
            error = "No email app found."
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Matched authorities", fontWeight = FontWeight.Bold) },
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
                "Issues: ${draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) }}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                draft.address.ifBlank { "Location pending" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Matched from the local office up to the Chief Minister's office and the PMO using the " +
                    "detected district and state.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        error ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Searching for the right offices...")
                    }
                    draft.authorities.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No office matched this combination", fontWeight = FontWeight.Bold)
                        Text(
                            "You can still continue and add a contact later in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(draft.authorities) { authority ->
                            AuthorityRow(
                                authority = authority,
                                checked = selected.contains(authority.id),
                                onToggle = { reportViewModel.toggleAuthority(authority.id) },
                                onCall = { call(authority.phone) },
                                onMail = { mail(authority.email) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                enabled = selected.isNotEmpty()
            ) {
                Text("Continue with ${selected.size} office(s)")
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
            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (checked) "Included" else "Excluded",
                    tint = if (checked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(authority.name, fontWeight = FontWeight.Bold)
                Text(
                    "${authority.level} level",
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
