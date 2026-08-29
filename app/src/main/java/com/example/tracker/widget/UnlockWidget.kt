package com.example.tracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import com.example.tracker.MainActivity

class UnlockWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            UnlockWidgetContent()
        }
    }

    @Composable
    private fun UnlockWidgetContent() {
        // In a real app, this would be fetched from a Repository/Pref
        val unlockCount = 12 
        val goal = 25

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PHONE UNLOCKS",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = GlanceModifier.height(4.dp))
            
            Text(
                text = "$unlockCount",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            Text(
                text = "Goal: $goal",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            // Simple Progress Bar representation
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(GlanceTheme.colors.onSurfaceVariant)
            ) {
                val progress = (unlockCount.toFloat() / goal).coerceIn(0f, 1f)
                if (progress > 0) {
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .background(GlanceTheme.colors.primary)
                    ) {}
                    if (progress < 1f) {
                        Spacer(modifier = GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }
}

class UnlockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UnlockWidget()
}
