package com.example.tracker.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tracker.ui.components.GlassCard
import com.example.tracker.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UnblockRequest(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val appName: String = "",
    val packageName: String = "",
    val status: String = "",
    val timestamp: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    var requests by remember { mutableStateOf<List<UnblockRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        isLoading = true
        val listener = db.collection("unblock_requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null) {
                    errorMessage = when {
                        e.message?.contains("permission-denied", ignoreCase = true) == true -> 
                            "Permission Denied: Admin is not authenticated with Firebase. Please ensure Firestore rules allow this or use a valid Firebase account."
                        else -> e.localizedMessage ?: "Failed to fetch requests"
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    requests = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UnblockRequest::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.timestamp }
                    errorMessage = null
                }
            }
        
        onDispose {
            listener.remove()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Admin Panel", fontWeight = FontWeight.Black, color = Color.White)
                            if (auth.currentUser == null) {
                                Text("Unauthenticated Mode", style = MaterialTheme.typography.labelSmall, color = Color.Yellow.copy(alpha = 0.7f))
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        // Real-time updates active
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryAccent)
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            errorMessage!!,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { /* Automatic retry via listener if it stays active */ },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                        ) {
                            Text("Try Again")
                        }
                    }
                } else if (requests.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No pending requests", color = TextSecondary, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("All caught up!", color = TextSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(requests, key = { it.id }) { request ->
                            UnblockRequestCard(
                                request = request,
                                onAccept = {
                                    scope.launch {
                                        try {
                                            // 1. Mark request as completed
                                            db.collection("unblock_requests").document(request.id)
                                                .update("status", "approved")
                                                .await()
                                            
                                            // 2. Remove the app from the user's blocked list
                                            db.collection("users").document(request.userId)
                                                .collection("blocked_apps").document(request.packageName)
                                                .delete()
                                                .await()
                                            
                                            snackbarHostState.showSnackbar("Request approved and app unblocked")
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Action failed: ${e.localizedMessage}")
                                        }
                                    }
                                },
                                onReject = {
                                    scope.launch {
                                        try {
                                            db.collection("unblock_requests").document(request.id)
                                                .update("status", "rejected")
                                                .await()
                                            snackbarHostState.showSnackbar("Request rejected")
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Action failed: ${e.localizedMessage}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun UnblockRequestCard(
    request: UnblockRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.appName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Text(request.packageName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(
                    java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(request.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("User: ${request.userEmail}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            Text("UID: ${request.userId}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Approve")
                }
                
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reject")
                }
            }
        }
    }
}
