@file:OptIn(ExperimentalFoundationApi::class)

package com.example.tracker.ui.screens.roadmap

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.data.local.entities.RoadmapStageEntity
import com.example.tracker.data.local.entities.TimelineItemEntity
import com.example.tracker.ui.components.GlassCard
import com.example.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(viewModel: RoadmapViewModel = viewModel()) {
    val stages by viewModel.stages.collectAsState()
    val timelineItems by viewModel.timelineItems.collectAsState()
    val headers by viewModel.headers.collectAsState()

    var showAddStageDialog by remember { mutableStateOf(false) }
    var showAddTimelineDialog by remember { mutableStateOf(false) }

    var editingStage by remember { mutableStateOf<RoadmapStageEntity?>(null) }
    var editingTimelineItem by remember { mutableStateOf<TimelineItemEntity?>(null) }
    var editingHeader by remember { mutableStateOf<Pair<String, String>?>(null) } // id to current title
    var isTableView by remember { mutableStateOf(true) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Dynamic Animated Background Blobs
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(animOffset, 150f),
                    radius = 800f
                ),
                radius = 800f,
                center = Offset(animOffset, 150f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(size.width - animOffset, size.height - 300f),
                    radius = 900f
                ),
                radius = 900f,
                center = Offset(size.width - animOffset, size.height - 300f)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                        initialOffsetY = { -40 },
                        animationSpec = tween(1000)
                    )
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                "Study Roadmap",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                        },
                        actions = {
                            IconButton(onClick = { isTableView = !isTableView }) {
                                Icon(
                                    if (isTableView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle View",
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = { showAddStageDialog = true }) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add Stage", tint = Color.White)
                            }
                            IconButton(onClick = { showAddTimelineDialog = true }) {
                                Icon(Icons.Default.Event, contentDescription = "Add Timeline Item", tint = Color.White)
                            }
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier
                                        .background(Color(0xFF1A1A1D), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Clear All", color = Color.White) },
                                        onClick = {
                                            viewModel.clearAll()
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        ) { padding ->
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 300)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(1000, delayMillis = 300)
                )
            ) {
                if (isTableView) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val journeyTitle = headers.find { it.id == "journey" }?.title ?: "Study Journey"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(journeyTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                        IconButton(
                            onClick = { editingHeader = "journey" to journeyTitle },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape).size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = Color.White)
                        }
                    }

                    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        RoadmapStageTable(
                            stages = stages,
                            onEdit = { editingStage = it },
                            onDelete = { viewModel.deleteStage(it) }
                        )
                    }

                    val timelineTitle = headers.find { it.id == "timeline" }?.title ?: "Timeline"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(timelineTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                        IconButton(
                            onClick = { editingHeader = "timeline" to timelineTitle },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape).size(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = Color.White)
                        }
                    }

                    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        TimelineTable(
                            items = timelineItems,
                            onEdit = { editingTimelineItem = it },
                            onDelete = { viewModel.deleteTimelineItem(it) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        val journeyTitle = headers.find { it.id == "journey" }?.title ?: "Study Journey"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                journeyTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            IconButton(
                                onClick = { editingHeader = "journey" to journeyTitle },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Title",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    itemsIndexed(stages, key = { _, stage -> stage.id }) { index, stage ->
                        RoadmapStageCard(
                            stage = stage,
                            onEdit = { editingStage = it },
                            onDelete = { viewModel.deleteStage(it) },
                            onMoveUp = if (index > 0) { { viewModel.moveStage(index, index - 1) } } else null,
                            onMoveDown = if (index < stages.size - 1) { { viewModel.moveStage(index, index + 1) } } else null
                        )
                    }

                    item {
                        val timelineTitle = headers.find { it.id == "timeline" }?.title ?: "Timeline"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                timelineTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            IconButton(
                                onClick = { editingHeader = "timeline" to timelineTitle },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Title",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    items(timelineItems) { item ->
                        TimelineItemCard(
                            item = item,
                            onEdit = { editingTimelineItem = it },
                            onDelete = { viewModel.deleteTimelineItem(it) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }

            if (showAddStageDialog) {
                StageDialog(
                    onDismiss = { showAddStageDialog = false },
                    onConfirm = { title, duration, topics, resources ->
                        viewModel.addStage(title, duration, topics, resources)
                        showAddStageDialog = false
                    }
                )
            }

            editingStage?.let { stage ->
                StageDialog(
                    initialStage = stage,
                    onDismiss = { editingStage = null },
                    onConfirm = { title, duration, topics, resources ->
                        viewModel.updateStage(stage.copy(title = title, duration = duration, topics = topics, resources = resources))
                        editingStage = null
                    }
                )
            }

            if (showAddTimelineDialog) {
                TimelineDialog(
                    onDismiss = { showAddTimelineDialog = false },
                    onConfirm = { period, description ->
                        viewModel.addTimelineItem(period, description)
                        showAddTimelineDialog = false
                    }
                )
            }

            editingTimelineItem?.let { item ->
                TimelineDialog(
                    initialItem = item,
                    onDismiss = { editingTimelineItem = null },
                    onConfirm = { period, description ->
                        viewModel.updateTimelineItem(item.copy(period = period, description = description))
                        editingTimelineItem = null
                    }
                )
            }

            editingHeader?.let { (id, title) ->
                var newTitle by remember { mutableStateOf(title) }
                Dialog(onDismissRequest = { editingHeader = null }) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        alpha = 0.95f,
                        containerColor = Color(0xFF1A1A1D)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text("Edit Title", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                placeholder = { Text("Title", color = Color.White.copy(alpha = 0.4f)) },
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
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { editingHeader = null }) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) }
                                Button(
                                    onClick = {
                                        viewModel.updateHeader(id, newTitle)
                                        editingHeader = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BackgroundDark),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Save", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

}

@Composable
fun RoadmapStageCard(
    stage: RoadmapStageEntity,
    onEdit: (RoadmapStageEntity) -> Unit,
    onDelete: (RoadmapStageEntity) -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), alpha = 0.08f) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = onMoveUp ?: {},
                            modifier = Modifier.size(24.dp),
                            enabled = onMoveUp != null
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                tint = if (onMoveUp != null) Color.White else Color.White.copy(alpha = 0.1f)
                            )
                        }
                        IconButton(
                            onClick = onMoveDown ?: {},
                            modifier = Modifier.size(24.dp),
                            enabled = onMoveDown != null
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                tint = if (onMoveDown != null) Color.White else Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            stage.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .background(PrimaryAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, PrimaryAccent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                stage.duration,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = PrimaryAccent
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = { onEdit(stage) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = { onDelete(stage) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed.copy(alpha = 0.6f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val topicList = stage.topics.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            topicList.forEach { topic ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(PrimaryAccent.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, PrimaryAccent.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = PrimaryAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        topic,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            if (stage.resources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stage.resources,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineItemCard(
    item: TimelineItemEntity,
    onEdit: (TimelineItemEntity) -> Unit,
    onDelete: (TimelineItemEntity) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), alpha = 0.08f, showAccentGlow = true) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(PrimaryAccent, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.period,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Row {
                IconButton(onClick = { onEdit(item) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun StageDialog(
    initialStage: RoadmapStageEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialStage?.title ?: "") }
    var duration by remember { mutableStateOf(initialStage?.duration ?: "") }
    var topics by remember { mutableStateOf(initialStage?.topics ?: "") }
    var resources by remember { mutableStateOf(initialStage?.resources ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            alpha = 0.95f,
            containerColor = Color(0xFF1A1A1D)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    if (initialStage == null) "New Stage" else "Edit Stage",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Title (e.g. Physics Basics)", color = Color.White.copy(alpha = 0.4f)) },
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

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    placeholder = { Text("Duration (e.g. 2 Weeks)", color = Color.White.copy(alpha = 0.4f)) },
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

                OutlinedTextField(
                    value = topics,
                    onValueChange = { topics = it },
                    placeholder = { Text("Topics (comma separated)", color = Color.White.copy(alpha = 0.4f)) },
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

                OutlinedTextField(
                    value = resources,
                    onValueChange = { resources = it },
                    placeholder = { Text("Resources/Links", color = Color.White.copy(alpha = 0.4f)) },
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onConfirm(title, duration, topics, resources) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Stage", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineDialog(
    initialItem: TimelineItemEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var period by remember { mutableStateOf(initialItem?.period ?: "") }
    var description by remember { mutableStateOf(initialItem?.description ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            alpha = 0.95f,
            containerColor = Color(0xFF1A1A1D)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    if (initialItem == null) "New Timeline Item" else "Edit Item",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                OutlinedTextField(
                    value = period,
                    onValueChange = { period = it },
                    placeholder = { Text("Period (e.g. May-June)", color = Color.White.copy(alpha = 0.4f)) },
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

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Description", color = Color.White.copy(alpha = 0.4f)) },
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.6f)) }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onConfirm(period, description) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Item", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RoadmapStageTable(
    stages: List<RoadmapStageEntity>,
    onEdit: (RoadmapStageEntity) -> Unit,
    onDelete: (RoadmapStageEntity) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        alpha = 0.05f,
        isFloating = false
    ) {
        Column {
            Row(
                modifier = Modifier
                    .background(PrimaryAccent.copy(alpha = 0.15f))
                    .padding(vertical = 12.dp)
            ) {
                RoadmapTableCell(text = "STAGE", isHeader = true, width = 160.dp)
                RoadmapTableCell(text = "DURATION", isHeader = true, width = 100.dp)
                RoadmapTableCell(text = "TOPICS", isHeader = true, width = 250.dp)
                RoadmapTableCell(text = "RESOURCES", isHeader = true, width = 200.dp)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            stages.forEachIndexed { index, stage ->
                Row(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { onEdit(stage) },
                            onLongClick = { onDelete(stage) }
                        )
                        .background(if (index % 2 == 0) Color.White.copy(alpha = 0.03f) else Color.Transparent)
                ) {
                    RoadmapTableCell(text = stage.title, isHeader = false, width = 160.dp)
                    RoadmapTableCell(text = stage.duration, isHeader = false, width = 100.dp, isAccent = true)
                    RoadmapTableCell(text = stage.topics, isHeader = false, width = 250.dp)
                    RoadmapTableCell(text = stage.resources, isHeader = false, width = 200.dp)
                }
                if (index < stages.size - 1) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun TimelineTable(
    items: List<TimelineItemEntity>,
    onEdit: (TimelineItemEntity) -> Unit,
    onDelete: (TimelineItemEntity) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        alpha = 0.05f,
        isFloating = false
    ) {
        Column {
            Row(
                modifier = Modifier
                    .background(PrimaryAccent.copy(alpha = 0.15f))
                    .padding(vertical = 12.dp)
            ) {
                RoadmapTableCell(text = "PERIOD", isHeader = true, width = 130.dp)
                RoadmapTableCell(text = "DESCRIPTION", isHeader = true, width = 300.dp)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { onEdit(item) },
                            onLongClick = { onDelete(item) }
                        )
                        .background(if (index % 2 == 0) Color.White.copy(alpha = 0.03f) else Color.Transparent)
                ) {
                    RoadmapTableCell(text = item.period, isHeader = false, width = 130.dp, isAccent = true)
                    RoadmapTableCell(text = item.description, isHeader = false, width = 300.dp)
                }
                if (index < items.size - 1) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun RoadmapTableCell(
    text: String,
    isHeader: Boolean,
    width: Dp,
    isAccent: Boolean = false,
    textAlign: TextAlign = TextAlign.Start
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(12.dp),
        contentAlignment = if (textAlign == TextAlign.Center) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = if (isHeader) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
            else MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = if (isHeader) Color.White
            else if (isAccent) PrimaryAccent.copy(alpha = 0.9f)
            else Color.White.copy(alpha = 0.7f),
            textAlign = textAlign,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

