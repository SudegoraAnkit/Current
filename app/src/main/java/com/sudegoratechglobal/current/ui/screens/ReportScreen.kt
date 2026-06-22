package com.sudegoratechglobal.current.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudegoratechglobal.current.data.remote.GoogleDriveService
import com.sudegoratechglobal.current.ui.theme.*
import com.sudegoratechglobal.current.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val allTasks by viewModel.allTasks.collectAsState()
    val isDriveLinked by viewModel.isDriveLinked.collectAsState()
    val driveSyncState by viewModel.driveSyncState.collectAsState()

    var showVibeCheckDialog by remember { mutableStateOf(false) }

    // Calculate mock/real metrics from tasks list
    val completedTasks = allTasks.filter { it.isCompleted }
    val totalFocusedSeconds = allTasks.sumOf { it.elapsedTime }
    val totalFocusedMinutes = totalFocusedSeconds / 60

    // Group elapsed time by execution style
    val pomodoroMinutes = allTasks.filter { it.executionStyle == "POMODORO" }.sumOf { it.elapsedTime } / 60
    val timeBoxMinutes = allTasks.filter { it.executionStyle == "TIME_BOXING" }.sumOf { it.elapsedTime } / 60
    val frogMinutes = allTasks.filter { it.executionStyle == "EAT_THE_FROG" }.sumOf { it.elapsedTime } / 60

    val lastSyncTime = GoogleDriveService.getLastSyncTime(context)
    val syncText = if (lastSyncTime > 0) {
        val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        "Last backed up: ${sdf.format(Date(lastSyncTime))}"
    } else {
        "Never backed up"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Track",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CleanOffWhite,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "See where your lock-in time was spent this week.",
                fontSize = 14.sp,
                color = MutedText
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Main stats card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CardSurface
                ),
                shape = RoundedCornerShape(24.dp),
                border = CardDefaults.outlinedCardBorder(false).copy(
                    brush = SolidColor(BorderColor)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL FOCUS TIME",
                            fontSize = 12.sp,
                            color = MutedText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$totalFocusedMinutes mins",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = SageGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(SageToLavenderGradient, CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Share report text
                                val shareText = """
                                    📊 My Weekly Focus Report on Current:
                                    - Total Focus Time: $totalFocusedMinutes minutes
                                    - Tasks Completed: ${completedTasks.size}
                                    - Pomodoro Mode: $pomodoroMinutes mins
                                    - Timebox Mode: $timeBoxMinutes mins
                                    - Frog Mode: $frogMinutes mins
                                    
                                    Sent from Current app.
                                """.trimIndent()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Focus Report"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFF1E3527),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Weekly Vibe Check Card Trigger Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CardSurface
                ),
                shape = RoundedCornerShape(24.dp),
                border = CardDefaults.outlinedCardBorder(false).copy(
                    brush = SolidColor(BorderColor)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showVibeCheckDialog = true
                    }
                    .shadow(2.dp, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WEEKLY PRODUCTIVITY PERSONA",
                            fontSize = 11.sp,
                            color = MutedText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sunday Vibe Check",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanOffWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Auto-generate your GenZ-style story summary.",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                    Text(
                        text = "✨",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Canvas-based gradient chart
            Text(
                text = "Focus distribution",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CleanOffWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CardSurface
                ),
                shape = RoundedCornerShape(24.dp),
                border = CardDefaults.outlinedCardBorder(false).copy(
                    brush = SolidColor(BorderColor)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Time spent per execution style",
                        color = MutedText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    val maxVal = maxOf(pomodoroMinutes, timeBoxMinutes, frogMinutes, 1L).toFloat()
                    val pPercent = pomodoroMinutes / maxVal
                    val tPercent = timeBoxMinutes / maxVal
                    val fPercent = frogMinutes / maxVal

                    // Draw custom bar charts on canvas
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ChartBarRow(
                            label = "Pomodoro",
                            valueText = "$pomodoroMinutes mins",
                            percentage = pPercent,
                            brush = SageToLavenderGradient
                        )
                        ChartBarRow(
                            label = "Time Box",
                            valueText = "$timeBoxMinutes mins",
                            percentage = tPercent,
                            brush = LavenderToPeachGradient
                        )
                        ChartBarRow(
                            label = "Eat the Frog",
                            valueText = "$frogMinutes mins",
                            percentage = fPercent,
                            brush = Brush.linearGradient(listOf(SoftPeach, SoftPink))
                        )
                    }
                }
            }

            // Google Drive settings card
            Text(
                text = "Backup & Cloud Sync",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CleanOffWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CardSurface
                ),
                shape = RoundedCornerShape(24.dp),
                border = CardDefaults.outlinedCardBorder(false).copy(
                    brush = SolidColor(BorderColor)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Personal Google Drive",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOffWhite
                            )
                            Text(
                                text = syncText,
                                fontSize = 13.sp,
                                color = MutedText
                            )
                        }

                        // Link button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isDriveLinked) {
                                    viewModel.unlinkGoogleDrive()
                                } else {
                                    viewModel.linkGoogleDrive()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDriveLinked) Color(0xFF3F2727) else SageGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isDriveLinked) "Disconnect" else "Link",
                                color = if (isDriveLinked) SoftPink else Color(0xFF1E3527),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Loading/Syncing logs state indicators
                    AnimatedVisibility(
                        visible = driveSyncState is GoogleDriveService.SyncState.Syncing ||
                                driveSyncState is GoogleDriveService.SyncState.Success
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .background(Color(0xFF1E2220), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            if (driveSyncState is GoogleDriveService.SyncState.Syncing) {
                                CircularProgressIndicator(
                                    color = SageGreen,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Backing up JSON tasks payload...",
                                    color = MutedText,
                                    fontSize = 13.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Synced",
                                    tint = SageGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sync connection active. Locked in.",
                                    color = SageGreen,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Privacy Policy Dialog trigger
            var showPrivacyDialog by remember { mutableStateOf(false) }

            Text(
                text = "Privacy Policy & Disclosures",
                color = SageGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacyDialog = true }
                    .padding(vertical = 12.dp)
            )

            if (showPrivacyDialog) {
                AlertDialog(
                    onDismissRequest = { showPrivacyDialog = false },
                    title = {
                        Text(
                            text = "Privacy Policy",
                            fontWeight = FontWeight.Bold,
                            color = CleanOffWhite
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .heightIn(max = 300.dp)
                        ) {
                            Text(
                                text = "Current is a local-first, offline-first application.\n\n" +
                                        "1. Zero Server: All tasks, priorities, and focus durations are stored on your device's local database. We do not collect or monitor your data.\n\n" +
                                        "2. Google Drive: Backups are stored directly in your personal Google Drive app folder. Developers have zero access to this storage.\n\n" +
                                        "3. SMS Accountability: SMS warnings are only sent if explicitly configured for procrastination tax lockouts.",
                                fontSize = 13.sp,
                                color = MutedText,
                                lineHeight = 18.sp
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPrivacyDialog = false }) {
                            Text(text = "Okay", color = SageGreen)
                        }
                    },
                    containerColor = CardSurface
                )
            }
        }

        if (showVibeCheckDialog) {
            VibeCheckCardDialog(
                totalMinutes = totalFocusedMinutes,
                completedCount = completedTasks.size,
                pomodoroMinutes = pomodoroMinutes,
                frogMinutes = frogMinutes,
                onDismiss = { showVibeCheckDialog = false }
            )
        }
    }
}

@Composable
fun ChartBarRow(
    label: String,
    valueText: String,
    percentage: Float,
    brush: Brush
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label, color = CleanOffWhite, fontSize = 14.sp)
            Text(text = valueText, color = MutedText, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        
        // Progress bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFF1E2220), RoundedCornerShape(5.dp))
        ) {
            // Animated bar width expansion
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage.coerceIn(0.01f, 1f))
            ) {
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }
    }
}
