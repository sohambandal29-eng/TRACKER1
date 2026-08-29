@file:OptIn(ExperimentalFoundationApi::class)

package com.example.tracker.ui.screens.timetable

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import android.content.ContentValues
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import java.io.OutputStream
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tracker.data.local.entities.TimetableEntity
import androidx.compose.ui.graphics.Brush
import com.example.tracker.ui.theme.*
import com.example.tracker.ui.components.GlassCard
import androidx.compose.foundation.border
import com.example.tracker.utils.NotificationHelper
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(viewModel: TimetableViewModel = viewModel()) {
    val timetableItems by viewModel.timetableItems.collectAsState()
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<TimetableEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<TimetableEntity?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var isTableView by remember { mutableStateOf(true) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Animated Background
        Canvas(modifier = Modifier.fillMaxSize().blur(100.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(animOffset, 200f),
                    radius = 900f
                ),
                radius = 900f,
                center = Offset(animOffset, 200f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width - animOffset, size.height - 300f),
                    radius = 1100f
                ),
                radius = 1100f,
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
                    CenterAlignedTopAppBar(
                        title = { 
                            Text(
                                "Study Timetable", 
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.White
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
                            IconButton(onClick = { 
                                downloadTimetablePdf(context, days, timetableItems)
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = Color.White)
                            }
                            IconButton(onClick = { showClearConfirm = true }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.background(Brush.linearGradient(PrimaryGradient), CircleShape),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Item", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                Box(modifier = Modifier.padding(padding)) {
                    if (isTableView) {
                        WeeklyTableView(
                            days = days,
                            timetableItems = timetableItems,
                            onEdit = { editingItem = it },
                            onDelete = { itemToDelete = it }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            days.forEach { day ->
                                val itemsForDay = timetableItems[day] ?: emptyList()
                                if (itemsForDay.isNotEmpty()) {
                                    item {
                                        TimetableTable(
                                            day = day,
                                            items = itemsForDay,
                                            onEdit = { editingItem = it },
                                            onDelete = { itemToDelete = it }
                                        )
                                    }
                                }
                            }
                            
                            if (timetableItems.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No timetable items yet. Tap + to add.", color = Color.White.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddEditTimetableDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { day, timing, endTime, subject, allDays, excludedDays ->
                    if (allDays) {
                        viewModel.addItemsToAllDays(timing, endTime, subject, excludedDays)
                    } else {
                        viewModel.addItem(day, timing, endTime, subject)
                    }
                    showAddDialog = false
                }
            )
        }

        if (editingItem != null) {
            AddEditTimetableDialog(
                initialDay = editingItem!!.day,
                initialTiming = editingItem!!.timing,
                initialEndTime = editingItem!!.endTime,
                initialSubject = editingItem!!.subject,
                isEdit = true,
                onDismiss = { editingItem = null },
                onConfirm = { day, timing, endTime, subject, _, _ ->
                    viewModel.updateItem(editingItem!!.copy(day = day, timing = timing, endTime = endTime, subject = subject))
                    editingItem = null
                }
            )
        }

        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Delete Item", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete '${itemToDelete?.subject}'? This cannot be undone.", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    TextButton(onClick = {
                        itemToDelete?.let { viewModel.deleteItem(it) }
                        itemToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = BackgroundDark,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            )
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear Timetable", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to clear the entire timetable? This action is permanent.", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAll()
                        showClearConfirm = false
                    }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = BackgroundDark,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            )
        }
    }
}

@Composable
fun TimetableTable(
    day: String,
    items: List<TimetableEntity>,
    onEdit: (TimetableEntity) -> Unit,
    onDelete: (TimetableEntity) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = day,
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(SecondaryGradient),
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(
                            if (index % 2 == 0) Color.White.copy(alpha = 0.03f) 
                            else Color.Transparent, 
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.35f)) {
                        Text(
                            item.timing, 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (item.endTime.isNotBlank()) {
                            Text(
                                item.endTime, 
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Text(
                        item.subject, 
                        modifier = Modifier.weight(0.45f), 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(modifier = Modifier.weight(0.2f), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { onEdit(item) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTimetableDialog(
    initialDay: String = "Monday",
    initialTiming: String = "",
    initialEndTime: String = "",
    initialSubject: String = "",
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Boolean, List<String>) -> Unit
) {
    var day by remember { mutableStateOf(initialDay) }
    var timing by remember { mutableStateOf(initialTiming) }
    var endTime by remember { mutableStateOf(initialEndTime) }
    var subject by remember { mutableStateOf(initialSubject) }
    var applyToAllDays by remember { mutableStateOf(false) }
    var excludedDays by remember { mutableStateOf(setOf<String>()) }
    
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var expanded by remember { mutableStateOf(false) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    val initialTime = remember(initialTiming) {
        try {
            if (initialTiming.isBlank()) LocalTime.of(9, 0)
            else {
                val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                LocalTime.parse(initialTiming.uppercase(), formatter)
            }
        } catch (e: Exception) {
            LocalTime.of(9, 0)
        }
    }

    val initialEndTimeVal = remember(initialEndTime) {
        try {
            if (initialEndTime.isBlank()) LocalTime.of(10, 0)
            else {
                val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                LocalTime.parse(initialEndTime.uppercase(), formatter)
            }
        } catch (e: Exception) {
            LocalTime.of(10, 0)
        }
    }
    
    val startTimePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = false
    )

    val endTimePickerState = rememberTimePickerState(
        initialHour = initialEndTimeVal.hour,
        initialMinute = initialEndTimeVal.minute,
        is24Hour = false
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedTime = LocalTime.of(startTimePickerState.hour, startTimePickerState.minute)
                    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                    timing = selectedTime.format(formatter)
                    showStartTimePicker = false
                }) {
                    Text("OK", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        ) {
            TimePicker(
                state = startTimePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Color.White.copy(alpha = 0.05f),
                    clockDialSelectedContentColor = Color.White,
                    clockDialUnselectedContentColor = Color.White.copy(alpha = 0.6f),
                    selectorColor = PrimaryAccent,
                    periodSelectorBorderColor = Color.White.copy(alpha = 0.2f),
                    periodSelectorSelectedContainerColor = PrimaryAccent.copy(alpha = 0.2f),
                    periodSelectorUnselectedContainerColor = Color.Transparent,
                    periodSelectorSelectedContentColor = Color.White,
                    periodSelectorUnselectedContentColor = Color.White.copy(alpha = 0.6f),
                    timeSelectorSelectedContainerColor = PrimaryAccent.copy(alpha = 0.2f),
                    timeSelectorUnselectedContainerColor = Color.White.copy(alpha = 0.05f),
                    timeSelectorSelectedContentColor = Color.White,
                    timeSelectorUnselectedContentColor = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedTime = LocalTime.of(endTimePickerState.hour, endTimePickerState.minute)
                    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                    endTime = selectedTime.format(formatter)
                    showEndTimePicker = false
                }) {
                    Text("OK", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        ) {
            TimePicker(
                state = endTimePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Color.White.copy(alpha = 0.05f),
                    clockDialSelectedContentColor = Color.White,
                    clockDialUnselectedContentColor = Color.White.copy(alpha = 0.6f),
                    selectorColor = PrimaryAccent,
                    periodSelectorBorderColor = Color.White.copy(alpha = 0.2f),
                    periodSelectorSelectedContainerColor = PrimaryAccent.copy(alpha = 0.2f),
                    periodSelectorUnselectedContainerColor = Color.Transparent,
                    periodSelectorSelectedContentColor = Color.White,
                    periodSelectorUnselectedContentColor = Color.White.copy(alpha = 0.6f),
                    timeSelectorSelectedContainerColor = PrimaryAccent.copy(alpha = 0.2f),
                    timeSelectorUnselectedContainerColor = Color.White.copy(alpha = 0.05f),
                    timeSelectorSelectedContentColor = Color.White,
                    timeSelectorUnselectedContentColor = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            alpha = 0.95f,
            containerColor = Color(0xFF1A1A1D)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isEdit) "Edit Item" else "Add Item",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                if (!isEdit) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = applyToAllDays,
                                onCheckedChange = { applyToAllDays = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PrimaryAccent,
                                    uncheckedColor = Color.White.copy(alpha = 0.4f),
                                    checkmarkColor = Color.White
                                )
                            )
                            Text("Repeat daily", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        if (applyToAllDays) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Except:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                days.forEach { d ->
                                    FilterChip(
                                        selected = d in excludedDays,
                                        onClick = {
                                            excludedDays = if (d in excludedDays) {
                                                excludedDays - d
                                            } else {
                                                excludedDays + d
                                            }
                                        },
                                        label = { Text(d.take(3)) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = Color.White,
                                            containerColor = Color.Transparent,
                                            labelColor = Color.White.copy(alpha = 0.5f)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = d in excludedDays,
                                            borderColor = Color.White.copy(alpha = 0.1f),
                                            selectedBorderColor = PrimaryAccent.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (!applyToAllDays) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = day,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Day of the week", color = Color.White.copy(alpha = 0.5f)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(BackgroundDark).border(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            days.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption, color = Color.White) },
                                    onClick = {
                                        day = selectionOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).clickable { showStartTimePicker = true }) {
                        OutlinedTextField(
                            value = timing,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Start", color = Color.White.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.White,
                                disabledBorderColor = Color.White.copy(alpha = 0.2f),
                                disabledLabelColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                    Box(modifier = Modifier.weight(1f).clickable { showEndTimePicker = true }) {
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("End", color = Color.White.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.White,
                                disabledBorderColor = Color.White.copy(alpha = 0.2f),
                                disabledLabelColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
                
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Activity", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = { 
                            if (timing.isNotBlank() && subject.isNotBlank()) {
                                onConfirm(day, timing, endTime, subject, applyToAllDays, excludedDays.toList())
                            }
                        },
                        enabled = timing.isNotBlank() && subject.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = BackgroundDark,
                            disabledContainerColor = Color.White.copy(alpha = 0.2f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .padding(24.dp),
            alpha = 0.95f,
            containerColor = Color(0xFF1A1A1D)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = "Select Time",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

fun downloadTimetablePdf(
    context: Context,
    days: List<String>,
    timetableItems: Map<String, List<TimetableEntity>>
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(1000, 1414, 1).create() // A4-ish proportions
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = AndroidPaint()

    // Title
    paint.textSize = 24f
    paint.isFakeBoldText = true
    canvas.drawText("Study Timetable", 40f, 50f, paint)

    val startX = 40f
    val startY = 100f
    val cellWidth = 120f
    val cellHeight = 40f
    
    // Header (Days)
    paint.textSize = 12f
    paint.isFakeBoldText = true
    days.forEachIndexed { index, day ->
        val x = startX + (index + 1) * cellWidth
        canvas.drawRect(x, startY, x + cellWidth, startY + cellHeight, paint.apply { style = AndroidPaint.Style.STROKE })
        canvas.drawText(day, x + 10f, startY + 25f, paint.apply { style = AndroidPaint.Style.FILL })
    }

    // Collect all unique times
    val allTimes = timetableItems.values.flatten()
        .map { it.timing }
        .distinct()
        .sortedWith { t1, t2 ->
            try {
                val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                LocalTime.parse(t1.uppercase(), formatter).compareTo(LocalTime.parse(t2.uppercase(), formatter))
            } catch (e: Exception) {
                t1.compareTo(t2)
            }
        }

    // Draw grid and content
    allTimes.forEachIndexed { timeIndex, time ->
        val y = startY + (timeIndex + 1) * cellHeight
        
        // Time label
        paint.isFakeBoldText = true
        canvas.drawRect(startX, y, startX + cellWidth, y + cellHeight, paint.apply { style = AndroidPaint.Style.STROKE })
        canvas.drawText(time, startX + 10f, y + 25f, paint.apply { style = AndroidPaint.Style.FILL })

        days.forEachIndexed { dayIndex, day ->
            val x = startX + (dayIndex + 1) * cellWidth
            canvas.drawRect(x, y, x + cellWidth, y + cellHeight, paint.apply { style = AndroidPaint.Style.STROKE })
            
            val item = timetableItems[day]?.find { it.timing == time }
            if (item != null) {
                paint.isFakeBoldText = false
                val subjectText = if (item.subject.length > 15) item.subject.take(12) + "..." else item.subject
                canvas.drawText(subjectText, x + 5f, y + 25f, paint)
            }
        }
    }

    pdfDocument.finishPage(page)

    val fileName = "Timetable_${System.currentTimeMillis()}.pdf"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    
    try {
        uri?.let {
            val outputStream: OutputStream? = context.contentResolver.openOutputStream(it)
            outputStream?.use { stream ->
                pdfDocument.writeTo(stream)
            }
            Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_LONG).show()
            
            // Show pop-up notification
            NotificationHelper(context).showNotification(
                title = "Timetable Downloaded",
                message = "Your study timetable PDF has been saved to the Downloads folder.",
                notificationId = (System.currentTimeMillis() % 10000).toInt()
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}

@Composable
fun WeeklyTableView(
    days: List<String>,
    timetableItems: Map<String, List<TimetableEntity>>,
    onEdit: (TimetableEntity) -> Unit,
    onDelete: (TimetableEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTimeSlots = remember(timetableItems) {
        timetableItems.values.flatten()
            .map { it.timing to it.endTime }
            .distinct()
            .sortedWith { t1, t2 ->
                try {
                    val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
                    val time1 = LocalTime.parse(t1.first.uppercase(), formatter)
                    val time2 = LocalTime.parse(t2.first.uppercase(), formatter)
                    time1.compareTo(time2)
                } catch (e: Exception) {
                    t1.first.compareTo(t2.first)
                }
            }
    }

    if (allTimeSlots.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No timetable items yet. Tap + to add.", color = Color.White.copy(alpha = 0.5f))
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        GlassCard(modifier = Modifier.fillMaxSize(), alpha = 0.05f) {
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .background(PrimaryAccent.copy(alpha = 0.15f))
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableCell(text = "TIME", isHeader = true, width = 90.dp)
                        days.forEach { day ->
                            TableCell(text = day.take(3).uppercase(), isHeader = true, width = 120.dp)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                    // Time Rows
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allTimeSlots) { (startTime, endTime) ->
                            Row(
                                modifier = Modifier.height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableCell(
                                    text = startTime, 
                                    isHeader = false, 
                                    width = 90.dp, 
                                    isTime = true, 
                                    endTime = endTime
                                )
                                
                                days.forEach { day ->
                                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color.White.copy(alpha = 0.05f)))
                                    val item = timetableItems[day]?.find { it.timing == startTime && it.endTime == endTime }
                                    SubjectCell(
                                        item = item,
                                        width = 120.dp,
                                        onEdit = onEdit,
                                        onDelete = onDelete
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(text: String, isHeader: Boolean, width: Dp, isTime: Boolean = false, endTime: String = "") {
    Box(
        modifier = Modifier
            .width(width)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = if (isHeader) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                        else MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isHeader) Color.White 
                        else if (isTime) PrimaryAccent.copy(alpha = 0.9f)
                        else Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            if (isTime && endTime.isNotBlank()) {
                Text(
                    text = endTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SubjectCell(
    item: TimetableEntity?,
    width: Dp,
    onEdit: (TimetableEntity) -> Unit,
    onDelete: (TimetableEntity) -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(70.dp)
            .padding(4.dp)
            .then(
                if (item != null) Modifier
                    .background(
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))), 
                        RoundedCornerShape(8.dp)
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { onEdit(item) },
                        onLongClick = { onDelete(item) }
                    )
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (item != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = item.subject,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
