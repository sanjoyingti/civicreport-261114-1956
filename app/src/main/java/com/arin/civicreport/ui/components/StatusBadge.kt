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
        IncidentStatus.Draft -> {
            label = "Draft"
            container = colours.surfaceVariant
            content = colours.onSurfaceVariant
        }
        IncidentStatus.Pending -> {
            label = "Pending"
            container = colours.tertiaryContainer
            content = colours.onTertiaryContainer
        }
        IncidentStatus.Sent -> {
            label = "Sent"
            container = colours.primaryContainer
            content = colours.onPrimaryContainer
        }
        IncidentStatus.Failed -> {
            label = "Failed"
            container = colours.errorContainer
            content = colours.onErrorContainer
        }
    }
    Text(
        text = label,
        color = content,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}
