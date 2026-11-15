package com.arin.civicreport.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.Sms
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

private val LEVEL_ORDER = listOf("Local", "State", "Central")

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
    var authorities by remember { mutableStateOf<List<Authority>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city) {
        loading = true
        error = null
        try {
            val found = mutableListOf<Authority>()
            draft.incidentTypes.forEach { tag ->
                found += repo.getAuthoritiesForIncident(
                    tag,
                    draft.state,
                    draft.city.lowercase(Locale.US)
                )
            }
            val distinct = found.distinctBy { it.id }
                .sortedWith(compareBy({ LEVEL_ORDER.indexOf(it.level).let { i -> if (i < 0) 99 else i } }, { it.order }))
            authorities = distinct
            selected = distinct.filter { it.level == "Local" }.map { it.id }.toSet()
            if (distinct.isEmpty()) {
                error = "No matching contacts found for this area. You can add offices in Settings."
            }
        } catch (e: Exception) {
            error = e.message ?: "The contact list could not be loaded."
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Concerned officials", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        reportViewModel.setAuthorities(authorities, selected.toList())
                        onContinue()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selected.isNotEmpty()
                ) {
                    Text("Continue with ${selected.size} office(s)")
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
                "Matched automatically for: " +
                    draft.incidentTypes.joinToString(", ") { IncidentType.titleFor(it) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                draft.address.ifBlank { "Location not set" } +
                    if (draft.state.isNotBlank()) " • ${draft.state}" else "",
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
                            Text("Finding the right offices...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                authorities.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            error ?: "No matching contacts found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LEVEL_ORDER.forEach { level ->
                            val group = authorities.filter { it.level == level }
                            if (group.isNotEmpty()) {
                                item(key = "header_$level") {
                                    Text(
                                        when (level) {
                                            "Local" -> "Local offices"
                                            "State" -> "State level"
                                            else -> "Central government"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                items(group, key = { it.id }) { authority ->
                                    AuthorityRow(
                                        authority = authority,
                                        checked = selected.contains(authority.id),
                                        onCheckedChange = { isChecked ->
                                            selected = if (isChecked) selected + authority.id
                                            else selected - authority.id
                                        },
                                        onCall = { dial(context, authority.phone) },
                                        onMail = { mail(context, authority.email) },
                                        onSms = { message(context, authority.phone) }
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
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
    onCheckedChange: (Boolean) -> Unit,
    onCall: () -> Unit,
    onMail: () -> Unit,
    onSms: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Column(modifier = Modifier.weight(1f)) {
                Text(authority.name, fontWeight = FontWeight.Bold)
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
            IconButton(onClick = onCall, enabled = authority.phone.isNotBlank()) {
                Icon(Icons.Filled.Call, contentDescription = "Call")
            }
            IconButton(onClick = onMail, enabled = authority.email.isNotBlank()) {
                Icon(Icons.Filled.Email, contentDescription = "Email")
            }
            IconButton(onClick = onSms, enabled = authority.phone.isNotBlank()) {
                Icon(Icons.Filled.Sms, contentDescription = "Message")
            }
        }
    }
}

private fun dial(context: Context, phone: String) {
    if (phone.isBlank()) return
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone)))
    runCatching { context.startActivity(intent) }
}

private fun mail(context: Context, email: String) {
    if (email.isBlank()) return
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(email)))
    runCatching { context.startActivity(intent) }
}

private fun message(context: Context, phone: String) {
    if (phone.isBlank()) return
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone))
    runCatching { context.startActivity(intent) }
}
