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
import com.wade.school.data.local.entity.SemesterRecord
import com.wade.school.data.local.entity.Student
import com.wade.school.util.AcademicUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterRecordScreen(
    classId: String,
    viewModel: CounselorViewModel = viewModel(),
    onBack: () -> Unit
) {
    val year = AcademicUtils.getCurrentAcademicYear()
    val sem = AcademicUtils.getCurrentSemester()
    
    val students by viewModel.homeroomStudents.collectAsState()
    val records by viewModel.getSemesterRecords(classId, year, sem).collectAsState(initial = emptyList())
    
    var selectedStudent by remember { mutableStateOf<Student?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 學期結算 ($year-$sem)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Stats summary
            val finalizedCount = records.count { it.isFinalized }
            LinearProgressIndicator(
                progress = { if (students.isEmpty()) 0f else finalizedCount.toFloat() / students.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "結算進度: $finalizedCount / ${students.size}",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.labelSmall
            )

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(students) { student ->
                    val record = records.find { it.studentId == student.studentId }
                    SemesterStudentCard(student, record, viewModel) {
                        selectedStudent = student
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (selectedStudent != null) {
        val existingRecord = records.find { it.studentId == selectedStudent!!.studentId }
        EditSemesterRecordDialog(
            student = selectedStudent!!,
            record = existingRecord,
            viewModel = viewModel,
            onDismiss = { selectedStudent = null }
        )
    }
}

@Composable
fun SemesterStudentCard(student: Student, record: SemesterRecord?, viewModel: CounselorViewModel, onClick: () -> Unit) {
    val year = AcademicUtils.getCurrentAcademicYear()
    val sem = AcademicUtils.getCurrentSemester()
    val scoreOffset by viewModel.getDisciplineScore(student.studentId, year, sem).collectAsState(initial = 0)
    val calcScore = 85 + (scoreOffset ?: 0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${student.seatNo}", modifier = Modifier.width(30.dp), fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (record?.isFinalized == true) "已送出" else "編輯中",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (record?.isFinalized == true) Color(0xFF43A047) else Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("操行", style = MaterialTheme.typography.labelSmall)
                Text(
                    "${record?.conductScore ?: calcScore}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EditSemesterRecordDialog(student: Student, record: SemesterRecord?, viewModel: CounselorViewModel, onDismiss: () -> Unit) {
    var comment by remember { mutableStateOf(record?.teacherComment ?: "") }
    var conductScore by remember { mutableStateOf(record?.conductScore?.toString() ?: "") }
    var finalized by remember { mutableStateOf(record?.isFinalized ?: false) }
    
    val year = AcademicUtils.getCurrentAcademicYear()
    val sem = AcademicUtils.getCurrentSemester()
    
    // Templates for quick selection
    val templates = listOf(
        "學習態度積極，課堂參與熱忱，為同學樹立良好榜樣。",
        "思維敏捷，具備獨立思考能力，表現優異。",
        "性情溫和，與同學相處融洽，唯需加強自主學習。"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${student.name} 學期結算") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 450.dp)) {
                item {
                    OutlinedTextField(
                        value = conductScore,
                        onValueChange = { conductScore = it },
                        label = { Text("操行成績 (最終確認)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("導師評語", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                    
                    Text("評語模板", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    Column {
                        templates.forEach { t ->
                            TextButton(onClick = { comment += t }) {
                                Text(t, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = finalized, onCheckedChange = { finalized = it })
                        Text("標記為已結算 (Finalized)")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.saveSemesterRecord(
                    SemesterRecord(
                        id = record?.id ?: 0,
                        studentId = student.studentId,
                        studentName = student.name,
                        classId = student.currentClass,
                        academicYear = year,
                        semester = sem,
                        conductScore = conductScore.toFloatOrNull(),
                        teacherComment = comment,
                        isFinalized = finalized,
                        finalizedAt = if (finalized) System.currentTimeMillis() else null
                    )
                )
                onDismiss()
            }) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
