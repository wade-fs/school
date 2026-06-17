package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.DisciplineRecord
import com.wade.school.data.local.entity.DisciplineType
import com.wade.school.data.local.entity.Student
import com.wade.school.util.AcademicUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisciplineScreen(
    classId: String,
    viewModel: CounselorViewModel = viewModel(),
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("新增獎懲", "紀錄查閱", "操行試算")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 獎懲管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AddDisciplineTab(classId, viewModel)
                1 -> DisciplineRecordsTab(classId, viewModel)
                2 -> ConductScoreTab(classId, viewModel)
            }
        }
    }
}

@Composable
fun AddDisciplineTab(classId: String, viewModel: CounselorViewModel) {
    val students by viewModel.homeroomStudents.collectAsState()
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var selectedType by remember { mutableStateOf(DisciplineType.COMMENDATION) }
    var reason by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showStudentPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedCard(
            onClick = { showStudentPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text(selectedStudent?.name ?: "選擇學生") },
                supportingContent = { Text(selectedStudent?.studentId ?: "請點擊選擇") },
                trailingContent = { Icon(Icons.Default.Person, null) }
            )
        }

        Text("獎懲類型", style = MaterialTheme.typography.labelMedium)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DisciplineType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.label) }
                )
            }
        }

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("獎懲事由") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("例如：打掃認真、協助班務、曠課...") }
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("備註") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                selectedStudent?.let { student ->
                    viewModel.saveDisciplineRecord(
                        DisciplineRecord(
                            studentId = student.studentId,
                            studentName = student.name,
                            classId = classId,
                            type = selectedType,
                            reason = reason,
                            academicYear = AcademicUtils.getCurrentAcademicYear(),
                            semester = AcademicUtils.getCurrentSemester(),
                            note = note
                        )
                    )
                    // Reset
                    reason = ""
                    note = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedStudent != null && reason.isNotBlank()
        ) {
            Text("儲存紀錄")
        }
    }

    if (showStudentPicker) {
        AlertDialog(
            onDismissRequest = { showStudentPicker = false },
            title = { Text("選擇學生") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(students) { student ->
                        ListItem(
                            headlineContent = { Text("${student.seatNo}號 ${student.name}") },
                            modifier = Modifier.clickable {
                                selectedStudent = student
                                showStudentPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStudentPicker = false }) { Text("取消") } }
        )
    }
}

@Composable
fun DisciplineRecordsTab(classId: String, viewModel: CounselorViewModel) {
    val records by viewModel.getDisciplineByClass(classId).collectAsState(initial = emptyList())
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("目前尚無獎懲紀錄", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(records) { record ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text("${record.studentName} - ${record.type.label}") },
                        supportingContent = { Text(record.reason) },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(sdf.format(Date(record.recordDate)), style = MaterialTheme.typography.labelSmall)
                                Text("${if (record.type.score > 0) "+" else ""}${record.type.score}", 
                                    color = if (record.type.score >= 0) Color(0xFF43A047) else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConductScoreTab(classId: String, viewModel: CounselorViewModel) {
    val students by viewModel.homeroomStudents.collectAsState()
    val year = AcademicUtils.getCurrentAcademicYear()
    val sem = AcademicUtils.getCurrentSemester()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("操行試算 (基準分 85)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(students) { student ->
            val scoreOffset by viewModel.getDisciplineScore(student.studentId, year, sem).collectAsState(initial = 0)
            val finalScore = 85 + (scoreOffset ?: 0)
            
            ListItem(
                headlineContent = { Text("${student.seatNo}號 ${student.name}") },
                trailingContent = {
                    Text(
                        text = "$finalScore",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (finalScore >= 80) Color(0xFF43A047) else if (finalScore >= 60) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            )
            HorizontalDivider()
        }
    }
}
