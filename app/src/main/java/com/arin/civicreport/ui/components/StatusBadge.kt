package com.arin.civicreport.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arin.civicreport.model.IncidentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusBadge(status: IncidentStatus) {
    val background = when (status) {
        IncidentStatus.Sent -> MaterialTheme.colorScheme.primary
        IncidentStatus.Pending -> Color(0xFFFFA600)
        IncidentStatus.Failed -> MaterialTheme.colorScheme.error
        IncidentStatus.Draft -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColour = when (status) {
        IncidentStatus.Sent, IncidentStatus.Failed -> Color.White
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = background, shape = MaterialTheme.shapes.small) {
        Text(
            status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColour
        )
    }
}

fun formatDate(epochMs: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epochMs))
