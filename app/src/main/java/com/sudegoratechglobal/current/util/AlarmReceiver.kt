package com.sudegoratechglobal.current.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sudegoratechglobal.current.data.local.AppDatabase
import com.sudegoratechglobal.current.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra("task_id", -1L)
        val taskTitle = intent.getStringExtra("task_title") ?: "Task"
        val contact = intent.getStringExtra("contact")
        val timerType = intent.getStringExtra("timer_type") ?: "timer"

        createNotificationChannel(context)

        when (action) {
            "com.sudegoratechglobal.current.ACTION_TIMER_COMPLETE" -> {
                sendTimerCompleteNotification(context, taskTitle, timerType)
            }
            "com.sudegoratechglobal.current.ACTION_PROCRASTINATION_TAX" -> {
                if (taskId != -1L) {
                    checkAndTriggerProcrastinationTax(context, taskId, taskTitle, contact)
                }
            }
        }
    }

    private fun checkAndTriggerProcrastinationTax(
        context: Context,
        taskId: Long,
        taskTitle: String,
        contact: String?
    ) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val task = db.taskDao().getTaskById(taskId)
            // Double check if the task is still active and locked
            if (task != null && !task.isCompleted && task.isLocked) {
                // Trigger Procrastination Tax
                val message = "Current App Alert: I locked in the task \"$taskTitle\" but missed the deadline! Tracking productivity."

                var smsSent = false
                if (!contact.isNullOrBlank()) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.SEND_SMS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.getSystemService(SmsManager::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                SmsManager.getDefault()
                            }
                            smsManager.sendTextMessage(contact, null, message, null, null)
                            smsSent = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                sendProcrastinationNotification(context, taskTitle, contact, message, smsSent)
            }
        }
    }

    private fun sendTimerCompleteNotification(context: Context, taskTitle: String, timerType: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val mainIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            100,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when (timerType.lowercase()) {
            "pomodoro" -> "Focus session done! Time for a premium 5-minute break."
            "break" -> "Break's up! Back to the Focus Zone."
            else -> "Time is up! Let's check in on progress."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Current Focus Complete")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(TIMER_NOTIFICATION_ID, notification)
    }

    private fun sendProcrastinationNotification(
        context: Context,
        taskTitle: String,
        contact: String?,
        message: String,
        smsSent: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent to open SMS app if not sent programmatically
        val intent = if (!smsSent && !contact.isNullOrBlank()) {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$contact")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val description = if (smsSent) {
            "Deadline missed! Sent text notification to your study partner ($contact)."
        } else if (!contact.isNullOrBlank()) {
            "Deadline missed! Tap to send warning text to your partner ($contact)."
        } else {
            "Deadline missed! Lock-in tax applied to your focus streak."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Lock-in Deadline Missed! ⚠️")
            .setContentText("Missed: \"$taskTitle\"")
            .setSubText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Missed: \"$taskTitle\"\n$description"))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(TAX_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Current Alerts"
            val descriptionText = "Notifications for focus timers and tax warnings"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "current_alerts_channel"
        private const val TIMER_NOTIFICATION_ID = 1001
        private const val TAX_NOTIFICATION_ID = 1002
    }
}
