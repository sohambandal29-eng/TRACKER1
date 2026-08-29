package com.example.tracker.ui.screens.tasks

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.data.local.entities.TaskEntity
import com.example.tracker.ui.components.GlassCard
import com.example.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    viewModel: TaskViewModel = viewModel()
) {
    val tasks by viewModel.todayTasks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<com.example.tracker.data.local.entities.TaskEntity?>(null) }

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
                    colors = listOf(PrimaryAccent.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(animOffset, 100f),
                    radius = 700f
                ),
                radius = 700f,
                center = Offset(animOffset, 100f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(size.width - animOffset, size.height - 200f),
                    radius = 900f
                ),
                radius = 900f,
                center = Offset(size.width - animOffset, size.height - 200f)
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
                                "Daily Tasks",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800, 400)) + slideInVertically(tween(800, 400)) { 40 }
                ) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = Color.White,
                        contentColor = BackgroundDark,
                        shape = CircleShape,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
            }
        ) { padding ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200)) { 40 }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tasks) { task ->
                        TaskItem(
                            task = task,
                            onDelete = { taskToDelete = task }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, category, priority, targetMinutes ->
                    viewModel.addTask(title, category, priority, targetMinutes)
                    showAddDialog = false
                }
            )
        }

        taskToDelete?.let { task ->
            AlertDialog(
                onDismissRequest = { taskToDelete = null },
                title = { Text("Delete Task", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete \"${task.title}\"?", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTask(task)
                            taskToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { taskToDelete = null }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                containerColor = Color(0xFF1A1A1D),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun TaskItem(
    task: TaskEntity,
    onDelete: () -> Unit
) {
    val isTaskCompleted = task.isCompleted || (task.targetMinutes > 0 && task.timeSpentSeconds >= task.targetMinutes * 60)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        showAccentGlow = !isTaskCompleted
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isTaskCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isTaskCompleted) "Completed" else "In Progress",
                    tint = if (isTaskCompleted) SuccessGreen else PrimaryAccent,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (isTaskCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            fontWeight = if (isTaskCompleted) FontWeight.Medium else FontWeight.Bold
                        ),
                        color = if (isTaskCompleted) Color.White.copy(alpha = 0.4f) else Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryAccent.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${task.timeSpentSeconds / 60}m / ${task.targetMinutes}m",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.White.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Study") }
    var targetHours by remember { mutableStateOf("1") }
    var targetMinutesInput by remember { mutableStateOf("0") }
    var customCategory by remember { mutableStateOf("") }
    var isCustomCategory by remember { mutableStateOf(false) }
    val categories = listOf("Exam", "Study", "Coding", "Revision", "Other")

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            alpha = 0.95f,
            containerColor = Color(0xFF1A1A1D),
            contentPadding = PaddingValues(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    "New Task",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Task name", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = targetHours,
                        onValueChange = { if (it.all { char -> char.isDigit() }) targetHours = it },
                        label = { Text("Hours", color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = targetMinutesInput,
                        onValueChange = { if (it.all { char -> char.isDigit() }) targetMinutesInput = it },
                        label = { Text("Minutes", color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Column {
                    Text(
                        "Category",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val selected = if (isCustomCategory) cat == "Other" else category == cat
                            Surface(
                                onClick = {
                                    if (cat == "Other") {
                                        isCustomCategory = true
                                    } else {
                                        isCustomCategory = false
                                        category = cat
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.1f),
                                contentColor = if (selected) BackgroundDark else Color.White,
                                border = if (selected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    cat,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (isCustomCategory) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        placeholder = { Text("Custom Category", color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val finalCategory = if (isCustomCategory) customCategory.ifBlank { "Other" } else category
                            val hours = targetHours.toLongOrNull() ?: 0L
                            val mins = targetMinutesInput.toLongOrNull() ?: 0L
                            val totalMinutes = (hours * 60) + mins
                            val finalTargetMinutes = if (totalMinutes > 0) totalMinutes else 60L
                            if (title.isNotBlank()) onConfirm(title, finalCategory, 2, finalTargetMinutes)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = BackgroundDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Task", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

