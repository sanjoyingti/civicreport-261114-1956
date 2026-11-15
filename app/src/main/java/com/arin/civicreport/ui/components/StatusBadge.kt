package com.arin.civicreport.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arin.civicreport.model.IncidentStatus

@Composable
fun StatusBadge(status: IncidentStatus) {
    val label = when (status) {
        IncidentStatus.Draft -> "Draft"
        IncidentStatus.Pending -> "Pending"
        IncidentStatus.Sent -> "Sent"
        IncidentStatus.Failed -> "Failed"
    }
    val color = when (status) {
        IncidentStatus.Draft -> MaterialTheme.colorScheme.outline
        IncidentStatus.Pending -> Color(0xFF8A6D00)
        IncidentStatus.Sent -> MaterialTheme.colorScheme.primary
        IncidentStatus.Failed -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
