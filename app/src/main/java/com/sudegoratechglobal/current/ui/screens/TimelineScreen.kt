package com.sudegoratechglobal.current.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudegoratechglobal.current.data.local.TaskEntity
import com.sudegoratechglobal.current.ui.theme.*
import com.sudegoratechglobal.current.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TimelineScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.activeTasks.collectAsState()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Next up",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CleanOffWhite,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Swipe right to push to Tomorrow. Drag vertically to slide time.",
                fontSize = 14.sp,
                color = MutedText
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your timeline is clean.",
                        color = MutedText,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TimelineTaskCard(
                            task = task,
                            viewModel = viewModel,
                            onSwipeToTomorrow = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.swipeTaskToTomorrow(task)
                            },
                            onTimeSlide = { offsetMinutes ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val newTime = task.scheduledTime + (offsetMinutes * 60 * 1000)
                                viewModel.rescheduleTask(task, newTime)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineTaskCard(
    task: TaskEntity,
    viewModel: TaskViewModel,
    onSwipeToTomorrow: () -> Unit,
    onTimeSlide: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var dragTimeOffset by remember { mutableStateOf(0) }
    var isDraggingVertically by remember { mutableStateOf(false) }

    val dateText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(task.scheduledTime))

    // Vertical drag handling to slide time
    val verticalDragModifier = Modifier.pointerInput(task.id) {
        detectDragGestures(
            onDragStart = {
                isDraggingVertically = true
            },
            onDragEnd = {
                isDraggingVertically = false
                if (dragTimeOffset != 0) {
                    onTimeSlide(dragTimeOffset)
                    dragTimeOffset = 0
                }
            },
            onDragCancel = {
                isDraggingVertically = false
                dragTimeOffset = 0
            },
            onDrag = { change, dragAmount ->
                change.consume()
                // Each 10 pixels of vertical drag equals 15 minutes of shift
                val deltaY = -dragAmount.y
                val offsetMins = (deltaY / 10f).roundToInt() * 15
                if (offsetMins != dragTimeOffset) {
                    dragTimeOffset = offsetMins
                }
            }
        )
    }

    // Horizontal swipe modifier (Swipe right to push to Tomorrow)
    val horizontalSwipeModifier = Modifier.pointerInput(task.id) {
        detectHorizontalDragGestures(
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                // Only allow swiping right (positive X)
                if (dragAmount > 0 || offsetX.value > 0) {
                    scope.launch {
                        offsetX.snapTo((offsetX.value + dragAmount).coerceAtLeast(0f))
                    }
                }
            },
            onDragEnd = {
                if (offsetX.value > 250f) {
                    scope.launch {
                        offsetX.animateTo(800f)
                        onSwipeToTomorrow()
                        offsetX.snapTo(0f)
                    }
                } else {
                    scope.launch {
                        offsetX.animateTo(0f)
                    }
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Back layer indicating Tomorrow push
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF28241B)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (offsetX.value > 50f) 1f else 0f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Tomorrow",
                    tint = SoftPeach
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Pushed to Tomorrow",
                    color = SoftPeach,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Front task card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDraggingVertically) Color(0xFF2B2E2C) else CardSurface
            ),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder(isDraggingVertically).copy(
                brush = if (isDraggingVertically) LavenderToPeachGradient else SolidColor(BorderColor)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .then(horizontalSwipeModifier)
                .then(verticalDragModifier)
                .shadow(if (isDraggingVertically) 8.dp else 2.dp, RoundedCornerShape(20.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Vertical priority strip
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .clip(CircleShape)
                        .background(
                            when (task.priority) {
                                1 -> SoftPink
                                2 -> SageGreen
                                else -> SoftLavender
                            }
                        )
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanOffWhite
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = if (isDraggingVertically && dragTimeOffset != 0) {
                                val direction = if (dragTimeOffset > 0) "+" else ""
                                "Rescheduling ($direction${dragTimeOffset}m)"
                            } else {
                                dateText
                            },
                            fontSize = 13.sp,
                            color = if (isDraggingVertically && dragTimeOffset != 0) SageGreen else MutedText,
                            fontWeight = if (isDraggingVertically) FontWeight.Bold else FontWeight.Normal
                        )
                        if (task.isLocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Locked",
                                fontSize = 11.sp,
                                color = SoftPeach,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Vertical Drag handle icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Drag Handle",
                        tint = MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Info, // Reusing icon for arrow down look
                        contentDescription = "Drag Handle",
                        tint = MutedText,
                        modifier = Modifier.size(20.dp).graphicsLayer(rotationZ = 180f)
                    )
                }
            }
        }
    }
}
