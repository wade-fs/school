package com.wade.school.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.SubjectAttendance
import com.wade.school.data.local.entity.AttendanceStatus
import com.wade.school.util.AcademicUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectAttendanceScreen(
    classId: String,
    onBack: () -> Unit,
    date: Long = System.currentTimeMillis(),
    period: Int = 1,
    viewModel: SubjectTeacherViewModel = viewModel()
) {
    val students by viewModel.getStudentsInClass(classId).collectAsState(initial = emptyList())
    val attendanceRecords by viewModel.getSubjectAttendance(classId, date, period).collectAsState(initial = emptyList())
    
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val dateStr = sdf.format(Date(date))

    // Map student ID to current status (default "PRESENT")
    var localStatusMap by remember(attendanceRecords, students) {
        val initialMap = students.associate { it.studentId to AttendanceStatus.PRESENT }.toMutableMap()
        attendanceRecords.forEach { record ->
            initialMap[record.studentId] = record.status
        }
        mutableStateOf(initialMap)
    }

    val options = AttendanceStatus.values()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("$classId 班 - 科任點名", style = MaterialTheme.typography.titleMedium)
                        Text("$dateStr (第 $period 節)", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val records = students.map { student ->
                                SubjectAttendance(
                                    studentId = student.studentId,
                                    studentName = student.name,
                                    classId = classId,
                                    subjectName = "自訂科目", // Should be dynamic
                                    date = date,
                                    period = period,
                                    status = localStatusMap[student.studentId] ?: AttendanceStatus.PRESENT
                                )
                            }
                            viewModel.saveSubjectAttendance(records)
                            onBack()
                        }
                    ) {
                        Text("送出")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("名單 (${students.size} 人)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(students) { student ->
                val currentStatus = localStatusMap[student.studentId] ?: AttendanceStatus.PRESENT
                val isAbnormal = currentStatus != AttendanceStatus.PRESENT
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAbnormal) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${student.seatNo}號 - ${student.name}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(student.studentId, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Use FlowRow or a Scrollable Row if many statuses
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            options.forEach { option ->
                                FilterChip(
                                    selected = currentStatus == option,
                                    onClick = {
                                        val newMap = localStatusMap.toMutableMap()
                                        newMap[student.studentId] = option
                                        localStatusMap = newMap
                                    },
                                    label = { Text(getStatusLabel(option), fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getStatusLabel(status: AttendanceStatus): String = when(status) {
    AttendanceStatus.PRESENT -> "出席"
    AttendanceStatus.LATE -> "遲到"
    AttendanceStatus.ABSENT -> "曠課"
    AttendanceStatus.SICK -> "病假"
    AttendanceStatus.PERSONAL -> "事假"
    AttendanceStatus.OFFICIAL -> "公假"
    AttendanceStatus.EARLY_LEAVE -> "早退"
}
