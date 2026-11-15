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

/** Small rounded label showing a report's status. */
@Composable
fun StatusBadge(status: IncidentStatus) {
    val (label, background, foreground) = when (status) {
        IncidentStatus.Sent -> Triple(
            "Sent",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        IncidentStatus.Pending -> Triple(
            "Pending",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        IncidentStatus.Failed -> Triple(
            "Failed",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        IncidentStatus.Draft -> Triple(
            "Draft",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Text(
        text = label,
        color = foreground,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/** Simple badge for plain text status names coming from older saved rows. */
@Composable
fun StatusBadge(statusName: String) {
    val parsed = runCatching { IncidentStatus.valueOf(statusName) }.getOrDefault(IncidentStatus.Draft)
    if (statusName == parsed.name) {
        StatusBadge(parsed)
    } else {
        Text(
            text = statusName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Colour helper used by the map legend and the review screen. */
fun statusColor(status: IncidentStatus): Color = when (status) {
    IncidentStatus.Sent -> Color(0xFF206039)
    IncidentStatus.Pending -> Color(0xFF6750A4)
    IncidentStatus.Failed -> Color(0xFFB3261E)
    IncidentStatus.Draft -> Color(0xFF79747E)
}
