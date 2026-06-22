package com.sudegoratechglobal.current.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sudegoratechglobal.current.ui.theme.*

@Composable
fun VibeCheckCardDialog(
    totalMinutes: Long,
    completedCount: Int,
    pomodoroMinutes: Long,
    frogMinutes: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Determine weekly persona based on focus parameters
    val (personaName, personaDescription) = when {
        frogMinutes > pomodoroMinutes && frogMinutes > 0 -> {
            "THE FROG EATER 🐸" to "You tackle your hardest challenges first thing. Zero hesitation, maximum execution."
        }
        totalMinutes > 90 -> {
            "THE HYPER-FOCUS PRO ⚡" to "Deep, long work blocks are your superpower. Hours feel like minutes when you're in the zone."
        }
        completedCount > 8 -> {
            "THE TASK DESTROYER ⚔️" to "Nothing survives your timeline. You clear action items like a checklist speedrunner."
        }
        pomodoroMinutes > 0 -> {
            "THE INTERVAL MASTER ⏳" to "You balance deep focus and healthy recovery perfectly. Rhythmic productivity at its finest."
        }
        else -> {
            "THE STEADY FLOW 🧘" to "Quiet, consistent progress. You don't rush, you don't stall. You just keep moving."
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // Make it full screen width
        )
    ) {
        Surface(
            color = DarkCharcoal,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header Row
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Vibe Check",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanOffWhite
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The 9:16 Shareable Card Layout
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(LavenderToPeachGradient)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Card Header
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CURRENT VIBE CHECK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E223D),
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Weekly Productivity Profile",
                                fontSize = 13.sp,
                                color = Color(0xFF2E223D).copy(alpha = 0.7f)
                            )
                        }

                        // Persona Center Block
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 24.dp)
                        ) {
                            Text(
                                text = personaName,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF161917),
                                textAlign = TextAlign.Center,
                                lineHeight = 36.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = personaDescription,
                                fontSize = 14.sp,
                                color = Color(0xFF2E223D),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                lineHeight = 20.sp
                            )
                        }

                        // Stats Summary Row
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF161917).copy(alpha = 0.15f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$totalMinutes",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF161917)
                                )
                                Text(
                                    text = "Focus Mins",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E223D)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$completedCount",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF161917)
                                )
                                Text(
                                    text = "Done Tasks",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E223D)
                                )
                            }
                        }

                        // Branding Footer
                        Text(
                            text = "flow offline • current app",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E223D).copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // CTA Button (Share story card)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val shareText = """
                            ✨ My Weekly Focus Persona on Current App: $personaName
                            🧘 $personaDescription
                            ⚡ Locked in $totalMinutes minutes of focus time this week!
                            
                            Download Current and flow offline.
                        """.trimIndent()

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Vibe Check"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF1E3527)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Share Weekly Persona",
                        color = Color(0xFF1E3527),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
