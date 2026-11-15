package com.arin.civicreport.ui.screens

import android.content.ActivityNotFoundException
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
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

private fun levelTitle(level: String): String = when (level.lowercase(Locale.US)) {
    "local" -> "Your local office"
    "state" -> "State level"
    else -> "National level"
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
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(draft.incidentTypes, draft.state, draft.city, refresh) {
        loading = true
        error = null
        try {
            val city = draft.city.lowercase(Locale.US).trim()
            val found = linkedMapOf<String, Authority>()
            draft.incidentTypes.forEach { tag ->
                repo.getAuthoritiesForIncident(tag, draft.state.trim(), city).forEach { candidate ->
                    found[candidate.id] = candidate
                }
            }
            reportViewModel.setAuthorities(
                found.values.sortedWith(compareBy({ levelRank(it.level) }, { it.order }))
            )
        } catch (e: Exception) {
            error = e.message ?: "The responsible offices could not be looked up"
        }
        loading = false
    }

    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app on this phone can open that", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Who to tell", fontWeight = FontWeight.Bold) },
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
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = draft.selectedAuthorityIds.isNotEmpty()
                ) {
                    Text("Continue with ${draft.selectedAuthorityIds.size} office(s)")
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
                "Matched for ${draft.address.ifBlank { "the pinned location" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Finding the responsible offices...")
                        }
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error ?: "", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { refresh++ }) { Text("Try again") }
                        }
                    }
                }
                draft.authorities.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No office matched this problem yet")
                            Text(
                                "Add a contact in Settings and it will appear for every report.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { refresh++ }) { Text("Search again") }
                        }
                    }
                }
                else -> {
                    val grouped = draft.authorities.groupBy { levelTitle(it.level) }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        grouped.forEach { (title, list) ->
                            item { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                            items(list) { authority ->
                                AuthorityRow(
                                    authority = authority,
                                    checked = draft.selectedAuthorityIds.contains(authority.id),
                                    onToggle = { reportViewModel.toggleAuthority(authority.id) },
                                    onCall = {
                                        open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + authority.phone)))
                                    },
                                    onMail = {
                                        open(
                                            Intent(
                                                Intent.ACTION_SENDTO,
                                                Uri.parse("mailto:" + authority.email)
                                            )
                                        )
                                    },
                                    onSms = {
                                        open(
                                            Intent(
                                                Intent.ACTION_SENDTO,
                                                Uri.parse("smsto:" + authority.phone)
                                            )
                                        )
                                    }
                                )
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
    onToggle: () -> Unit,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(authority.name, fontWeight = FontWeight.Bold)
                if (authority.phone.isNotBlank()) {
                    Text(authority.phone, style = MaterialTheme.typography.bodySmall)
                }
                if (authority.email.isNotBlank()) {
                    Text(authority.email, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (checked) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (authority.phone.isNotBlank()) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Filled.Call, contentDescription = "Call")
                }
                IconButton(onClick = onSms) {
                    Icon(Icons.Filled.Sms, contentDescription = "Text")
                }
            }
            if (authority.email.isNotBlank()) {
                IconButton(onClick = onMail) {
                    Icon(Icons.Filled.Email, contentDescription = "E-mail")
                }
            }
        }
    }
}
