package com.wade.teacher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.verticalScroll

@Composable
fun WeeklyTimetableGrid(
    entries: List<com.wade.teacher.data.local.entity.TimetableEntry>,
    periods: List<com.wade.teacher.data.local.entity.PeriodTime>
) {
    val days = listOf("一", "二", "三", "四", "五")
    val periodsToUse = if (periods.isNotEmpty()) periods else (1..8).map { com.wade.teacher.data.local.entity.PeriodTime(period = it, startTime = "", endTime = "") }

    // Use a Box with a max height to ensure it fits in the dialog and triggers scrolling
    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row {
                Box(modifier = Modifier.size(45.dp, 40.dp), contentAlignment = Alignment.Center) {
                    Text("節次", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                days.forEach { day ->
                    Box(modifier = Modifier.size(75.dp, 40.dp), contentAlignment = Alignment.Center) {
                        Text(day, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
            
            periodsToUse.forEach { pt ->
                Row {
                    Box(modifier = Modifier.size(45.dp, 60.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${pt.period}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (pt.startTime.isNotEmpty()) {
                                Text(pt.startTime, fontSize = 8.sp, color = Color.Gray)
                            }
                        }
                    }
                    
                    (1..5).forEach { dayIndex ->
                        val entry = entries.find { it.dayOfWeek == dayIndex && it.period == pt.period }
                        Box(
                            modifier = Modifier
                                .size(75.dp, 60.dp)
                                .padding(2.dp)
                                .background(
                                    if (entry != null) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.shapes.extraSmall
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (entry != null) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)
                                ) {
                                    Text(
                                        text = entry.subjectName, 
                                        fontSize = 10.sp, 
                                        fontWeight = FontWeight.Bold, 
                                        lineHeight = 12.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = entry.classId, 
                                        fontSize = 9.sp, 
                                        lineHeight = 11.sp,
                                        maxLines = 1
                                    )
                                    if (entry.roomNumber.isNotEmpty()) {
                                        Text(
                                            text = entry.roomNumber, 
                                            fontSize = 9.sp, 
                                            color = MaterialTheme.colorScheme.primary, 
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 11.sp,
                                            maxLines = 1
                                        )
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
fun DashboardActionCard(title: String, description: String, actionText: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun SubjectClassSwitcher(
    viewModel: SubjectTeacherViewModel = viewModel()
) {
    // Correctly collect the Flow as State
    val assignedClasses by viewModel.assignedClasses.collectAsState(initial = emptyList())
    val currentLesson by viewModel.currentLesson.collectAsState()
    val fullTimetable by viewModel.fullTimetable.collectAsState()
    val periodTimes by viewModel.periodTimes.collectAsState()
    var showGrid by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("科任班級管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { showGrid = true }) {
                Text("週課表總覽")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        assignedClasses.forEach { classId ->
            val isCurrent = currentLesson?.classId == classId
            ClassCard(
                className = classId, 
                subject = if (isCurrent) currentLesson?.subjectName ?: "" else "班級導覽", 
                status = if (isCurrent) "🕒 正在上課..." else "點擊進入工作台"
            )
        }
        
        if (showGrid) {
            AlertDialog(
                onDismissRequest = { showGrid = false },
                title = { Text("個人週課表") },
                text = {
                    WeeklyTimetableGrid(fullTimetable, periodTimes)
                },
                confirmButton = {
                    TextButton(onClick = { showGrid = false }) { Text("關閉") }
                }
            )
        }
    }
}

@Composable
fun ClassCard(className: String, subject: String, status: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = className, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = subject, style = MaterialTheme.typography.bodySmall)
                Text(text = status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun InteractionHub() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("親師生互動樞紐", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        DashboardActionCard("家長線上簽閱", "聯絡簿數位化簽收系統", "進入系統")
        DashboardActionCard("學生提問箱", "來自 101 班的 3 個物理疑問", "查看問題")
        DashboardActionCard("心情溫度計", "今日有 2 位學生回報低落", "關懷訪談")
    }
}
