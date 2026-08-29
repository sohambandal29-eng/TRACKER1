package com.example.tracker.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.ui.MainViewModel
import com.example.tracker.ui.components.GlassCard
import com.example.tracker.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToBlocks: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Dynamic Animated Background Blobs
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(animOffset, 200f),
                    radius = 800f
                ),
                radius = 800f,
                center = Offset(animOffset, 200f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(size.width - animOffset, size.height - 400f),
                    radius = 900f
                ),
                radius = 900f,
                center = Offset(size.width - animOffset, size.height - 400f)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { -40 }
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        ) { padding ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200)) { 40 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    ThemeSelector(
                        selectedMode = themeMode,
                        onModeSelected = { viewModel.setThemeMode(it) }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Timer Presets",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    TimerPresetSettings()

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Focus Protection",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    val strictModeEnabled by viewModel.strictMode.collectAsState()

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(PrimaryAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Shield, null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("Strict Mode", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("Prevent timer cancellation", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                                Switch(
                                    checked = strictModeEnabled,
                                    onCheckedChange = { 
                                        viewModel.setStrictMode(it)
                                        viewModel.syncData()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = PrimaryAccent,
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToBlocks() },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(PrimaryAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Block, null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("App Blocks", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("Manage restricted applications", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))


                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Version", color = Color.White.copy(alpha = 0.6f))
                                Text("1.0.0", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Developer", color = Color.White.copy(alpha = 0.6f))
                                Text(" Mr.Soham Bandal", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerPresetSettings() {
    val context = LocalContext.current
    val pomodoroMin = remember { mutableLongStateOf(com.example.tracker.ui.screens.timer.TimerManager.getDurationForMode(com.example.tracker.ui.screens.timer.TimerMode.POMODORO) / 60) }
    val shortBreakMin = remember { mutableLongStateOf(com.example.tracker.ui.screens.timer.TimerManager.getDurationForMode(com.example.tracker.ui.screens.timer.TimerMode.SHORT_BREAK) / 60) }
    val longBreakMin = remember { mutableLongStateOf(com.example.tracker.ui.screens.timer.TimerManager.getDurationForMode(com.example.tracker.ui.screens.timer.TimerMode.LONG_BREAK) / 60) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TimerSettingItem("Timer", pomodoroMin) { com.example.tracker.ui.screens.timer.TimerManager.setCustomDuration(com.example.tracker.ui.screens.timer.TimerMode.POMODORO, it) }
            TimerSettingItem("Short Break", shortBreakMin) { com.example.tracker.ui.screens.timer.TimerManager.setCustomDuration(com.example.tracker.ui.screens.timer.TimerMode.SHORT_BREAK, it) }
            TimerSettingItem("Long Break", longBreakMin) { com.example.tracker.ui.screens.timer.TimerManager.setCustomDuration(com.example.tracker.ui.screens.timer.TimerMode.LONG_BREAK, it) }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            
            TextButton(
                onClick = {
                    val user = FirebaseAuth.getInstance().currentUser
                    val userId = user?.uid ?: "unknown_user"
                    val bodyText = """
                            CUSTOM PRESET REQUEST
                            -----------------------
                            User ID: $userId
                            
                            Requested Preset Type: [e.g. Ultra Focus]
                            Requested Duration: [e.g. 90 minutes]
                            
                            Reason for request:
                            
                            
                            -----------------------
                        """.trimIndent()

                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:trackerteamsupport@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "PRESET REQUEST")
                        putExtra(Intent.EXTRA_TEXT, bodyText)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsScreen", "No email client found", e)
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Mail, null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Request custom preset", color = PrimaryAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TimerSettingItem(label: String, state: MutableState<Long>, onSave: (Long) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.8f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { 
                if (state.value > 1) {
                    state.value--
                    onSave(state.value)
                }
            }) {
                Text("-", color = PrimaryAccent, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Text(
                "${state.value}m",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { 
                state.value++
                onSave(state.value)
            }) {
                Text("+", color = PrimaryAccent, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun ThemeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    val radioOptions = listOf("light", "dark", "system")

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            radioOptions.forEach { text ->
                val isSelected = (text == selectedMode)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = isSelected,
                            onClick = { onModeSelected(text) },
                            role = Role.RadioButton
                        )
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = PrimaryAccent,
                            unselectedColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    Text(
                        text = text.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

