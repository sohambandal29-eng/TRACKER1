package com.example.tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class UsageData(
    val label: String,
    val durationMinutes: Int,
    val color: Color
)

@Composable
fun UsageDonutChart(
    usageList: List<UsageData>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp
) {
    val totalMinutes = usageList.sumOf { it.durationMinutes }.toFloat()
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            
            if (totalMinutes == 0f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            } else {
                usageList.forEach { data ->
                    val sweepAngle = (data.durationMinutes / totalMinutes) * 360f
                    drawArc(
                        color = data.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val totalHours = (totalMinutes / 60).toInt()
            val remainingMinutes = (totalMinutes % 60).toInt()
            
            Text(
                text = if (totalHours > 0) "${totalHours}h ${remainingMinutes}m" else "${remainingMinutes}m",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "Total Usage",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
        }
    }
}
