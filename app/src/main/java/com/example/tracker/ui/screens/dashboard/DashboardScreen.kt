package com.example.tracker.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.ui.MainViewModel
import com.example.tracker.ui.components.GlassCard
import com.example.tracker.ui.components.UsageData
import com.example.tracker.ui.components.UsageDonutChart
import com.example.tracker.ui.screens.tasks.TaskViewModel
import com.example.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTasks: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: TaskViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val tasks by viewModel.todayTasks.collectAsState()
    val todayStudyTime by viewModel.todayStudyTime.collectAsState()
    val weeklyStudyTime by viewModel.weeklyStudyTime.collectAsState()
    val streakCount by viewModel.streakCount.collectAsState()
    val userName by mainViewModel.userName.collectAsState()

    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }

    val completedTasks = tasks.filter { it.isCompleted }.size
    val totalTasks = tasks.size
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    val studyHours = todayStudyTime / 3600
    val studyMinutes = (todayStudyTime % 3600) / 60
    val studySeconds = todayStudyTime % 60

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

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Animated Background
        Canvas(modifier = Modifier.fillMaxSize().blur(60.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.7f), Color.Transparent),
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
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { -40 }
                ) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (userName.isNullOrEmpty()) "Dashboard" else "Hi, $userName",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToProfile) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White.copy(alpha = 0.7f))
                            }
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White.copy(alpha = 0.7f))
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800, 400)) + slideInVertically(tween(800, 400)) { 40 }
                ) {
                    ExtendedFloatingActionButton(
                        onClick = onNavigateToTasks,
                        containerColor = Color.White,
                        contentColor = BackgroundDark,
                        shape = RoundedCornerShape(20.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                        modifier = Modifier
                            .padding(bottom = 16.dp, end = 8.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("New Task", fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { padding ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200)) { 40 }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 32.dp,
                            containerColor = GlassSurface,
                            contentPadding = PaddingValues(24.dp),
                            showAccentGlow = true
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = PrimaryAccent.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "DAILY GOAL",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Today's Progress",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$completedTasks of $totalTasks tasks completed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                                
                                Box(contentAlignment = Alignment.Center) {
                                    // Background Glow
                                    Canvas(modifier = Modifier.size(90.dp)) {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(PrimaryAccent.copy(alpha = 0.15f), Color.Transparent)
                                            )
                                        )
                                    }
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.size(76.dp),
                                        strokeWidth = 8.dp,
                                        strokeCap = StrokeCap.Round,
                                        color = PrimaryAccent,
                                        trackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 24.dp,
                                contentPadding = PaddingValues(20.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimaryAccent.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Whatshot, null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "$streakCount Days",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        "STREAK",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            GlassCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 24.dp,
                                contentPadding = PaddingValues(20.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimaryAccent.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Timer, null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    val studyText = if (studyHours > 0) "${studyHours}h ${studyMinutes}m" else "${studyMinutes}m"
                                    Text(
                                        text = studyText,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        "FOCUSED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            contentPadding = PaddingValues(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "WEEKLY SUMMARY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = TextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Total focus time",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                val weeklyHours = weeklyStudyTime / 3600
                                val weeklyMinutes = (weeklyStudyTime % 3600) / 60
                                Text(
                                    text = "${weeklyHours}h ${weeklyMinutes}m",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryAccent
                                )
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(4.dp, 16.dp).background(PrimaryAccent, RoundedCornerShape(2.dp)))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "DAILY INSPIRATION",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp,
                                contentPadding = PaddingValues(20.dp)
                            ) {
                                Column {
                                    Icon(
                                        Icons.Default.FormatQuote,
                                        null,
                                        tint = PrimaryAccent.copy(alpha = 0.3f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Success is not final, failure is not fatal: it is the courage to continue that counts.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = Color.White.copy(alpha = 0.9f),
                                        lineHeight = 28.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                                        Text(
                                            "- Winston Churchill",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UsageLegendItem(label: String, time: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun AchievementItem(label: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
