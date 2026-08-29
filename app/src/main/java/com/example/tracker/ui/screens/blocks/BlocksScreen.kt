package com.example.tracker.ui.screens.blocks

import android.content.Intent
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.data.local.entities.BlockedAppEntity
import com.example.tracker.ui.MainViewModel
import com.example.tracker.ui.components.GlassCard
import com.example.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocksScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val strictModeEnabled by viewModel.strictMode.collectAsState()
    val blockedApps by viewModel.blockedApps.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val pendingRequests by viewModel.pendingUnblockRequests.collectAsState()
    val isTimerStrictActive by viewModel.isTimerStrictActive.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // Auto-sync when entering the screen to fetch latest admin changes
    LaunchedEffect(Unit) {
        viewModel.syncData()
    }

    val filteredApps = remember(installedApps, searchQuery) {
        installedApps.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "App Blocks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StrictProtectionCard(
                        enabled = strictModeEnabled,
                        isAutoActive = isTimerStrictActive,
                        onToggle = { 
                            viewModel.setStrictMode(it)
                            viewModel.syncData()
                        }
                    )
                }

                item {
                    Column {
                        Text(
                            "Block Management",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            "Toggle apps to block them. Once blocked, you must send an email to trackerteamsupport@gmail.com to unblock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }
                }

                item {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search apps...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) }
                    )
                }

                items(filteredApps) { app ->
                    val isBlocked = blockedApps.any { it.packageName == app.packageName }
                    val isPending = pendingRequests.contains(app.packageName)
                    AppBlockToggleCard(
                        appName = app.name,
                        packageName = app.packageName,
                        isBlocked = isBlocked,
                        isPending = isPending,
                        onToggle = { checked ->
                            if (checked) {
                                viewModel.addBlockedApp(app.packageName, app.name)
                                viewModel.syncData()
                            } else {
                                // Request removal flow
                                val user = FirebaseAuth.getInstance().currentUser
                                val userId = user?.uid ?: "unknown_user"
                                val userEmail = user?.email ?: "No Email"
                                
                                val request = hashMapOf(
                                    "userId" to userId,
                                    "userEmail" to userEmail,
                                    "appName" to app.name,
                                    "packageName" to app.packageName,
                                    "timestamp" to System.currentTimeMillis(),
                                    "status" to "pending",
                                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                    "serverTimestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                )
                                
                                FirebaseFirestore.getInstance()
                                    .collection("unblock_requests")
                                    .add(request)
                                    .addOnSuccessListener {
                                        android.util.Log.d("BlocksScreen", "Request logged successfully")
                                        viewModel.syncData() // Trigger sync to check for immediate updates
                                    }
                                    .addOnFailureListener { e ->
                                        android.util.Log.e("BlocksScreen", "Error logging request", e)
                                    }

                                val bodyText = """
                                    APP UNBLOCK REQUEST FORM
                                    -----------------------
                                    User ID: $userId
                                    Package: ${app.packageName}
                                    App Name: ${app.name}
                                    
                                    Reason for unblocking:
                                    
                                    
                                    -----------------------
                                """.trimIndent()

                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:trackerteamsupport@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "UNBLOCK REQUEST: ${app.name}")
                                    putExtra(Intent.EXTRA_TEXT, bodyText)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.util.Log.e("BlocksScreen", "No email client found", e)
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun AppBlockToggleCard(
    appName: String,
    packageName: String,
    isBlocked: Boolean,
    isPending: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(packageName) {
        try {
            val pm = context.packageManager
            pm.getApplicationInfo(packageName, 0).loadIcon(pm)
        } catch (e: Exception) {
            null
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isBlocked) PrimaryAccent.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(appIcon),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        if (isBlocked) Icons.Default.Block else Icons.Default.Apps,
                        null,
                        tint = if (isBlocked) PrimaryAccent else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(appName, fontWeight = FontWeight.Bold, color = Color.White)
                    if (isPending) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFFF9800).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "PENDING",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
                Text(packageName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = isBlocked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
fun StrictProtectionCard(enabled: Boolean, isAutoActive: Boolean = false, onToggle: (Boolean) -> Unit) {
    val effectiveActive = enabled || isAutoActive
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        showAccentGlow = effectiveActive,
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (effectiveActive) PrimaryAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isAutoActive) Icons.Default.LockClock else Icons.Default.Shield,
                    null,
                    tint = if (effectiveActive) PrimaryAccent else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Strict Mode", fontWeight = FontWeight.Black, color = Color.White)
                    if (isAutoActive && !enabled) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = PrimaryAccent.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ACTIVE",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryAccent
                            )
                        }
                    }
                }
                Text(
                    if (isAutoActive && !enabled) "Automatically active during focus session" 
                    else "Prevents using other apps during focus", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = TextSecondary
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryAccent,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
fun AddAppDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var packageName by remember { mutableStateOf("") }
    var appName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add App to Block List", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Warning: Once added, you cannot remove this app yourself.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                TextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name (e.g. Instagram)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )
                TextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name (e.g. com.instagram.android)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (packageName.isNotBlank() && appName.isNotBlank()) onConfirm(packageName, appName) },
                enabled = packageName.isNotBlank() && appName.isNotBlank()
            ) {
                Text("Block Forever")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = BackgroundDark
    )
}

@Composable
fun BlockedAppCard(app: BlockedAppEntity, onRequestRemoval: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Block, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, fontWeight = FontWeight.Bold, color = Color.White)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            IconButton(onClick = onRequestRemoval) {
                Icon(Icons.Default.Mail, "Request Removal", tint = PrimaryAccent)
            }
        }
    }
}
