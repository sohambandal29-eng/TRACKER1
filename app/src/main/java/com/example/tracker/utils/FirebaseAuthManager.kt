package com.example.tracker.utils

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FirebaseAuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val _userIdFlow = MutableStateFlow<String?>(auth.currentUser?.uid)
    val userIdFlow: StateFlow<String?> = _userIdFlow.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _userIdFlow.value = firebaseAuth.currentUser?.uid
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
}
