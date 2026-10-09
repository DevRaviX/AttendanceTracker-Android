package com.devravix.attendancetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devravix.attendancetracker.ui.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSubjectsScreen(
    viewModel: HomeViewModel,
    onNavigateToSubject: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Subjects Insights", fontWeight = FontWeight.Bold) },
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
            items(uiState.subjectStats, key = { it.subjectId }) { stats ->
                val requiredClasses = HomeViewModel.calculateRequiredClasses(stats.presentCount, stats.totalConducted)
                val safeBunks = HomeViewModel.calculateSafeBunks(stats.presentCount, stats.totalConducted)
                val percentage = if (stats.totalConducted > 0) (stats.presentCount.toFloat() / stats.totalConducted) * 100 else 0f

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSubject(stats.subjectId) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stats.subjectName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${String.format(java.util.Locale.getDefault(), "%.1f", percentage)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (percentage >= 75f) Color(0xFF388E3C) else Color(0xFFD32F2F)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Attendance: ${stats.presentCount} / ${stats.totalConducted} (Present/Conducted)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (requiredClasses > 0) {
                            Text(
                                text = "Critical: Attend next $requiredClasses classes to hit 75%.",
                                color = Color(0xFFD32F2F),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (safeBunks > 0) {
                            Text(
                                text = "Safe: You can bunk next $safeBunks classes.",
                                color = Color(0xFF388E3C),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (stats.totalConducted > 0) {
                            Text(
                                text = "On Track: Attendance is exactly 75%.",
                                color = Color(0xFFF57C00),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "No classes conducted yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}