package com.sudegoratechglobal.current.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sudegoratechglobal.current.MainActivity

class AestheticTimerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("current_app_prefs", Context.MODE_PRIVATE)

        provideContent {
            val taskTitle = prefs.getString("widget_task_title", "Lock-in Clear") ?: "Lock-in Clear"
            val timeText = prefs.getString("widget_time_text", "Focus Mode") ?: "Focus Mode"
            val isRunning = prefs.getBoolean("widget_timer_running", false)

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF222623))) // CardSurface color
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CURRENT",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFB1D7C2)), // SageGreen
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Text(
                    text = timeText,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE7ECE8)), // CleanOffWhite
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = taskTitle,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF909A93)), // MutedText
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

                // Action button to launch app Focus Zone
                Button(
                    text = if (isRunning) "View Flow" else "Start Flow",
                    onClick = actionStartActivity<MainActivity>(),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(36.dp)
                )
            }
        }
    }
}

class AestheticTimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AestheticTimerWidget()
}
