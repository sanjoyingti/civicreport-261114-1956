package com.arin.civicreport.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.model.IncidentType

@Composable
fun IncidentSelectorScreen(onContinue: (List<String>) -> Unit) {
    var selected by remember { mutableStateOf(setOf<String>()) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Select incident type(s)", fontWeight = FontWeight.Bold) }
        )
    }) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text("Choose one or more incident types:", style = MaterialTheme.typography.bodyLarge)
            Text("If one incident involves several issues, select them all.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(IncidentType.all().size) { index ->
                    val t = IncidentType.all()[index]
                    val isSelected = selected.contains(t.slug)
                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                selected = if (isSelected) selected - t.slug else selected + t.slug
                            }
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Alignment.CenterVertically
                        ) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (selected.isEmpty()) {
                        AlertDialog(onDismissRequest = { }, title = { Text("Select at least one") }, text = { Text("Pick at least one incident type to continue.") }, confirmButton = { TextButton(onClick = { }) { Text("OK") } })
                    } else {
                        onContinue(selected.toList())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selected.isNotEmpty()
            ) {
                Text("Continue")
            }
        }
    }
}
