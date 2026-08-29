package com.example.tracker

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.example.tracker.ui.MainViewModel
import com.example.tracker.ui.components.GlassCard
import com.example.tracker.ui.navigation.Screen
import com.example.tracker.ui.screens.admin.AdminScreen
import com.example.tracker.ui.screens.analytics.AnalyticsScreen
import com.example.tracker.ui.screens.auth.AuthViewModel
import com.example.tracker.ui.screens.auth.LoginScreen
import com.example.tracker.ui.screens.auth.RegisterScreen
import com.example.tracker.ui.screens.blocks.BlocksScreen
import com.example.tracker.ui.screens.dashboard.DashboardScreen
import com.example.tracker.ui.screens.profile.ProfileScreen
import com.example.tracker.ui.screens.roadmap.RoadmapScreen
import com.example.tracker.ui.screens.settings.SettingsScreen
import com.example.tracker.ui.screens.tasks.TasksScreen
import com.example.tracker.ui.screens.timer.TimerScreen
import com.example.tracker.ui.screens.timetable.TimetableScreen
import com.example.tracker.ui.theme.*
import com.example.tracker.utils.NotificationHelper
import com.example.tracker.worker.ReminderWorker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationHelper(this).createNotificationChannel()
        scheduleReminders()
        
        enableEdgeToEdge()
        setContent {
            val themeMode by mainViewModel.themeMode.collectAsState()
            TrackerTheme(themeMode = themeMode) {
                var showSplash by remember { mutableStateOf(true) }
                
                if (showSplash) {
                    SplashScreen(onFinish = { showSplash = false })
                } else {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val lifecycleOwner = LocalLifecycleOwner.current

                    var hasUsageAccess by remember { mutableStateOf(true) }
                    var hasOverlayAccess by remember { mutableStateOf(true) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    fun checkPermissions() {
                        hasUsageAccess = hasUsageStatsPermission(context)
                        hasOverlayAccess = Settings.canDrawOverlays(context)
                    }

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                checkPermissions()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    LaunchedEffect(Unit) {
                        checkPermissions()
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    // Handle redirection from TimerService when an app is blocked
                    LaunchedEffect(Unit) {
                        val activity = context as? android.app.Activity
                        val intent = activity?.intent
                        if (intent?.getBooleanExtra("blocked_app_attempt", false) == true) {
                            navController.navigate(Screen.Timer.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            // Clear intent so it doesn't trigger again on configuration change
                            activity.intent.removeExtra("blocked_app_attempt")
                        }
                    }

                    if (!hasUsageAccess) {
                        UsagePermissionDialog(
                            onGrant = {
                                try {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                            }
                        )
                    } else if (!hasOverlayAccess) {
                        OverlayPermissionDialog(
                            onGrant = {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                                }
                            }
                        )
                    } else {
                        MainScreen(mainViewModel, navController)
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun scheduleReminders() {
        val workManager = WorkManager.getInstance(this)
        
        scheduleDailyWork(workManager, "MORNING", 8, 0)
        scheduleDailyWork(workManager, "NIGHT", 22, 0)
        
        val intervalWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS)
            .setInputData(workDataOf("type" to "INTERVAL"))
            .build()
        workManager.enqueueUniquePeriodicWork(
            "Reminder_Interval",
            ExistingPeriodicWorkPolicy.KEEP,
            intervalWorkRequest
        )
    }

    private fun scheduleDailyWork(workManager: WorkManager, type: String, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val delayMillis = calendar.timeInMillis - now
        
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("type" to type))
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            "Reminder_$type",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

@Composable
fun UsagePermissionDialog(onGrant: () -> Unit) {
    Dialog(onDismissRequest = { /* Mandatory */ }) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            cornerRadius = 24.dp,
            showAccentGlow = true
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(PrimaryAccent.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, null, tint = PrimaryAccent, modifier = Modifier.size(32.dp))
                }
                
                Text(
                    "Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                
                Text(
                    "To detect distracting apps like Instagram and Snapchat during focus sessions, Tracker needs Usage Access permission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Button(
                    onClick = onGrant,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun OverlayPermissionDialog(onGrant: () -> Unit) {
    Dialog(onDismissRequest = { /* Mandatory */ }) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            cornerRadius = 24.dp,
            showAccentGlow = true
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(PrimaryAccent.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Layers, null, tint = PrimaryAccent, modifier = Modifier.size(32.dp))
                }
                
                Text(
                    "Overlay Permission",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                
                Text(
                    "To prevent you from opening blocked apps, Tracker needs permission to display over other apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Button(
                    onClick = onGrant,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(2000)
        visible = false
        delay(500)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.2f),
                    radius = 600f
                ),
                radius = 600f,
                center = Offset(size.width * 0.2f, size.height * 0.2f)
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1200)) + scaleIn(tween(1200, easing = LinearEasing)),
            exit = fadeOut(tween(600)) + scaleOut(tween(600))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GlassCard(
                    modifier = Modifier.size(180.dp),
                    showAccentGlow = true,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            "T",
                            fontSize = 100.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    "TRACKER",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 16.sp,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }
    }
}

@Composable
fun MainScreen(mainViewModel: MainViewModel, navController: androidx.navigation.NavHostController) {
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    
    // Auto-restore data from cloud if logged in
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            mainViewModel.syncData()
        }
    }

    val items = listOf(
        BottomNavItem("Focus", Screen.Dashboard.route, Icons.Default.Dashboard),
        BottomNavItem("Planner", Screen.Tasks.route, Icons.Default.Event),
        BottomNavItem("Timetable", Screen.Timetable.route, Icons.Default.CalendarToday),
        BottomNavItem("Timer", Screen.Timer.route, Icons.Default.Timer),
        BottomNavItem("Analytics", Screen.Analytics.route, Icons.Default.Analytics),
        BottomNavItem("Roadmap", Screen.Roadmap.route, Icons.Default.Map),
        BottomNavItem("Settings", Screen.Settings.route, Icons.Default.Settings),
    )

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != Screen.Login.route && currentRoute != Screen.Register.route && currentRoute != Screen.Admin.route) {
                PremiumBottomNavigation(navController, items)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onAdminLogin = {
                        navController.navigate(Screen.Admin.route)
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }
            composable(Screen.Profile.route) {
                val authViewModel: AuthViewModel = viewModel()
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Tasks.route) {
                TasksScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Timer.route) {
                TimerScreen()
            }
            composable(Screen.Timetable.route) {
                TimetableScreen()
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            composable(Screen.Roadmap.route) {
                RoadmapScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToBlocks = { navController.navigate(Screen.Blocks.route) },
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0)
                        }
                    }
                )
            }
            composable(Screen.Blocks.route) {
                BlocksScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Admin.route) {
                val isAdmin by mainViewModel.isAdmin.collectAsState()
                if (isAdmin) {
                    AdminScreen(onBack = { navController.popBackStack() })
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumBottomNavigation(navController: androidx.navigation.NavHostController, items: List<BottomNavItem>) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .navigationBarsPadding()
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 32.dp,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    val iconColor by animateColorAsState(targetValue = if (selected) PrimaryAccent else Color.White.copy(alpha = 0.4f))
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            .padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.name,
                            modifier = Modifier.size(24.dp),
                            tint = iconColor
                        )
                    }
                }
            }
        }
    }
}

data class BottomNavItem(val name: String, val route: String, val icon: ImageVector)
