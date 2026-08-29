package com.example.tracker.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.data.local.UserPreferences
import com.example.tracker.data.local.entities.BlockedAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.tracker.utils.FirebaseAuthManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferences = UserPreferences(application)
    private val auth = Firebase.auth
    private val blockedAppDao = AppDatabase.getDatabase(application).blockedAppDao()
    
    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private var unblockRequestsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var profileListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val loggedIn = firebaseAuth.currentUser != null
            _isLoggedIn.value = loggedIn
            if (loggedIn) {
                observeUserProfile()
                fetchPendingRequests()
                syncData(forceRefresh = true)
            } else {
                clearUserState()
            }
        }
        loadInstalledApps()
    }

    private fun observeUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        profileListener?.remove()
        profileListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    viewModelScope.launch {
                        snapshot.getString("name")?.let { userPreferences.saveUserName(it) }
                        snapshot.getString("bio")?.let { userPreferences.saveUserBio(it) }
                        snapshot.getString("goal")?.let { userPreferences.saveUserGoal(it) }
                        userPreferences.saveIsAdmin(snapshot.getBoolean("isAdmin") ?: false)
                    }
                }
            }
    }

    private fun clearUserState() {
        unblockRequestsListener?.remove()
        unblockRequestsListener = null
        profileListener?.remove()
        profileListener = null
        _pendingUnblockRequests.value = emptySet()
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.clearAll()
            AppDatabase.getDatabase(getApplication()).clearAllTables()
        }
    }

    private fun fetchPendingRequests() {
        val userId = auth.currentUser?.uid ?: return
        unblockRequestsListener?.remove()
        unblockRequestsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("unblock_requests")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                val pendingPackages = snapshot?.documents?.mapNotNull { 
                    it.getString("packageName") 
                }?.toSet() ?: emptySet()
                _pendingUnblockRequests.value = pendingPackages
            }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                // Get all installed packages
                val packages = pm.getInstalledPackages(0)
                
                packages.mapNotNull { pkg ->
                    // Skip our own app
                    if (pkg.packageName == getApplication<Application>().packageName) return@mapNotNull null
                    
                    val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                    
                    // Check if the app has a launcher intent (meaning it's a user-facing app)
                    val launchIntent = pm.getLaunchIntentForPackage(pkg.packageName)
                    if (launchIntent != null) {
                        AppInfo(
                            name = appInfo.loadLabel(pm).toString(),
                            packageName = pkg.packageName
                        )
                    } else null
                }.sortedBy { it.name }
            }
            android.util.Log.d("MainViewModel", "Found ${apps.size} installed apps")
            _installedApps.value = apps
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val blockedApps: StateFlow<List<BlockedAppEntity>> = FirebaseAuthManager.userIdFlow.flatMapLatest { userId ->
        if (userId != null) {
            blockedAppDao.getAllBlockedApps(userId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val userName: StateFlow<String?> = userPreferences.userName.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val userBio: StateFlow<String?> = userPreferences.userBio.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val userGoal: StateFlow<String?> = userPreferences.userGoal.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val profilePhotoUri: StateFlow<String?> = userPreferences.profilePhotoUri.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val _pendingUnblockRequests = MutableStateFlow<Set<String>>(emptySet())
    val pendingUnblockRequests: StateFlow<Set<String>> = _pendingUnblockRequests.asStateFlow()

    val themeMode: StateFlow<String> = userPreferences.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "system"
    )

    val isAdmin: StateFlow<Boolean> = userPreferences.isAdmin.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val strictMode: StateFlow<Boolean> = userPreferences.strictMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val isTimerStrictActive: StateFlow<Boolean> = combine(
        com.example.tracker.ui.screens.timer.TimerManager.isRunning,
        com.example.tracker.ui.screens.timer.TimerManager.timerMode
    ) { running, mode ->
        running && mode == com.example.tracker.ui.screens.timer.TimerMode.POMODORO
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun saveUserName(name: String) {
        viewModelScope.launch {
            userPreferences.saveUserName(name)
            updateProfileField("name", name)
        }
    }

    fun saveUserBio(bio: String) {
        viewModelScope.launch {
            userPreferences.saveUserBio(bio)
            updateProfileField("bio", bio)
        }
    }

    fun saveUserGoal(goal: String) {
        viewModelScope.launch {
            userPreferences.saveUserGoal(goal)
            updateProfileField("goal", goal)
        }
    }

    private fun updateProfileField(field: String, value: String) {
        val userId = auth.currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .set(mapOf(field to value), com.google.firebase.firestore.SetOptions.merge())
    }

    fun saveProfilePhotoUri(uriString: String) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val file = File(context.filesDir, "profile_photo.jpg")
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    userPreferences.saveProfilePhotoUri(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferences.saveThemeMode(mode)
        }
    }

    fun setStrictMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.saveStrictMode(enabled)
        }
    }

    fun syncData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                com.example.tracker.data.sync.SyncManager(getApplication()).syncAll(forceRefresh)
                android.util.Log.d("SyncManager", "Manual forced sync triggered for UI update")
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "Manual sync failed", e)
                e.printStackTrace()
            }
        }
    }

    fun addBlockedApp(packageName: String, appName: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            blockedAppDao.insert(
                BlockedAppEntity(
                    packageName = packageName,
                    userId = userId,
                    appName = appName
                )
            )
        }
    }
}

data class AppInfo(val name: String, val packageName: String)
