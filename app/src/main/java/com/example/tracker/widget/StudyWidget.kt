package com.example.tracker.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import com.example.tracker.MainActivity
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.data.local.entities.TimetableEntity
import com.example.tracker.utils.FirebaseAuthManager
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.util.*

class StudyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val userId = FirebaseAuthManager.getCurrentUserId()
        
        val nextItem = if (userId != null) {
            val today = Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Monday"
            val timetableItems = db.timetableDao().getTimetableByDay(userId, today).first()
            
            timetableItems
                .filter { 
                    try {
                        val now = LocalTime.now()
                        val itemTime = LocalTime.parse(it.timing)
                        itemTime.isAfter(now)
                    } catch (_: Exception) {
                        false
                    }
                }
                .minByOrNull { it.timing }
        } else {
            null
        }

        provideContent {
            WidgetContent(nextItem)
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(nextItem: TimetableEntity?) {
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
                text = "Next Session",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            if (nextItem != null) {
                Text(
                    text = nextItem.subject,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = "${nextItem.timing} - ${nextItem.endTime}",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 14.sp
                    )
                )
            } else {
                Text(
                    text = "No more sessions",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

class StudyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyWidget()
}
