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

/** Small coloured pill showing the state of a report. */
@Composable
fun StatusBadge(status: IncidentStatus) {
    val background = when (status) {
        IncidentStatus.Sent -> MaterialTheme.colorScheme.primary
        IncidentStatus.Pending -> Color(0xFFFFA600)
        IncidentStatus.Failed -> MaterialTheme.colorScheme.error
        IncidentStatus.Draft -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColour = when (status) {
        IncidentStatus.Sent -> MaterialTheme.colorScheme.onPrimary
        IncidentStatus.Pending -> Color(0xFF2B1B00)
        IncidentStatus.Failed -> MaterialTheme.colorScheme.onError
        IncidentStatus.Draft -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = background,
        contentColor = textColour,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
