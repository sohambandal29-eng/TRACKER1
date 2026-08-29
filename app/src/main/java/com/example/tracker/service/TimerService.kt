package com.example.tracker.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.tracker.MainActivity
import com.example.tracker.data.local.UserPreferences
import com.example.tracker.utils.FirebaseAuthManager
import com.example.tracker.ui.screens.timer.TimerManager
import com.example.tracker.ui.screens.timer.TimerMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull

class TimerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null
    private var blockerJob: Job? = null
    private var databaseJob: Job? = null
    private var strictModeJob: Job? = null

    private var dynamicBlockedPackages = setOf<String>()
    private var isStrictModeEnabled = false
    private var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userId: String? = null

    private val userPreferences by lazy { UserPreferences(applicationContext) }

    companion object {
        const val CHANNEL_ID = "focus_timer_channel"
        const val CHANNEL_NAME = "Focus Timer"
        const val ALERT_CHANNEL_ID = "timer_alert_channel"
        const val ALERT_CHANNEL_NAME = "Timer Alerts"
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002

        const val ACTION_START = "com.example.tracker.ACTION_START"
        const val ACTION_PAUSE = "com.example.tracker.ACTION_PAUSE"
        const val ACTION_RESET = "com.example.tracker.ACTION_RESET"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeUserAndData()
        observeStrictMode()
        startAppBlocker() // Start the blocker thread here so it's always running
    }

    private fun observeUserAndData() {
        serviceScope.launch {
            FirebaseAuthManager.userIdFlow.collectLatest { currentUid ->
                userId = currentUid
                if (currentUid != null) {
                    // Restart data observations for the new user
                    startFirestoreSync(currentUid)
                    observeBlockedApps(currentUid)
                } else {
                    // Stop everything if logged out
                    firestoreListener?.remove()
                    dynamicBlockedPackages = emptySet()
                    databaseJob?.cancel()
                }
            }
        }
    }

    private fun startFirestoreSync(uid: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val localDb = com.example.tracker.data.local.AppDatabase.getDatabase(applicationContext)

        firestoreListener?.remove()
        firestoreListener = db.collection("users").document(uid).collection("blocked_apps")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                
                // Important: Don't process mirror deletions from cached data to prevent accidental wipes
                if (snapshot.metadata.isFromCache) return@addSnapshotListener

                serviceScope.launch(Dispatchers.IO) {
                    val remoteApps = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(com.example.tracker.data.local.entities.BlockedAppEntity::class.java)
                            ?.copy(packageName = doc.id, userId = uid)
                    }
                    val remotePackageNames = remoteApps.map { it.packageName }.toSet()
                    
                    // Sync local database with remote state
                    val localApps = localDb.blockedAppDao().getAllBlockedApps(uid).first()
                    localApps.forEach { local ->
                        if (local.packageName !in remotePackageNames && local.isSynced) {
                            localDb.blockedAppDao().delete(local)
                        }
                    }
                    remoteApps.forEach { remote ->
                        localDb.blockedAppDao().insert(remote.copy(isSynced = true))
                    }
                }
            }
    }

    private fun observeStrictMode() {
        strictModeJob?.cancel()
        strictModeJob = serviceScope.launch {
            TimerManager.isStrictMode.collect { enabled ->
                isStrictModeEnabled = enabled
            }
        }
    }

    private fun observeBlockedApps(uid: String) {
        databaseJob?.cancel()
        databaseJob = serviceScope.launch {
            val db = com.example.tracker.data.local.AppDatabase.getDatabase(applicationContext)
            db.blockedAppDao().getAllBlockedApps(uid).collect { apps ->
                dynamicBlockedPackages = apps.map { it.packageName }.toSet()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> TimerManager.startTimer(this)
            ACTION_PAUSE -> TimerManager.pauseTimer(this)
            ACTION_RESET -> {
                TimerManager.resetTimer(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startForegroundNotification()
        observeTimerState()
        startAppBlocker()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active study session countdown"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a timer finishes"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    private fun startForegroundNotification() {
        val notification = buildNotification(
            timeLeft = TimerManager.timeLeft.value,
            isRunning = TimerManager.isRunning.value,
            taskTitle = TimerManager.selectedTask.value?.title,
            mode = TimerManager.timerMode.value
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun hasPermissions(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED && Settings.canDrawOverlays(this)
    }

    private fun startAppBlocker() {
        if (blockerJob?.isActive == true) return
        
        blockerJob = serviceScope.launch(Dispatchers.Default) {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            while (isActive) {
                // The blocker should work if the timer is running OR if strict mode is enabled
                val isTimerRunning = TimerManager.isRunning.value
                val isPomodoro = TimerManager.timerMode.value == TimerMode.POMODORO
                val allBlocked = dynamicBlockedPackages
                
                // Automatically block during ANY running Pomodoro session
                // OR if the user has manually enabled Strict Mode (prevents distraction even if timer is paused/stopped)
                val shouldBlockNow = (isTimerRunning && isPomodoro) || isStrictModeEnabled

                if (shouldBlockNow && hasPermissions()) {
                    val time = System.currentTimeMillis()
                    var currentApp: String? = null
                    
                    // 1. Check Usage Events
                    val events = usageStatsManager.queryEvents(time - 5000, time)
                    val event = UsageEvents.Event()
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == 1) {
                            currentApp = event.packageName
                        }
                    }

                    // 2. Fallback to queryUsageStats
                    if (currentApp == null) {
                        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 2000, time)
                        if (!stats.isNullOrEmpty()) {
                            currentApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName
                        }
                    }

                    // BLOCKING LOGIC:
                    // We ONLY block apps that are explicitly in the user's blocked list.
                    // Once an app is in this list, it is blocked "Forever" (Lock-in mechanism)
                    // unless unblocked via the support request flow.
                    val isBlocked = allBlocked.contains(currentApp) && currentApp != null && currentApp != packageName

                    if (isBlocked) {
                        android.util.Log.d("TimerService", "Blocking restricted app: $currentApp")
                        withContext(Dispatchers.Main) {
                            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra("blocked_app_attempt", true)
                                putExtra("blocked_package_name", currentApp)
                            }
                            startActivity(intent)
                        }
                        delay(1200)
                    }
                }
                delay(400)
            }
        }
    }

    private fun observeTimerState() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            launch {
                TimerManager.timerFinished.collect {
                    sendAlertNotification()
                }
            }
            
            combine(
                TimerManager.timeLeft,
                TimerManager.isRunning,
                TimerManager.selectedTask,
                TimerManager.timerMode
            ) { timeLeft, isRunning, selectedTask, mode ->
                NotificationData(timeLeft, isRunning, selectedTask?.title, mode)
            }.collect { data ->
                val notification = buildNotification(
                    timeLeft = data.timeLeft,
                    isRunning = data.isRunning,
                    taskTitle = data.taskTitle,
                    mode = data.mode
                )
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun sendAlertNotification() {
        val mode = TimerManager.timerMode.value
        val title = when (mode) {
            TimerMode.POMODORO -> "Session Finished!"
            else -> "Break Ended!"
        }
        val text = when (mode) {
            TimerMode.POMODORO -> "Time to take a break."
            else -> "Ready to focus again?"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 10, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID, builder.build())
    }

    private data class NotificationData(
        val timeLeft: Long,
        val isRunning: Boolean,
        val taskTitle: String?,
        val mode: TimerMode
    )

    private fun buildNotification(
        timeLeft: Long,
        isRunning: Boolean,
        taskTitle: String?,
        mode: TimerMode
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val startIntent = Intent(this, TimerService::class.java).apply { action = ACTION_START }
        val startPendingIntent = PendingIntent.getService(
            this, 2, startIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val resetIntent = Intent(this, TimerService::class.java).apply { action = ACTION_RESET }
        val resetPendingIntent = PendingIntent.getService(
            this, 3, resetIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val modeText = when (mode) {
            TimerMode.POMODORO -> "Study Session"
            TimerMode.SHORT_BREAK -> "Short Break"
            TimerMode.LONG_BREAK -> "Long Break"
        }

        val title = if (taskTitle != null && mode == TimerMode.POMODORO) {
            "$modeText: $taskTitle"
        } else {
            modeText
        }

        val minutes = timeLeft / 60
        val seconds = timeLeft % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)
        val contentText = if (isRunning) "Remaining: $timeStr" else "Paused: $timeStr"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (isRunning) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        } else {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", startPendingIntent)
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reset", resetPendingIntent)

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove()
        updateJob?.cancel()
        blockerJob?.cancel()
        databaseJob?.cancel()
        strictModeJob?.cancel()
        serviceScope.cancel()
    }
}
