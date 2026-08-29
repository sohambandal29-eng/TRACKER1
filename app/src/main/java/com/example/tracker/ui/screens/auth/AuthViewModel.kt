package com.example.tracker.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tracker.data.local.AppDatabase
import com.example.tracker.data.local.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.example.tracker.data.sync.SyncManager
import com.example.tracker.ui.screens.timer.TimerManager
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val database = AppDatabase.getDatabase(application)
    private val userPreferences = UserPreferences(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val currentUser = auth.currentUser

    fun login(email: String, password: String) {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await()
                
                // Clear and Restore
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                    userPreferences.clearAll()
                    TimerManager.clearAll(getApplication())
                    WorkManager.getInstance(getApplication()).cancelAllWorkByTag("timetable_notification")
                    // Delete local profile photo file to prevent leakage between accounts
                    val file = java.io.File(getApplication<Application>().filesDir, "profile_photo.jpg")
                    if (file.exists()) file.delete()
                    
                    SyncManager(getApplication()).downloadRemoteChanges(auth.currentUser?.uid ?: "")
                }
                
                val snapshot = db.collection("users").document(auth.currentUser?.uid ?: "").get().await()
                userPreferences.saveUserName(snapshot.getString("name") ?: "")
                userPreferences.saveUserBio(snapshot.getString("bio") ?: "")
                userPreferences.saveUserGoal(snapshot.getString("goal") ?: "")
                userPreferences.saveIsAdmin(snapshot.getBoolean("isAdmin") ?: false)
                
                // Note: profilePhotoUri restoration usually requires a URL or downloading the file if stored in Firebase Storage.
                // If it's just a local path, it won't work across devices unless synced.
                snapshot.getString("profilePhotoUrl")?.let { url ->
                    // For now, if there's a URL, we'd need to download it. 
                    // But if the user just wants it cleared on logout, clearing in signOut is the main fix.
                }

                com.example.tracker.data.repository.SyncRepository(getApplication()).scheduleSync()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Login failed")
            }
        }
    }

    fun signUp(name: String, email: String, password: String) {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Clear local database and preferences before signup
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                    userPreferences.clearAll()
                    TimerManager.clearAll(getApplication())
                    WorkManager.getInstance(getApplication()).cancelAllWorkByTag("timetable_notification")
                    val file = java.io.File(getApplication<Application>().filesDir, "profile_photo.jpg")
                    if (file.exists()) file.delete()
                }

                val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await()
                val user = result.user
                if (user != null) {
                    // Save user info to Firestore
                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("users").document(user.uid).set(userData).await()
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Registration failed")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.PasswordResetSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Password reset failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    SyncManager(getApplication()).uploadLocalChanges(userId)
                }
                
                auth.signOut()
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                    userPreferences.clearAll()
                    TimerManager.clearAll(getApplication())
                    WorkManager.getInstance(getApplication()).cancelAllWorkByTag("timetable_notification")
                    // Delete local profile photo file
                    val file = java.io.File(getApplication<Application>().filesDir, "profile_photo.jpg")
                    if (file.exists()) file.delete()
                }
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Sign out error", e)
                auth.signOut()
                withContext(Dispatchers.IO) { 
                    database.clearAllTables()
                    userPreferences.clearAll()
                    TimerManager.clearAll(getApplication())
                }
                _authState.value = AuthState.Idle
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object PasswordResetSent : AuthState()
    data class Error(val message: String) : AuthState()
}
