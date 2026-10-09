package com.example.firstapplications.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firstapplications.data.AttendanceStatus
import com.example.firstapplications.ui.SubjectDetailViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    viewModel: SubjectDetailViewModel,
    onBack: () -> Unit
) {
    val subject by viewModel.subject.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val timetables by viewModel.timetables.collectAsStateWithLifecycle()

    val recordsByDate = records.associateBy { it.date }
    
    var recordToEdit by remember { mutableStateOf<com.example.firstapplications.data.AttendanceRecordEntity?>(null) }
    var showAddPastDialog by remember { mutableStateOf(false) }
    var preselectedDateForAdd by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subject?.name ?: "Loading...", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    TextButton(onClick = { 
                        preselectedDateForAdd = LocalDate.now().minusDays(1)
                        showAddPastDialog = true 
                    }) {
                        Text("Add Past", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                val present = records.count { it.status == AttendanceStatus.PRESENT }
                val total = records.count { 
                    it.status in listOf(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT, AttendanceStatus.MASS_BUNK) 
                }
                val pct = if (total > 0) (present.toFloat() / total * 100) else 0f
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Overall Attendance", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$present / $total (${String.format(Locale.getDefault(), "%.1f", pct)}%)",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CalendarView(
                            recordsByDate = recordsByDate,
                            onDateClick = { date, existingRecord ->
                                if (existingRecord != null) {
                                    recordToEdit = existingRecord
                                } else if (!date.isAfter(LocalDate.now())) {
                                    preselectedDateForAdd = date
                                    showAddPastDialog = true
                                }
                            }
                        )
                    }
                }
            }

            item {
                Text("Recent History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            if (records.isEmpty()) {
                item {
                    Text("No attendance recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(records.sortedByDescending { it.date }) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { recordToEdit = record }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.date.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + record.date.dayOfMonth + ", " + record.date.year,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        StatusBadge(record.status)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
        
        if (recordToEdit != null) {
            EditRecordDialog(
                record = recordToEdit!!,
                onDismiss = { recordToEdit = null },
                onUpdate = { newStatus ->
                    viewModel.updateRecord(recordToEdit!!, newStatus)
                    recordToEdit = null
                },
                onDelete = {
                    viewModel.deleteRecord(recordToEdit!!)
                    recordToEdit = null
                }
            )
        }
        if (showAddPastDialog) {
            AddPastRecordDialog(
                timetables = timetables,
                initialDate = preselectedDateForAdd ?: LocalDate.now().minusDays(1),
                onDismiss = { showAddPastDialog = false },
                onAdd = { timetableId, date, status ->
                    viewModel.addRecord(timetableId, date, status)
                    showAddPastDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPastRecordDialog(
    timetables: List<com.example.firstapplications.data.TimetableEntity>,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onAdd: (Int, LocalDate, AttendanceStatus) -> Unit
) {
    if (timetables.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Error") },
            text = { Text("No timetables found for this subject. Cannot mark attendance.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedTimetable by remember { mutableStateOf(timetables.first()) }
    var selectedStatus by remember { mutableStateOf<AttendanceStatus?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Past Attendance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Quick Date Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { selectedDate = selectedDate.minusDays(1) }) { Text("< Prev Day") }
                    Text(
                        text = "${selectedDate.month.name.take(3)} ${selectedDate.dayOfMonth}",
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { selectedDate = selectedDate.plusDays(1) },
                        enabled = selectedDate.isBefore(LocalDate.now()) // Don't allow future dates
                    ) { Text("Next >") }
                }
                
                Text(
                    text = "Note: If you have multiple lectures of this subject on this day, select the specific time slot below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Timetable Selector (Dropdown substitute using LazyRow/ScrollableRow for simplicity)
                if (timetables.size > 1) {
                    Column {
                        timetables.forEach { timetable ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTimetable = timetable }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTimetable == timetable,
                                    onClick = { selectedTimetable = timetable }
                                )
                                Text("${timetable.startTime} - ${timetable.endTime} (${timetable.location})")
                            }
                        }
                    }
                }

                // Status Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButton("P", Color(0xFF4CAF50)) { selectedStatus = AttendanceStatus.PRESENT }
                    ActionButton("A", Color(0xFFF44336)) { selectedStatus = AttendanceStatus.ABSENT }
                    ActionButton("M", Color(0xFFFF9800)) { selectedStatus = AttendanceStatus.MASS_BUNK }
                }
                
                if (selectedStatus != null) {
                    Text("Selected Status: $selectedStatus", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedStatus != null) {
                        onAdd(selectedTimetable.id, selectedDate, selectedStatus!!)
                    }
                },
                enabled = selectedStatus != null
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditRecordDialog(
    record: com.example.firstapplications.data.AttendanceRecordEntity,
    onDismiss: () -> Unit,
    onUpdate: (AttendanceStatus) -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Attendance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select new status for ${record.date}:")
                val statuses = listOf(
                    AttendanceStatus.PRESENT to "Present",
                    AttendanceStatus.ABSENT to "Absent",
                    AttendanceStatus.MASS_BUNK to "Mass Bunk",
                    AttendanceStatus.CANCELLED to "Cancelled",
                    AttendanceStatus.HOLIDAY to "Holiday"
                )
                statuses.forEach { (status, label) ->
                    TextButton(
                        onClick = { onUpdate(status) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Delete Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CalendarView(
    recordsByDate: Map<LocalDate, com.example.firstapplications.data.AttendanceRecordEntity>,
    onDateClick: (LocalDate, com.example.firstapplications.data.AttendanceRecordEntity?) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Text("< Prev") }
            Text(
                text = currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + currentMonth.year,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Text("Next >") }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            userScrollEnabled = false
        ) {
            val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value - 1 // 0 for Mon, 6 for Sun
            val daysInMonth = currentMonth.lengthOfMonth()
            
            items(firstDayOfWeek) {
                Box(modifier = Modifier.aspectRatio(1f)) // Empty leading boxes
            }
            
            items(daysInMonth) { day ->
                val date = currentMonth.atDay(day + 1)
                val record = recordsByDate[date]
                
                val bgColor = when (record?.status) {
                    AttendanceStatus.PRESENT -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    AttendanceStatus.ABSENT -> Color(0xFFF44336).copy(alpha = 0.2f)
                    AttendanceStatus.MASS_BUNK -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    AttendanceStatus.CANCELLED -> Color(0xFF9E9E9E).copy(alpha = 0.2f)
                    AttendanceStatus.HOLIDAY -> Color(0xFF2196F3).copy(alpha = 0.2f)
                    else -> Color.Transparent
                }

                val textColor = when (record?.status) {
                    AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
                    AttendanceStatus.ABSENT -> Color(0xFFC62828)
                    AttendanceStatus.MASS_BUNK -> Color(0xFFEF6C00)
                    AttendanceStatus.CANCELLED -> Color(0xFF424242)
                    AttendanceStatus.HOLIDAY -> Color(0xFF1565C0)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { onDateClick(date, record) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (day + 1).toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (record != null) FontWeight.Bold else FontWeight.Normal,
                        color = textColor
                    )
                }
            }
        }
    }
}