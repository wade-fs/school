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
import com.wade.school.data.local.entity.Student
import com.wade.school.data.local.entity.StudentHealthInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHealthScreen(
    classId: String,
    viewModel: CounselorViewModel = viewModel(),
    onBack: () -> Unit
) {
    val students by viewModel.homeroomStudents.collectAsState()
    var selectedStudent by remember { mutableStateOf<Student?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 健康與安全資訊") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            items(students) { student ->
                HealthEntryCard(student, viewModel) {
                    selectedStudent = student
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (selectedStudent != null) {
        EditHealthDialog(
            student = selectedStudent!!,
            viewModel = viewModel,
            onDismiss = { selectedStudent = null }
        )
    }
}

@Composable
fun HealthEntryCard(student: Student, viewModel: CounselorViewModel, onClick: () -> Unit) {
    var healthInfo by remember { mutableStateOf<StudentHealthInfo?>(null) }
    
    LaunchedEffect(student.studentId) {
        healthInfo = viewModel.getStudentHealth(student.studentId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${student.seatNo}號 ${student.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (healthInfo?.iepStudent == true) {
                    Badge(containerColor = MaterialTheme.colorScheme.tertiary) { Text("IEP") }
                }
            }
            
            if (healthInfo == null) {
                Text("尚未建立健康資料", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                Text("血型: ${healthInfo?.bloodType ?: "未填"} | 過敏: ${healthInfo?.allergies ?: "無"}", style = MaterialTheme.typography.bodySmall)
                if (!healthInfo?.chronicDisease.isNullOrBlank()) {
                    Text("慢性病: ${healthInfo?.chronicDisease}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Text("緊急聯絡: ${healthInfo?.emergencyContact1 ?: "未填"} (${healthInfo?.emergencyPhone1 ?: "-"})", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun EditHealthDialog(student: Student, viewModel: CounselorViewModel, onDismiss: () -> Unit) {
    var bloodType by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var chronicDisease by remember { mutableStateOf("") }
    var medication by remember { mutableStateOf("") }
    var contact1 by remember { mutableStateOf("") }
    var phone1 by remember { mutableStateOf("") }
    var iep by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(student.studentId) {
        val info = viewModel.getStudentHealth(student.studentId)
        info?.let {
            bloodType = it.bloodType ?: ""
            allergies = it.allergies ?: ""
            chronicDisease = it.chronicDisease ?: ""
            medication = it.medication ?: ""
            contact1 = it.emergencyContact1 ?: ""
            phone1 = it.emergencyPhone1 ?: ""
            iep = it.iepStudent
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${student.name} 健康安全編輯") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = iep, onCheckedChange = { iep = it })
                        Text("IEP 學生 (個別化教育計畫)")
                    }
                    OutlinedTextField(value = bloodType, onValueChange = { bloodType = it }, label = { Text("血型") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = allergies, onValueChange = { allergies = it }, label = { Text("過敏原") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = chronicDisease, onValueChange = { chronicDisease = it }, label = { Text("慢性病紀錄") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = medication, onValueChange = { medication = it }, label = { Text("長期用藥") }, modifier = Modifier.fillMaxWidth())
                    Divider(Modifier.padding(vertical = 8.dp))
                    Text("緊急聯絡人資訊", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(value = contact1, onValueChange = { contact1 = it }, label = { Text("聯絡人姓名") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone1, onValueChange = { phone1 = it }, label = { Text("聯絡電話") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.saveStudentHealth(
                    StudentHealthInfo(
                        studentId = student.studentId,
                        studentName = student.name,
                        bloodType = bloodType,
                        allergies = allergies,
                        chronicDisease = chronicDisease,
                        medication = medication,
                        emergencyContact1 = contact1,
                        emergencyPhone1 = phone1,
                        iepStudent = iep
                    )
                )
                onDismiss()
            }) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
