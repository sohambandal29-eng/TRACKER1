package com.example.tracker.ui.screens.timer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.RingtoneManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel = viewModel()
) {
    val timeLeft by viewModel.timeLeft.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val timerMode by viewModel.timerMode.collectAsState()
    val tasks by viewModel.todayTasks.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val selectedSoundscape by viewModel.selectedSoundscape.collectAsState()
    val customSoundUri by viewModel.customSoundUri.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val todayStudyTime by viewModel.todayStudyTime.collectAsState()
    val weeklyStudyTime by viewModel.weeklyStudyTime.collectAsState()
    val focusedTime by viewModel.selectedTaskStudyTime.collectAsState()
    val strictMode by viewModel.strictMode.collectAsState()
    val isStrictBlocked = strictMode && timerMode == TimerMode.POMODORO && isRunning
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                // Persist permission for the URI if needed, though for MediaPlayer it might just work if it's a short session.
                // However, for long term access across reboots, we need to take persistable permission.
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                viewModel.setCustomSoundUri(it.toString())
                viewModel.setSoundscape(Soundscape.CUSTOM)
            }
        }
    )

    LaunchedEffect(viewModel.timerFinished) {
        viewModel.timerFinished.collect {
            // Haptic Feedback
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            
            // Audio Feedback (System Notification Sound)
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, notification)
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val totalTime by viewModel.totalTimeSeconds.collectAsState()
    val progress = if (totalTime > 0) timeLeft.toFloat() / totalTime else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Dynamic Background Blobs based on accentColor
        Canvas(modifier = Modifier.fillMaxSize().blur(60.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(animOffset, 100f),
                    radius = 900f
                ),
                radius = 900f,
                center = Offset(animOffset, 100f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentPurple.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(size.width - animOffset, size.height - 200f),
                    radius = 1100f
                ),
                radius = 1100f,
                center = Offset(size.width - animOffset, size.height - 200f)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                        initialOffsetY = { -40 },
                        animationSpec = tween(1000)
                    )
                ) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "Focus Timer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        ) { padding ->
            val scrollState = rememberScrollState()
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 300)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(1000, delayMillis = 300)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mode Tabs Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TimerTab(
                            text = "Timer",
                            isSelected = timerMode == TimerMode.POMODORO,
                            onClick = { 
                                if (!isStrictBlocked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setTimerMode(TimerMode.POMODORO)
                                }
                            },
                            accentColor = accentColor
                        )
                        TimerTab(
                            text = "Short Break",
                            isSelected = timerMode == TimerMode.SHORT_BREAK,
                            onClick = { 
                                if (!isStrictBlocked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setTimerMode(TimerMode.SHORT_BREAK)
                                }
                            },
                            accentColor = accentColor
                        )
                        TimerTab(
                            text = "Long Break",
                            isSelected = timerMode == TimerMode.LONG_BREAK,
                            onClick = { 
                                if (!isStrictBlocked) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setTimerMode(TimerMode.LONG_BREAK)
                                }
                            },
                            accentColor = accentColor
                        )
                    }

                    // Stats Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem("Today", viewModel.formatDuration(todayStudyTime), accentColor)
                        StatItem("Weekly", viewModel.formatDuration(weeklyStudyTime), accentColor)
                        if (selectedTask != null) {
                            StatItem("Focused", viewModel.formatDuration(focusedTime), accentColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Floating Timer Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(320.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.85f)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        )

                        Canvas(modifier = Modifier.fillMaxSize(0.8f)) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.1f),
                                style = Stroke(width = 4.dp.toPx())
                            )
                        }

                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = tween(500, easing = LinearOutSlowInEasing),
                            label = "progress"
                        )
                        Canvas(modifier = Modifier.fillMaxSize(0.8f)) {
                            drawArc(
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (timerMode == TimerMode.POMODORO) Icons.Default.Park else Icons.Default.Coffee,
                                contentDescription = if (timerMode == TimerMode.POMODORO) "Focusing" else "Resting",
                                tint = accentColor,
                                modifier = Modifier.size(70.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 24.dp)
                            ) {
                                Text(
                                    text = viewModel.formatTime(timeLeft),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 64.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                                IconButton(
                                    onClick = { 
                                        if (!isStrictBlocked) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.resetTimer()
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Reset timer",
                                        tint = if (isStrictBlocked) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Soundscape Selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(Soundscape.entries.toTypedArray()) { soundscape ->
                                Surface(
                                    onClick = { 
                                        when (soundscape) {
                                            Soundscape.CUSTOM -> launcher.launch(arrayOf("audio/*"))
                                            else -> viewModel.setSoundscape(soundscape)
                                        }
                                    },
                                    color = if (selectedSoundscape == soundscape) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (selectedSoundscape == soundscape) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (soundscape == Soundscape.CUSTOM && customSoundUri != null) {
                                                "Custom Audio"
                                            } else soundscape.title,
                                            color = if (selectedSoundscape == soundscape) Color.White else Color.White.copy(alpha = 0.4f),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        if (soundscape == Soundscape.CUSTOM) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = if (selectedSoundscape == soundscape) Color.White else Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Task Selection Section
                    if (timerMode == TimerMode.POMODORO) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "FOCUS ON",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.4f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(tasks.filter { !it.isCompleted }) { task ->
                                    TaskChip(
                                        title = task.title,
                                        isSelected = selectedTask?.id == task.id,
                                        onClick = { 
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.selectTask(task) 
                                        },
                                        accentColor = accentColor
                                    )
                                }
                            }
                        }
                    } else {
                        // Break Status Chip
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Eco,
                                    contentDescription = "Eco break mode active",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recovery Mode",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Action Button
                    Button(
                        onClick = { 
                            if (!isStrictBlocked || isRunning) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(64.dp)
                            .padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStrictBlocked && isRunning) Color.White.copy(alpha = 0.3f) else Color.White,
                            contentColor = BackgroundDark
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRunning) {
                                    if (isStrictBlocked) "Locked" else "Pause"
                                } else "Start",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun TaskChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) accentColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun TimerTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .background(accentColor, RoundedCornerShape(1.5.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}
