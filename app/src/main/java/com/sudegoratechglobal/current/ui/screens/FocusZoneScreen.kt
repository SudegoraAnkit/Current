package com.sudegoratechglobal.current.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudegoratechglobal.current.data.local.TaskEntity
import com.sudegoratechglobal.current.ui.theme.*
import com.sudegoratechglobal.current.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusZoneScreen(viewModel: TaskViewModel) {
    val haptic = LocalHapticFeedback.current
    val activeTasks by viewModel.activeTasks.collectAsState()
    val notesTasks by viewModel.notesTasks.collectAsState()
    val completedTodayTasks by viewModel.completedTodayTasks.collectAsState()
    val brainDumpText by viewModel.brainDumpText.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val streakCount by viewModel.streakCount.collectAsState()
    
    val activeTimerTask by viewModel.activeTimerTask.collectAsState()
    val timerRemaining by viewModel.timerRemainingSeconds.collectAsState()
    val timerRunning by viewModel.timerIsRunning.collectAsState()
    val timerType by viewModel.timerType.collectAsState()

    var nlpInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Keep track of which card is expanded
    var expandedTaskId by remember { mutableStateOf<Long?>(null) }
    
    // Check if "Eat the Frog" distraction-free fullscreen timer is active
    val isFrogModeActive = activeTimerTask != null && timerType == "EAT_THE_FROG"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
    ) {
        if (isFrogModeActive && activeTimerTask != null) {
            // Hyper-minimalist distraction-free "Eat the Frog" overlay
            FrogFocusOverlay(
                task = activeTimerTask!!,
                remainingSeconds = timerRemaining,
                isRunning = timerRunning,
                progress = viewModel.getTimerProgress(),
                onPlayPause = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (timerRunning) viewModel.pauseTimer() else viewModel.resumeTimer()
                },
                onComplete = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.completeTask(activeTimerTask!!)
                },
                onCancel = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.stopTimer()
                }
            )
        } else {
            // Standard Focus Zone screen
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    // Pinned bottom area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                    ) {
                        BrainDumpStrip(
                            value = brainDumpText,
                            onValueChange = { viewModel.brainDumpText.value = it },
                            onSwipeRight = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.elevateBrainDump()
                            },
                            onSwipeLeft = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.dismissBrainDump()
                            }
                        )
                        QuickDropInputBar(
                            value = nlpInput,
                            onValueChange = { nlpInput = it },
                            onSend = {
                                if (nlpInput.trim().isNotEmpty()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addTaskFromNlp(nlpInput)
                                    nlpInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Conversational header row with StreakBadge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hey $userName,",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = CleanOffWhite,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Here's what needs lock-in today.",
                                fontSize = 15.sp,
                                color = MutedText
                            )
                        }
                        StreakBadge(streakCount = streakCount)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val topTasks = activeTasks.take(3)

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (topTasks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Your focus is clean.\nType something below to schedule a task.",
                                        color = MutedText,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        } else {
                            items(topTasks, key = { it.id }) { task ->
                                val isExpanded = expandedTaskId == task.id
                                val isTimerRunningOnThisTask = activeTimerTask?.id == task.id
                                
                                FocusTaskCard(
                                    task = task,
                                    isExpanded = isExpanded,
                                    isTimerActive = isTimerRunningOnThisTask,
                                    remainingSeconds = if (isTimerRunningOnThisTask) timerRemaining else (task.durationMinutes ?: 25) * 60,
                                    timerRunning = isTimerRunningOnThisTask && timerRunning,
                                    timerProgress = if (isTimerRunningOnThisTask) viewModel.getTimerProgress() else 1f,
                                    onCardClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        expandedTaskId = if (isExpanded) null else task.id
                                    },
                                    onComplete = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.completeTask(task)
                                    },
                                    onStartTimer = { type, duration ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.updateTaskExecutionStyle(task, type, duration)
                                        viewModel.startTimer(task.copy(executionStyle = type, durationMinutes = duration), type)
                                    },
                                    onStopTimer = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.stopTimer()
                                    },
                                    onPauseTimer = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.pauseTimer()
                                    },
                                    onResumeTimer = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.resumeTimer()
                                    }
                                )
                            }
                        }

                        // Scratchpad section (Notes)
                        if (notesTasks.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Scratchpad",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CleanOffWhite,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                ScratchpadCarousel(
                                    notes = notesTasks,
                                    onPromote = { note ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.promoteNoteToTask(note)
                                    },
                                    onDelete = { note ->
                                        viewModel.deleteTask(note)
                                    }
                                )
                            }
                        }

                        // Completed Drawer section (Done Today)
                        if (completedTodayTasks.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                CompletedTaskDrawer(
                                    completedTasks = completedTodayTasks,
                                    onUncomplete = { task ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.uncompleteTask(task)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusTaskCard(
    task: TaskEntity,
    isExpanded: Boolean,
    isTimerActive: Boolean,
    remainingSeconds: Int,
    timerRunning: Boolean,
    timerProgress: Float,
    onCardClick: () -> Unit,
    onComplete: () -> Unit,
    onStartTimer: (String, Int) -> Unit,
    onStopTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit
) {
    val durationText = "${remainingSeconds / 60}:${String.format("%02d", remainingSeconds % 60)}"
    val dateText = task.scheduledTime?.let {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
    } ?: "Unscheduled"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        ),
        shape = RoundedCornerShape(24.dp),
        border = CardDefaults.outlinedCardBorder(isExpanded).copy(
            brush = if (isExpanded) SageToLavenderGradient else SolidColor(BorderColor)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .shadow(4.dp, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CleanOffWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (task.vibe != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            VibeChip(vibe = task.vibe, isSmall = true)
                        }
                        if (task.isLocked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = SoftPeach,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "Scheduled at $dateText",
                        fontSize = 13.sp,
                        color = MutedText
                    )
                }

                // Complete circle checkbox
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2220))
                        .clickable(onClick = onComplete)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                if (task.isCompleted) SageGreen else Color.Transparent
                            )
                    )
                }
            }

            // Expanded Timer Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(bottom = 16.dp))

                    if (isTimerActive) {
                        // Showing countdown timer UI
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Focus Lock",
                                    fontSize = 14.sp,
                                    color = MutedText
                                )
                                Text(
                                    text = durationText,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SageGreen
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { if (timerRunning) onPauseTimer() else onResumeTimer() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E3330)
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.size(48.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = if (timerRunning) Icons.Default.PlayArrow /* Pause icon is missing, use Play/Refresh */ else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = CleanOffWhite
                                    )
                                }
                                Button(
                                    onClick = onStopTimer,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3F2727)
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier.size(48.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop",
                                        tint = SoftPink
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LinearProgressIndicator(
                            progress = { timerProgress },
                            color = SageGreen,
                            trackColor = Color(0xFF1E2220),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    } else {
                        // Options to select execution framework
                        Text(
                            text = "Choose Execution Style",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MutedText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Pomodoro (25/5)
                            ExecutionOptionButton(
                                label = "Pomodoro",
                                description = "25m focus",
                                icon = Icons.Default.Notifications,
                                gradient = SageToLavenderGradient,
                                modifier = Modifier.weight(1f),
                                onClick = { onStartTimer("POMODORO", 25) }
                            )

                            // Time Boxing (15m quick chunk)
                            ExecutionOptionButton(
                                label = "Time Box",
                                description = "15m chunk",
                                icon = Icons.AutoMirrored.Filled.List,
                                gradient = LavenderToPeachGradient,
                                modifier = Modifier.weight(1f),
                                onClick = { onStartTimer("TIME_BOXING", 15) }
                            )

                            // Eat the Frog (Distraction free dashboard)
                            ExecutionOptionButton(
                                label = "Eat Frog",
                                description = "Toughest task",
                                icon = Icons.Default.Star,
                                gradient = Brush.linearGradient(listOf(SoftPeach, SoftPink)),
                                modifier = Modifier.weight(1f),
                                onClick = { onStartTimer("EAT_THE_FROG", 45) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutionOptionButton(
    label: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1B1E1C)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .shadow(1.dp, RoundedCornerShape(16.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(gradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF1E3527),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CleanOffWhite,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MutedText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun QuickDropInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .shadow(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Task input text field
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF1B1E1C), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Call Mom tomorrow at 5pm...",
                            color = MutedText,
                            fontSize = 15.sp
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(
                            color = CleanOffWhite,
                            fontSize = 15.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(SageGreen),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { onSend() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Send Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (value.trim().isNotEmpty()) SageToLavenderGradient else SolidColor(Color(0xFF2E3330))
                        )
                        .clickable(
                            enabled = value.trim().isNotEmpty(),
                            onClick = onSend
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (value.trim().isNotEmpty()) Color(0xFF1E3527) else MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            // Fast Append Toggles (Help users type temporal and attribute details quickly)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistTag(
                    text = "Lock Task 🔒",
                    isSelected = value.contains("lock"),
                    onClick = {
                        if (value.contains("lock")) {
                            onValueChange(value.replace(" lock", "").replace("lock", "").trim())
                        } else {
                            onValueChange("$value lock".trim())
                        }
                    }
                )
                
                AssistTag(
                    text = "Priority !high ⚠️",
                    isSelected = value.contains("!high"),
                    onClick = {
                        if (value.contains("!high")) {
                            onValueChange(value.replace(" !high", "").replace("!high", "").trim())
                        } else {
                            onValueChange("$value !high".trim())
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistTag(
                    text = "⚡ vibe:deep",
                    isSelected = value.contains("vibe:deep"),
                    onClick = {
                        if (value.contains("vibe:deep")) {
                            onValueChange(value.replace(" vibe:deep", "").replace("vibe:deep", "").trim())
                        } else {
                            val cleaned = value.replace(" vibe:low", "").replace("vibe:low", "")
                                               .replace(" vibe:home", "").replace("vibe:home", "")
                            onValueChange("$cleaned vibe:deep".trim())
                        }
                    }
                )
                
                AssistTag(
                    text = "☕ vibe:low",
                    isSelected = value.contains("vibe:low"),
                    onClick = {
                        if (value.contains("vibe:low")) {
                            onValueChange(value.replace(" vibe:low", "").replace("vibe:low", "").trim())
                        } else {
                            val cleaned = value.replace(" vibe:deep", "").replace("vibe:deep", "")
                                               .replace(" vibe:home", "").replace("vibe:home", "")
                            onValueChange("$cleaned vibe:low".trim())
                        }
                    }
                )

                AssistTag(
                    text = "🏠 vibe:home",
                    isSelected = value.contains("vibe:home"),
                    onClick = {
                        if (value.contains("vibe:home")) {
                            onValueChange(value.replace(" vibe:home", "").replace("vibe:home", "").trim())
                        } else {
                            val cleaned = value.replace(" vibe:deep", "").replace("vibe:deep", "")
                                               .replace(" vibe:low", "").replace("vibe:low", "")
                            onValueChange("$cleaned vibe:home".trim())
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AssistTag(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF2C3E33) else Color(0xFF1E2220))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isSelected) SageGreen else MutedText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FrogFocusOverlay(
    task: TaskEntity,
    remainingSeconds: Int,
    isRunning: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val durationText = "${remainingSeconds / 60}:${String.format("%02d", remainingSeconds % 60)}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FrogFocusGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "EAT THE FROG",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SoftPeach,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = task.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CleanOffWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "Tackle your hardest task of the day with absolute zero distraction.",
                fontSize = 14.sp,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Large, pulsing circular progress countdown
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw track
                    drawCircle(
                        color = Color(0xFF1E2220),
                        style = Stroke(width = 16f)
                    )
                    // Draw progress arc
                    drawArc(
                        brush = SageToLavenderGradient,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 16f)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = durationText,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanOffWhite
                    )
                    Text(
                        text = if (isRunning) "focused" else "paused",
                        fontSize = 14.sp,
                        color = MutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Complete Button
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = "Complete Frog",
                        color = Color(0xFF1E3527),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                // Pause / Pause Action
                Button(
                    onClick = onPlayPause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF222623)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = if (isRunning) "Pause" else "Resume",
                        color = CleanOffWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exit / Stop
            Text(
                text = "Exit Distraction Free Mode",
                fontSize = 14.sp,
                color = SoftPink,
                modifier = Modifier
                    .clickable(onClick = onCancel)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun CompletedTaskDrawer(
    completedTasks: List<TaskEntity>,
    onUncomplete: (TaskEntity) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161917))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Completed Tasks",
                    tint = SageGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Done Today (${completedTasks.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanOffWhite
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MutedText
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(bottom = 6.dp))
                completedTasks.forEach { task ->
                    CompletedTaskRow(
                        task = task,
                        onUncomplete = { onUncomplete(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun CompletedTaskRow(
    task: TaskEntity,
    onUncomplete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(SageGreen)
                    .clickable(onClick = onUncomplete)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF1E2220))
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = task.title,
                fontSize = 14.sp,
                color = MutedText,
                style = TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
            )
        }
        if (task.vibe != null) {
            Spacer(modifier = Modifier.width(8.dp))
            VibeChip(vibe = task.vibe, isSmall = true)
        }
    }
}

@Composable
fun ScratchpadCarousel(
    notes: List<TaskEntity>,
    onPromote: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(notes, key = { it.id }) { note ->
            ScratchpadNoteCard(
                note = note,
                onPromote = { onPromote(note) },
                onDelete = { onDelete(note) }
            )
        }
    }
}

@Composable
fun ScratchpadNoteCard(
    note: TaskEntity,
    onPromote: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetY by remember { mutableStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .width(160.dp)
            .height(110.dp)
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .pointerInput(note.id) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY < -150f) {
                            onPromote()
                        }
                        offsetY = 0f
                    },
                    onDragCancel = {
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Only allow dragging upwards (negative Y)
                        if (dragAmount.y < 0 || offsetY < 0) {
                            offsetY += dragAmount.y
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (note.vibe != null) {
                        VibeChip(vibe = note.vibe, isSmall = true)
                    } else {
                        Box(modifier = Modifier.size(1.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        tint = MutedText,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(onClick = onDelete)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = CleanOffWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Swipe up to promote ↑",
                fontSize = 10.sp,
                color = MutedText
            )
        }
    }
}

@Composable
fun BrainDumpStrip(
    value: String,
    onValueChange: (String) -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }

    AnimatedVisibility(
        visible = true, // Always show the strip (its empty state is handled below)
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E2220)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX > 200f) {
                                    onSwipeRight()
                                } else if (offsetX < -200f) {
                                    onSwipeLeft()
                                }
                                offsetX = 0f
                            },
                            onDragCancel = {
                                offsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount
                            }
                        )
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Brain Dump",
                        tint = SageGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = "Brain Dump: Type thought, swipe right to save...",
                                color = MutedText,
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            textStyle = TextStyle(color = CleanOffWhite, fontSize = 14.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(SageGreen),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (value.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "← swipe clear | save →",
                            fontSize = 10.sp,
                            color = MutedText
                        )
                    }
                }
            }
        }
    }
}

private data class VibeConfig(
    val emoji: String,
    val label: String,
    val color: Color,
    val bgColor: Color
)

@Composable
fun VibeChip(vibe: String, isSmall: Boolean = false) {
    val config = when (vibe) {
        "DEEP_FOCUS" -> VibeConfig("⚡", "Deep Focus", Color(0xFFFFD54F), Color(0xFF332D15))
        "LOW_ENERGY" -> VibeConfig("☕", "Low Energy", Color(0xFF81C784), Color(0xFF1B2E21))
        "HOME_BASE" -> VibeConfig("🏠", "Home Base", Color(0xFF64B5F6), Color(0xFF152A3C))
        else -> VibeConfig("🏷️", vibe, CleanOffWhite, Color(0xFF222624))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(config.bgColor)
            .padding(
                horizontal = if (isSmall) 6.dp else 10.dp,
                vertical = if (isSmall) 2.dp else 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = config.emoji, fontSize = if (isSmall) 10.sp else 12.sp)
            Text(
                text = config.label,
                fontSize = if (isSmall) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = config.color
            )
        }
    }
}
