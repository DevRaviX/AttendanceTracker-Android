package com.example.firstapplications.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.firstapplications.data.AttendanceStatus
import com.example.firstapplications.data.TodayClass
import com.example.firstapplications.ui.HomeUiState
import com.example.firstapplications.ui.HomeViewModel
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel, 
    onNavigateToSubject: (Int) -> Unit,
    onNavigateToAllSubjects: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Schedule", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToAllSubjects) {
                        Icon(Icons.Default.List, contentDescription = "All Subjects Insights")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CircularProgressDashboard(percentage = uiState.overallPercentage)
            }
            
            if (uiState.todayClasses.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(uiState.todayClasses, key = { it.timetableId }) { todayClass ->
                    val stats = uiState.subjectStats.find { it.subjectId == todayClass.subjectId }
                    val requiredClasses = stats?.let { HomeViewModel.calculateRequiredClasses(it.presentCount, it.totalConducted) } ?: 0
                    val safeBunks = stats?.let { HomeViewModel.calculateSafeBunks(it.presentCount, it.totalConducted) } ?: 0
                    
                    ClassCard(
                        todayClass = todayClass,
                        requiredClasses = requiredClasses,
                        safeBunks = safeBunks,
                        onStatusChange = { status -> viewModel.markAttendance(todayClass.timetableId, status) },
                        onClick = { onNavigateToSubject(todayClass.subjectId) }
                    )
                }
            }
        }
    }
}

@Composable
fun CircularProgressDashboard(percentage: Float) {
    var animationPlayed by remember { mutableStateOf(false) }
    val currentPercent = animateFloatAsState(
        targetValue = if (animationPlayed) percentage else 0f,
        animationSpec = tween(durationMillis = 1000), label = ""
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 16.dp)
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryColor,
                startAngle = 135f,
                sweepAngle = 270f * (currentPercent.value / 100f),
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${String.format(Locale.getDefault(), "%.1f", currentPercent.value)}%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Overall Attendance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ClassCard(
    todayClass: TodayClass,
    requiredClasses: Int,
    safeBunks: Int,
    onStatusChange: (AttendanceStatus) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = todayClass.subjectName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${todayClass.startTime} - ${todayClass.endTime} • ${todayClass.location}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = todayClass.teacher,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (todayClass.status != null) {
                    StatusBadge(todayClass.status)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 75% Engine Logic Display
            if (requiredClasses > 0) {
                Text(
                    text = "Status: Critical. Attend next $requiredClasses classes for 75%.",
                    color = Color(0xFFD32F2F), // Red
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            } else if (safeBunks > 0) {
                Text(
                    text = "Status: Safe. You can safely bunk $safeBunks classes.",
                    color = Color(0xFF388E3C), // Green
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton("P", Color(0xFF4CAF50)) { onStatusChange(AttendanceStatus.PRESENT) }
                ActionButton("A", Color(0xFFF44336)) { onStatusChange(AttendanceStatus.ABSENT) }
                ActionButton("M", Color(0xFFFF9800)) { onStatusChange(AttendanceStatus.MASS_BUNK) }
                ActionButton("C", Color(0xFF9E9E9E)) { onStatusChange(AttendanceStatus.CANCELLED) }
                ActionButton("H", Color(0xFF2196F3)) { onStatusChange(AttendanceStatus.HOLIDAY) }
            }
        }
    }
}

@Composable
fun ActionButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusBadge(status: AttendanceStatus) {
    val (color, text) = when (status) {
        AttendanceStatus.PRESENT -> Color(0xFF4CAF50) to "Present"
        AttendanceStatus.ABSENT -> Color(0xFFF44336) to "Absent"
        AttendanceStatus.MASS_BUNK -> Color(0xFFFF9800) to "Mass Bunk"
        AttendanceStatus.CANCELLED -> Color(0xFF9E9E9E) to "Cancelled"
        AttendanceStatus.HOLIDAY -> Color(0xFF2196F3) to "Holiday"
        AttendanceStatus.UNMARKED -> Color.Transparent to ""
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No classes scheduled for today!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}