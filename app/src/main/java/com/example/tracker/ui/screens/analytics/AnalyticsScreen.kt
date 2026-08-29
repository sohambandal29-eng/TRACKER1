package com.example.tracker.ui.screens.analytics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.data.local.dao.DailyTaskStatus
import com.example.tracker.data.local.dao.DailyTime
import com.example.tracker.ui.components.GlassCard
import com.example.tracker.ui.screens.tasks.TaskViewModel
import com.example.tracker.ui.theme.*
import java.time.*
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: TaskViewModel = viewModel(),
    productivityViewModel: ProductivityViewModel = viewModel()
) {
    val weeklyTime by viewModel.weeklyStudyTime.collectAsState()
    val lastWeeklyTime by viewModel.lastWeeklyStudyTime.collectAsState()
    val streak by viewModel.streakCount.collectAsState()
    val categoryDist by viewModel.categoryDistribution.collectAsState()
    val dailyTimes by viewModel.weeklyDailyTime.collectAsState()
    val monthlyTime by viewModel.monthlyStudyTime.collectAsState()
    val mostStudiedDay by viewModel.mostStudiedDay.collectAsState()
    val yearlyDailyTime by viewModel.yearlyDailyTime.collectAsState()
    val dailyTaskStats by viewModel.dailyTaskStats.collectAsState()
    val peakHour by productivityViewModel.peakHour.collectAsState(initial = null)
    val hourlyData by productivityViewModel.hourlyProductivity.collectAsState(initial = emptyMap())

    val focusData = (0..23).map { hour -> hourlyData[hour]?.toFloat() ?: 0f }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val hours = weeklyTime / 3600
    val minutes = (weeklyTime % 3600) / 60

    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Animated Background
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(animOffset, 200f),
                    radius = 900f
                ),
                radius = 900f,
                center = Offset(animOffset, 200f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(size.width - animOffset, size.height - 300f),
                    radius = 1100f
                ),
                radius = 1100f,
                center = Offset(size.width - animOffset, size.height - 300f)
            )
        }

        Scaffold(
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
                                "Study Analytics",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 300)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(1000, delayMillis = 300)
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Text(
                            text = "Focus Metrics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val diff = if (lastWeeklyTime > 0) {
                                ((weeklyTime - lastWeeklyTime).toFloat() / lastWeeklyTime * 100).toInt()
                            } else 0
                            
                            val trendText = if (diff >= 0) "+$diff%" else "$diff%"
                            val trendColor = if (diff >= 0) SuccessGreen else Color.Red

                            StatsCard(
                                modifier = Modifier.weight(1f),
                                title = "Focus Time",
                                value = "${hours}h ${minutes}m",
                                icon = Icons.Default.History,
                                trend = trendText,
                                trendColor = trendColor
                            )
                            StatsCard(
                                modifier = Modifier.weight(1f),
                                title = "Streak",
                                value = "$streak Days",
                                icon = Icons.AutoMirrored.Filled.TrendingUp
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val monthlyHours = monthlyTime / 3600
                            val monthlyMinutes = (monthlyTime % 3600) / 60

                            StatsCard(
                                modifier = Modifier.weight(1f),
                                title = "This Month",
                                value = "${monthlyHours}h ${monthlyMinutes}m",
                                icon = Icons.Default.CalendarMonth
                            )
                            StatsCard(
                                modifier = Modifier.weight(1f),
                                title = "Best Day",
                                value = mostStudiedDay?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: "N/A",
                                icon = Icons.Default.Star
                            )
                        }
                    }

                    val peakHourValue = peakHour
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), showAccentGlow = true) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryAccent.copy(alpha = 0.15f))
                                            .border(1.dp, PrimaryAccent.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = PrimaryAccent,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            "Today's Focus Pattern",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        val hourText = if (peakHourValue == null) "No data yet" 
                                            else if (peakHourValue < 12) "$peakHourValue AM" 
                                            else if (peakHourValue == 12) "12 PM" 
                                            else "${peakHourValue - 12} PM"
                                        
                                        Text(
                                            if (peakHourValue == null) "Start a session to see your pattern" 
                                            else "Peak productivity at $hourText",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Hourly Focus Graph
                                DailyFocusGraph(
                                    data = focusData,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    val hoursToShow = (0..23 step 2).toList()
                                    hoursToShow.forEach { hour ->
                                        val label = when {
                                            hour == 0 -> "12a"
                                            hour == 12 -> "12p"
                                            hour < 12 -> "${hour}a"
                                            else -> "${hour - 12}p"
                                        }
                                        val fraction = hour / 23f
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = if (hour % 4 == 0) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (hour % 4 == 0) TextSecondary else TextSecondary.copy(alpha = 0.5f),
                                            modifier = Modifier.align(
                                                androidx.compose.ui.BiasAlignment(2 * fraction - 1, 0f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Weekly Activity",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val maxTime = (dailyTimes.maxOfOrNull { it.totalTime } ?: 0L).coerceAtLeast(3600L * 2)

                                val startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                                val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    weekDays.forEach { date ->
                                        val daily = dailyTimes.find {
                                            Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == date
                                        }
                                        val totalTime = daily?.totalTime ?: 0L
                                        val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                        val progress = totalTime.toFloat() / maxTime
                                        val isToday = date == LocalDate.now()

                                        val animatedBarHeight by animateFloatAsState(
                                            targetValue = progress.coerceAtLeast(0.01f),
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                            label = "BarHeight"
                                        )

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxHeight().weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth(),
                                                contentAlignment = Alignment.BottomCenter
                                            ) {
                                                // Bar Background
                                                Box(
                                                    modifier = Modifier
                                                        .width(10.dp)
                                                        .fillMaxHeight()
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.05f))
                                                )
                                                // Progress Bar
                                                Box(
                                                    modifier = Modifier
                                                        .width(10.dp)
                                                        .fillMaxHeight(animatedBarHeight)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isToday) PrimaryAccent
                                                            else PrimaryAccent.copy(alpha = 0.3f)
                                                        )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = dayLabel.first().toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isToday) FontWeight.Black else FontWeight.Medium,
                                                color = if (isToday) PrimaryAccent else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Yearly Consistency",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassCard(cornerRadius = 24.dp) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(SuccessGreen)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "STRICT 3-HOUR DAILY GOAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(16.dp))
                                YearlyConsistencyGrid(yearlyDailyTime, dailyTaskStats)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Subject Distribution",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassCard(cornerRadius = 24.dp) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val colors = listOf(
                                    PrimaryAccent, SecondaryCyan, AccentPurple,
                                    Color(0xFFFF5252), SuccessGreen, WarningOrange
                                )

                                if (categoryDist.isEmpty()) {
                                    Text(
                                        "No data available yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(200.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(150.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val total = categoryDist.sumOf { it.totalTime }.toFloat()
                                                var startAngle = -90f
                                                categoryDist.forEachIndexed { index, cat ->
                                                    val sweepAngle = (cat.totalTime.toFloat() / total) * 360f
                                                    drawArc(
                                                        color = if (index == 0) PrimaryAccent else colors[index % colors.size],
                                                        startAngle = startAngle,
                                                        sweepAngle = sweepAngle,
                                                        useCenter = false,
                                                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                                    )
                                                    startAngle += sweepAngle
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    "${categoryDist.size}",
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                                Text(
                                                    "Topics",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        
                                        Column(
                                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            categoryDist.take(4).forEachIndexed { index, cat ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(if (index == 0) PrimaryAccent else colors[index % colors.size])
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        cat.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))

                                    categoryDist.forEachIndexed { index, cat ->
                                        val progress = cat.totalTime.toFloat() / (cat.targetTime.coerceAtLeast(1L)).toFloat()
                                        DistributionRow(
                                            label = cat.category,
                                            percentage = progress,
                                            color = if (index == 0) PrimaryAccent else colors[index % colors.size],
                                            displayPercentage = "${(cat.totalTime / 60)} / ${(cat.targetTime / 60)} min"
                                        )
                                        if (index < categoryDist.size - 1) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ConsistencyLegend() {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendItem("100%", SuccessGreen)
                LegendItem("75%+", SuccessGreen.copy(alpha = 0.35f))
                LegendItem("50%+", ErrorRed.copy(alpha = 0.35f))
                LegendItem("<50%", ErrorRed)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
                Text(" 3h Goal", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(" $label", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
    }
}

@Composable
fun YearlyConsistencyGrid(
    yearlyData: List<DailyTime>,
    dailyTaskStats: List<DailyTaskStatus>
) {
    val scrollState = rememberScrollState()
    val currentYear = remember { LocalDate.now().year }
    val months = remember { (1..12).map { YearMonth.of(currentYear, it) } }
    val dayRange = 1..31
    
    var selectedDateInfo by remember { mutableStateOf<String?>(null) }

    Column {
        // Selected Date Details
        AnimatedVisibility(
            visible = selectedDateInfo != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selectedDateInfo?.let {
                Surface(
                    color = PrimaryAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, PrimaryAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = "Daily Details", tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Close details", 
                            tint = TextSecondary, 
                            modifier = Modifier.size(16.dp).clickable(onClickLabel = "Close details") { selectedDateInfo = null }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column {
                // Month Headers
                Row(modifier = Modifier.padding(bottom = 8.dp)) {
                    Spacer(modifier = Modifier.width(30.dp))
                    months.forEach { month ->
                        Text(
                            text = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase().take(1),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = TextSecondary,
                            modifier = Modifier.width(26.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 9.sp
                        )
                    }
                }

                dayRange.forEach { day ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = day.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.width(30.dp).padding(end = 6.dp),
                            textAlign = TextAlign.End,
                            fontSize = 9.sp
                        )
                        months.forEach { month ->
                            val date = try { month.atDay(day) } catch (_: Exception) { null }
                            val isToday = date == LocalDate.now()
                            
                            val studyTime = date?.let { d ->
                                yearlyData.find { 
                                    Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == d
                                }?.totalTime ?: 0L
                            } ?: 0L

                            val stats = date?.let { d ->
                                val epochDayToCheck = d.toEpochDay()
                                dailyTaskStats.find { it.day == epochDayToCheck }
                            }

                            val totalTasks = stats?.totalTasks ?: 0
                            val completedTasks = stats?.completedTasks ?: 0
                            val timeGoalMet = studyTime >= 10800L // 3 hours in seconds

                            val taskProgress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else -1f
                            
                            val color = when {
                                date == null -> Color.Transparent
                                !timeGoalMet && taskProgress == -1f -> Color.White.copy(alpha = 0.05f) // Goal not met, no tasks
                                !timeGoalMet -> ErrorRed.copy(alpha = 0.35f) // Goal not met, tasks exist
                                taskProgress == -1f -> PrimaryAccent.copy(alpha = 0.2f) // Goal met, no tasks
                                taskProgress >= 1f -> SuccessGreen
                                taskProgress >= 0.75f -> SuccessGreen.copy(alpha = 0.35f)
                                taskProgress >= 0.5f -> ErrorRed.copy(alpha = 0.35f)
                                else -> ErrorRed
                            }

                            Box(
                                modifier = Modifier
                                    .padding(1.dp)
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                                    .then(
                                        if (isToday) Modifier.border(1.dp, Color.White, RoundedCornerShape(4.dp))
                                        else Modifier
                                    )
                                    .clickable(
                                        enabled = date != null,
                                        onClickLabel = "View details for ${date?.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"))}"
                                    ) {
                                        val h = studyTime / 3600
                                        val m = (studyTime % 3600) / 60
                                        val timeStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                                        val taskStr = if (totalTasks > 0) "$completedTasks/$totalTasks tasks" else "No tasks assigned"
                                        selectedDateInfo = "${date?.format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd"))}: $timeStr • $taskStr"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (timeGoalMet) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Daily 3h goal met",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        ConsistencyLegend()
    }
}

@Composable
fun StatsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    trend: String? = null,
    trendColor: Color = SuccessGreen
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryAccent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                }
                if (trend != null) {
                    Text(
                        text = trend,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun DailyFocusGraph(
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1)
        val maxVal = (data.maxOfOrNull { it } ?: 0f).coerceAtLeast(1f)

        val points = data.mapIndexed { index, value ->
            Offset(index * spacing, height - (value / maxVal) * height)
        }

        // Draw background area
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(width, height)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(PrimaryAccent.copy(alpha = 0.3f), Color.Transparent)
            )
        )

        // Draw line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = PrimaryAccent,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        // Draw points
        points.forEachIndexed { index, point ->
            if (data[index] > 0) {
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
fun DistributionRow(
    label: String,
    percentage: Float,
    color: Color,
    displayPercentage: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                displayPercentage,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
