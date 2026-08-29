package com.example.tracker.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.tracker.ui.MainViewModel
import com.example.tracker.ui.components.GlassCard
import com.example.tracker.ui.screens.tasks.TaskViewModel
import com.example.tracker.ui.theme.*
import com.example.tracker.util.GamificationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MainViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val userBio by viewModel.userBio.collectAsState()
    val userGoal by viewModel.userGoal.collectAsState()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()

    val totalStudyTime by taskViewModel.totalStudyTime.collectAsState()
    val todayStudyTime by taskViewModel.todayStudyTime.collectAsState()
    val streakCount by taskViewModel.streakCount.collectAsState()

    val level = GamificationUtils.calculateLevel(totalStudyTime)
    val levelProgress = GamificationUtils.getProgressToNextLevel(totalStudyTime)
    val badges = GamificationUtils.getBadges(totalStudyTime, todayStudyTime, streakCount)

    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(userName) { mutableStateOf(userName ?: "") }
    var editedBio by remember(userBio) { mutableStateOf(userBio ?: "") }
    var editedGoal by remember(userGoal) { mutableStateOf(userGoal ?: "") }

    var showLogoutDialog by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.saveProfilePhotoUri(it.toString()) }
    }

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
        Canvas(modifier = Modifier.fillMaxSize().blur(60.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(animOffset, 100f),
                    radius = 900f
                ),
                radius = 900f,
                center = Offset(animOffset, 100f)
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
                        title = { Text("Profile", fontWeight = FontWeight.Black, color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.syncData(forceRefresh = true) }) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Sync Data",
                                    tint = PrimaryAccent
                                )
                            }
                            IconButton(onClick = { showLogoutDialog = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Logout",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
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
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 32.dp,
                        showAccentGlow = true
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .border(2.dp, PrimaryAccent, CircleShape)
                                    .clickable { photoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (profilePhotoUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(LocalContext.current)
                                                .data(profilePhotoUri)
                                                .crossfade(true)
                                                .build()
                                        ),
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = Color.White.copy(alpha = 0.5f))
                                }
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(24.dp).padding(bottom = 8.dp), tint = Color.White)
                                }
                            }

                            val totalHours = totalStudyTime / 3600
                            val totalMinutes = (totalStudyTime % 3600) / 60

                            // Level & Progress Section
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "Level $level",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Focused for ${totalHours}h ${totalMinutes}m",
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { levelProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = PrimaryAccent,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                                Text(
                                    "Next level in ${(5 - (totalStudyTime / 3600f % 5)).toInt()} hours",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Badges Section
                            Text(
                                "Milestone Badges",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                badges.chunked(2).forEach { rowBadges ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowBadges.forEach { badge ->
                                            BadgeItem(badge = badge, modifier = Modifier.weight(1f))
                                        }
                                        if (rowBadges.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            if (isEditing) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                    val textFieldColors = TextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedIndicatorColor = PrimaryAccent.copy(alpha = 0.5f),
                                        unfocusedTextColor = Color.White,
                                        focusedTextColor = Color.White,
                                        cursorColor = PrimaryAccent
                                    )
                                    TextField(
                                        value = editedName,
                                        onValueChange = { editedName = it },
                                        label = { Text("Name", color = TextSecondary) },
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                        colors = textFieldColors,
                                        singleLine = true
                                    )
                                    TextField(
                                        value = editedBio,
                                        onValueChange = { editedBio = it },
                                        label = { Text("Bio", color = TextSecondary) },
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                        colors = textFieldColors,
                                        maxLines = 3
                                    )
                                    TextField(
                                        value = editedGoal,
                                        onValueChange = { editedGoal = it },
                                        label = { Text("Daily Goal", color = TextSecondary) },
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                        colors = textFieldColors,
                                        singleLine = true
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (userName.isNullOrBlank()) "No Name Set" else userName!!,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = userBio ?: "No bio yet",
                                        fontSize = 16.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)
                                    )
                                    Surface(
                                        color = PrimaryAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.padding(top = 16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Flag, null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(userGoal ?: "Set a goal", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    if (isEditing) {
                                        viewModel.saveUserName(editedName)
                                        viewModel.saveUserBio(editedBio)
                                        viewModel.saveUserGoal(editedGoal)
                                    }
                                    isEditing = !isEditing
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isEditing) SuccessGreen else Color.White.copy(alpha = 0.1f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (isEditing) "Save Changes" else "Edit Profile", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to log out?", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = BackgroundDark,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            )
        }
    }
}

@Composable
fun BadgeItem(badge: com.example.tracker.util.Badge, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (badge.isUnlocked) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (badge.isUnlocked) PrimaryAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (badge.isUnlocked) PrimaryAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    badge.icon,
                    contentDescription = null,
                    tint = if (badge.isUnlocked) PrimaryAccent else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = badge.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (badge.isUnlocked) Color.White else Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (badge.isUnlocked) TextSecondary else TextSecondary.copy(alpha = 0.4f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (badge.progress.toFloat() / badge.target).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(CircleShape),
                        color = if (badge.isUnlocked) PrimaryAccent else Color.White.copy(alpha = 0.2f),
                        trackColor = Color.White.copy(alpha = 0.05f)
                    )
                    Text(
                        text = "${badge.progress}/${badge.target}${badge.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.isUnlocked) PrimaryAccent else Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
