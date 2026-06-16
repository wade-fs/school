package com.wade.school.ui.screens

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
    onNavigateToAnalysis: (String) -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToManual: () -> Unit = {}
) {
    android.util.Log.d("SubjectTeacherDashboard", "Composing SubjectTeacherDashboard")
    val context = LocalContext.current
    val assignedClasses by viewModel.assignedClasses.collectAsState()
    val currentLesson by viewModel.currentLesson.collectAsState()
    val allClassIds by viewModel.allClassIds.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val fullTimetable by viewModel.fullTimetable.collectAsState()
    val periodTimes by viewModel.periodTimes.collectAsState()

    var showTimetableDialog by remember { mutableStateOf(false) }

    // Determine which class we are actually looking at for the student list
    val activeDisplayClassId = if (selectedClassId == "REAL_TIME") currentLesson?.classId else selectedClassId

    // Observe students for the active display class
    val studentsInClass by remember(activeDisplayClassId) {
        viewModel.getStudentsInClass(activeDisplayClassId ?: "")
    }.collectAsState(initial = emptyList())

    // Mime types for CSV
    val csvMimeTypes = arrayOf("text/csv", "text/comma-separated-values", "application/csv", "*/*")

    val studentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importStudents(context, it) }
    }
    val schedulePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importSchedule(context, it) }
    }

    // Simplified: "Real-time" chip + list of class IDs
    val classChips: List<Pair<String, String>> = remember(assignedClasses, allClassIds) {
        val base = if (assignedClasses.isNotEmpty()) assignedClasses else allClassIds
        listOf("REAL_TIME" to "🕒 即時") + base.map { it to it }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
        }

        // Pinned Header: Current Course Card and Class Switcher
        stickyHeader {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Current Course Card (Pinned)
                    if (currentLesson != null) {
                        val isRealTimeSelected = selectedClassId == "REAL_TIME"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRealTimeSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isRealTimeSelected) Icons.Default.PlayArrow else Icons.Default.Info, 
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isRealTimeSelected) "即時課程狀態" else "系統目前定位", 
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${currentLesson?.classId} 班 - ${currentLesson?.subjectName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("教室: ${currentLesson?.roomNumber}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("週${currentLesson?.dayOfWeek} 第${currentLesson?.period}節", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal Class Switcher (Pinned below card)
                    if (classChips.size > 1) {
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
                    }
                }
            }
        }

        // Scrollable Content
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show class schedule summary if a specific class is selected
                if (selectedClassId != null && selectedClassId != "REAL_TIME") {
                    Text("${selectedClassId} 班授課時段:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val classSchedule = fullTimetable.filter { it.classId == selectedClassId }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        classSchedule.forEach { entry ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text("週${entry.dayOfWeek} 第${entry.period}節", fontSize = 10.sp) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (activeDisplayClassId == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("尚未選擇班級或資料為空", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("請先從上方切換班級，或點擊右上角匯入學生/課表資料。", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Text("教學捷徑", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DashboardActionCard("教案模板庫", "對接 108 課綱核心素養", "開啟", onNavigateToLessonPlans)
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DashboardActionCard("課堂表現快速標記", "即時記錄學生發言、分組表現", "進入記錄", { activeDisplayClassId?.let { onNavigateToTagging(it) } })
            }
        }
        
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DashboardActionCard("作業派發", "管理作業截止日期與批改進度", "管理", { activeDisplayClassId?.let { onNavigateToAssignments(it) } })
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DashboardActionCard("學習成效分析", "班級成績分佈與個別學習曲線", "查看分析", { activeDisplayClassId?.let { onNavigateToAnalysis(it) } })
            }
        }
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DashboardActionCard("文件掃描", "拍照上傳紙本公文", "開始", onNavigateToScan)
            }
        }
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DashboardActionCard("使用手冊", "查看 App 操作說明", "查看", onNavigateToManual)
            }
        }

        // --- Student List Display ---
        if (activeDisplayClassId != null) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("${activeDisplayClassId} 班級學生清單", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (studentsInClass.isEmpty()) {
                item {
                    Text("此班級尚無學生資料。", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            items(studentsInClass) { student ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
