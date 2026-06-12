package com.wade.teacher.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SubjectTeacherDashboard(
    viewModel: SubjectTeacherViewModel = viewModel(),
    onNavigateToLessonPlans: () -> Unit = {},
    onNavigateToTagging: (String) -> Unit = {},
    onNavigateToAssignments: (String) -> Unit = {},
    onNavigateToAnalysis: (String) -> Unit = {}
) {
    android.util.Log.d("SubjectTeacherDashboard", "Composing SubjectTeacherDashboard")
    val context = LocalContext.current
    val assignedClasses by viewModel.assignedClasses.collectAsState()
    val allClassIds by viewModel.allClassIds.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val fullTimetable by viewModel.fullTimetable.collectAsState()
    val periodTimes by viewModel.periodTimes.collectAsState()
    val selectedClass = assignedClasses.find { it.classId == selectedClassId }

    var showTimetableDialog by remember { mutableStateOf(false) }

    // Observe students for selected class properly in the composable scope
    val studentsInClass by remember(selectedClassId) {
        viewModel.getStudentsInClass(selectedClassId ?: "")
    }.collectAsState(initial = emptyList())

    // Mime types for CSV
    val csvMimeTypes = arrayOf("text/csv", "text/comma-separated-values", "application/csv", "*/*")

    val studentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importStudents(context, it) }
    }
    val schedulePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importSchedule(context, it) }
    }

    // Move classChips calculation OUTSIDE LazyColumn as requested
    val classChips: List<Pair<String, String>> =
        if (assignedClasses.isNotEmpty())
            assignedClasses.map { it.classId to "${it.classId} ${it.subjectName}" }
        else
            allClassIds.map { it to it }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("教學工作台", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Row {
                        IconButton(onClick = { showTimetableDialog = true }) {
                            Icon(Icons.Default.GridView, contentDescription = "查看課表", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { studentPicker.launch(csvMimeTypes) }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "匯入學生", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { schedulePicker.launch(csvMimeTypes) }) {
                            Icon(Icons.Default.DateRange, contentDescription = "匯入課表", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = { viewModel.clearAllData() }) {
                            Icon(Icons.Default.Delete, contentDescription = "清空", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Horizontal Class Switcher (1-A)
        if (classChips.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("授課班級視角", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { showTimetableDialog = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("查看完整週課表", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    classChips.forEach { (id, label) ->
                        FilterChip(
                            selected = selectedClassId == id,
                            onClick = { viewModel.selectClass(id) },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else if (!isImporting) {
            item {
                Text("尚未匯入課表，請點擊上方日曆圖示進行匯入。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Current Course Card (1-B)
        if (selectedClass != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("即時課程狀態", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${selectedClass.classId} 班 - ${selectedClass.subjectName}", style = MaterialTheme.typography.titleMedium)
                        Text("教室: ${selectedClass.roomNumber} | 學生: ${selectedClass.studentCount} 位")
                        Text("下一堂課: ${selectedClass.nextLessonTime ?: "未排定"}", color = MaterialTheme.colorScheme.primary)
                        
                        // New: Small preview of class schedule
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("本班授課節次:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        val classSchedule = fullTimetable.filter { it.classId == selectedClass.classId }
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            classSchedule.forEach { entry ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text("週${entry.dayOfWeek} 第${entry.period}節", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Feature Shortcuts
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("教學捷徑", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            DashboardActionCard("教案模板庫", "對接 108 課綱核心素養", "開啟", onNavigateToLessonPlans)
        }

        item {
            DashboardActionCard("課堂表現快速標記", "即時記錄學生發言、分組表現", "進入記錄", { selectedClassId?.let { onNavigateToTagging(it) } })
        }
        
        item {
            DashboardActionCard("作業派發", "管理作業截止日期與批改進度", "管理", { selectedClassId?.let { onNavigateToAssignments(it) } })
        }

        item {
            DashboardActionCard("學習成效分析", "班級成績分佈與個別學習曲線", "查看分析", { selectedClassId?.let { onNavigateToAnalysis(it) } })
        }

        // --- Student List Display ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("班級學生清單", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(studentsInClass) { student ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                ListItem(
                    headlineContent = { Text("${student.seatNo}號 - ${student.name}") },
                    supportingContent = { Text("學號: ${student.studentId}") }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showTimetableDialog) {
        AlertDialog(
            onDismissRequest = { showTimetableDialog = false },
            title = { Text("個人週課表") },
            text = {
                WeeklyTimetableGrid(fullTimetable, periodTimes)
            },
            confirmButton = {
                TextButton(onClick = { showTimetableDialog = false }) { Text("關閉") }
            }
        )
    }
}
