package com.sudegoratechglobal.current.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudegoratechglobal.current.ui.theme.*

@Composable
fun StreakBadge(streakCount: Int) {
    val haptic = LocalHapticFeedback.current
    var showExplanationDialog by remember { mutableStateOf(false) }

    // Pulsing/flicker animation for the streak flame to gamify engagement
    val infiniteTransition = rememberInfiniteTransition(label = "streak_flicker")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_glow"
    )

    // Warm gradient for the streak glow
    val fireGradient = Brush.horizontalGradient(
        colors = listOf(SoftPink, SoftPeach)
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E2220)) // Pinned dark container for high contrast
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showExplanationDialog = true
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Animated Flame Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(20.dp)
            ) {
                // Glow effect in background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.3f)
                        .background(
                            brush = fireGradient,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .scale(glowAlpha)
                )
                Text(
                    text = "🔥",
                    fontSize = 13.sp
                )
            }

            Text(
                text = if (streakCount == 1) "1 Day" else "$streakCount Days",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CleanOffWhite
            )
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = {
                Text(
                    text = "Productivity Streak",
                    fontWeight = FontWeight.Bold,
                    color = CleanOffWhite
                )
            },
            text = {
                Column {
                    Text(
                        text = if (streakCount > 0) {
                            "You are currently on a $streakCount-day focus streak!"
                        } else {
                            "No active focus streak yet."
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = SageGreen,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (streakCount > 0) {
                            "Keep lock-in active! Complete at least one task every day to extend your streak and maintain your momentum."
                        } else {
                            "Start lock-in! Complete a task today to light your focus flame and start your streak."
                        },
                        fontSize = 13.sp,
                        color = MutedText,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExplanationDialog = false }) {
                    Text(text = "Lock in", color = SageGreen)
                }
            },
            containerColor = CardSurface
        )
    }
}
