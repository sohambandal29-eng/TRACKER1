package com.example.tracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode") // "light", "dark", "system"
        val BIO_KEY = stringPreferencesKey("user_bio")
        val GOAL_KEY = stringPreferencesKey("user_goal")
        val PROFILE_PHOTO_KEY = stringPreferencesKey("profile_photo_uri")
        val UNLOCK_COUNT_KEY = androidx.datastore.preferences.core.intPreferencesKey("unlock_count")
        val LAST_RESET_DATE_KEY = stringPreferencesKey("last_reset_date")
        val STRICT_MODE_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("strict_mode")
        val IS_ADMIN_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("is_admin")
    }

    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    val userBio: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[BIO_KEY]
    }

    val userGoal: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[GOAL_KEY]
    }

    val profilePhotoUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PROFILE_PHOTO_KEY]
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: "system"
    }

    val strictMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[STRICT_MODE_KEY] ?: false
    }

    val unlockCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[UNLOCK_COUNT_KEY] ?: 0
    }

    val isAdmin: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ADMIN_KEY] ?: false
    }

    suspend fun incrementUnlockCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[UNLOCK_COUNT_KEY] ?: 0
            preferences[UNLOCK_COUNT_KEY] = current + 1
        }
    }

    suspend fun resetUnlockCount() {
        context.dataStore.edit { preferences ->
            preferences[UNLOCK_COUNT_KEY] = 0
        }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    suspend fun saveUserBio(bio: String) {
        context.dataStore.edit { preferences ->
            preferences[BIO_KEY] = bio
        }
    }

    suspend fun saveUserGoal(goal: String) {
        context.dataStore.edit { preferences ->
            preferences[GOAL_KEY] = goal
        }
    }

    suspend fun saveProfilePhotoUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_PHOTO_KEY] = uri
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }

    suspend fun saveStrictMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STRICT_MODE_KEY] = enabled
        }
    }

    suspend fun saveIsAdmin(isAdmin: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ADMIN_KEY] = isAdmin
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
