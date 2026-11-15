package com.arin.civicreport.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arin.civicreport.model.IncidentStatus

/** Small rounded label showing a report's status with theme-aware colours. */
@Composable
fun StatusBadge(status: IncidentStatus) {
    val colours = MaterialTheme.colorScheme
    val label: String
    val container: Color
    val content: Color
    when (status) {
        IncidentStatus.Sent -> {
            label = "Sent"
            container = colours.primaryContainer
            content = colours.onPrimaryContainer
        }
        IncidentStatus.Pending -> {
            label = "Pending"
            container = colours.secondaryContainer
            content = colours.onSecondaryContainer
        }
        IncidentStatus.Failed -> {
            label = "Failed"
            container = colours.errorContainer
            content = colours.onErrorContainer
        }
        IncidentStatus.Draft -> {
            label = "Draft"
            container = colours.surfaceVariant
            content = colours.onSurfaceVariant
        }
    }
    Text(
        text = label,
        modifier = Modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = content,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}
