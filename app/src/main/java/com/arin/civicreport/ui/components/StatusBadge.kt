package com.arin.civicreport.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arin.civicreport.model.IncidentStatus

/** Coloured pill describing the delivery state of a report. */
@Composable
fun StatusBadge(status: IncidentStatus, modifier: Modifier = Modifier) {
    val background: Color = when (status) {
        IncidentStatus.Sent -> MaterialTheme.colorScheme.primary
        IncidentStatus.Pending -> MaterialTheme.colorScheme.tertiary
        IncidentStatus.Failed -> MaterialTheme.colorScheme.error
        IncidentStatus.Draft -> MaterialTheme.colorScheme.outline
    }
    val textColor: Color = when (status) {
        IncidentStatus.Sent -> MaterialTheme.colorScheme.onPrimary
        IncidentStatus.Pending -> MaterialTheme.colorScheme.onTertiary
        IncidentStatus.Failed -> Color.White
        IncidentStatus.Draft -> MaterialTheme.colorScheme.surface
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = background
    ) {
        Text(
            text = status.name,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
