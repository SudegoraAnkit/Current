package com.sudegoratechglobal.current.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.sudegoratechglobal.current.ui.theme.CleanOffWhite
import com.sudegoratechglobal.current.ui.theme.DarkBackgroundGradient
import com.sudegoratechglobal.current.ui.theme.MutedText
import com.sudegoratechglobal.current.ui.theme.SageGreen
import com.sudegoratechglobal.current.ui.theme.SageToLavenderGradient
import com.sudegoratechglobal.current.ui.viewmodel.TaskViewModel

@Composable
fun OnboardingScreen(viewModel: TaskViewModel) {
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var name by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackgroundGradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                initialOffsetY = { 80 },
                animationSpec = tween(800, easing = EaseOutCubic)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Aesthetic title with a gradient dot
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Current",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = CleanOffWhite,
                        letterSpacing = (-1).sp
                    )
                    Box(
                        modifier = Modifier
                            .padding(bottom = 8.dp, start = 2.dp)
                            .size(8.dp)
                            .background(SageGreen, RoundedCornerShape(50))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "A quiet space designed to lock in your daily focus.",
                    fontSize = 16.sp,
                    color = MutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(64.dp))

                Text(
                    text = "What should we call you?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = CleanOffWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful, minimalist input field with soft borders
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(Color(0xFF222623), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (name.isEmpty()) {
                        Text(
                            text = "Your first name...",
                            color = MutedText,
                            fontSize = 18.sp
                        )
                    }

                    BasicTextField(
                        value = name,
                        onValueChange = {
                            if (it.length <= 15) {
                                name = it
                            }
                        },
                        textStyle = TextStyle(
                            color = CleanOffWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(SageGreen),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (name.trim().isNotEmpty()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.saveOnboardingName(name)
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Pulsing glow button
                val isButtonEnabled = name.trim().isNotEmpty()
                
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.saveOnboardingName(name)
                    },
                    enabled = isButtonEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp)
                        .background(
                            brush = if (isButtonEnabled) SageToLavenderGradient else Brush.linearGradient(
                                listOf(Color(0xFF2E3330), Color(0xFF2E3330))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Text(
                        text = "Enter Space",
                        color = if (isButtonEnabled) Color(0xFF1E3527) else MutedText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
