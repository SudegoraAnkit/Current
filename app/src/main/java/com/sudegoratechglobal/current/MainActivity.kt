package com.sudegoratechglobal.current

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.sudegoratechglobal.current.ui.screens.FocusZoneScreen
import com.sudegoratechglobal.current.ui.screens.OnboardingScreen
import com.sudegoratechglobal.current.ui.screens.ReportScreen
import com.sudegoratechglobal.current.ui.screens.TimelineScreen
import com.sudegoratechglobal.current.ui.theme.*
import com.sudegoratechglobal.current.ui.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels()
    private val snackbarHostState = SnackbarHostState()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Notification permission denied - handle gracefully
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for notifications runtime permission for alarms
        checkAndRequestNotifications()

        // Check if launched with a deep link
        intent?.let { handleDeepLink(it) }

        setContent {
            CurrentTheme {
                val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (onboardingCompleted) {
                        MainLayout(viewModel, snackbarHostState)
                    } else {
                        OnboardingScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun checkAndRequestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleDeepLink(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            val data = intent.data
            if (data != null && data.scheme == "current" && data.host == "sync") {
                val url = data.getQueryParameter("url")
                if (!url.isNullOrBlank()) {
                    viewModel.importTeamTasks(url) { message ->
                        lifecycleScope.launch {
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainLayout(viewModel: TaskViewModel, snackbarHostState: SnackbarHostState) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Focus, 1 = Timeline, 2 = Track
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Premium custom bottom navigation bar
            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 12.dp)
                ) {
                    BottomNavItem(
                        label = "Focus",
                        icon = Icons.Default.Star,
                        isSelected = selectedTab == 0,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedTab = 0
                        }
                    )
                    BottomNavItem(
                        label = "Timeline",
                        icon = Icons.Default.List,
                        isSelected = selectedTab == 1,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedTab = 1
                        }
                    )
                    BottomNavItem(
                        label = "Track",
                        icon = Icons.Default.PlayArrow, // Reusing icon for track
                        isSelected = selectedTab == 2,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedTab = 2
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedTab) {
                0 -> FocusZoneScreen(viewModel)
                1 -> TimelineScreen(viewModel)
                2 -> ReportScreen(viewModel)
            }
        }
    }
}

@Composable
fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) SageGreen else MutedText,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) SageGreen else MutedText
        )
    }
}
