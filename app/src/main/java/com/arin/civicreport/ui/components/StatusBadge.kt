package com.arin.civicreport.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arin.civicreport.model.IncidentStatus

@Composable
fun StatusBadge(status: IncidentStatus) {
    val label: String
    val color: Color
    when (status) {
        IncidentStatus.Draft -> {
            label = "Draft"
            color = Color(0xFF6750A4)
        }
        IncidentStatus.Pending -> {
            label = "Pending"
            color = Color(0xFFB26A00)
        }
        IncidentStatus.Sent -> {
            label = "Sent"
            color = Color(0xFF206039)
        }
        IncidentStatus.Failed -> {
            label = "Failed"
            color = Color(0xFFB3261E)
        }
    }
    Text(
        text = label,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}
