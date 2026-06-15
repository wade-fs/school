package com.wade.teacher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.wade.teacher.data.local.entity.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    classId: String,
    onBack: () -> Unit,
    date: Long = System.currentTimeMillis(),
    viewModel: CounselorViewModel = viewModel()
) {
    val students by viewModel.students.collectAsState()
    val attendanceRecords by viewModel.getAttendanceForDate(classId, date).collectAsState(initial = emptyList())
    
    val studentsInClass = students.filter { it.currentClass == classId }
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val dateStr = sdf.format(Date(date))

    // Map student ID to current status (default "出席")
    var localStatusMap by remember(attendanceRecords, studentsInClass) {
        val initialMap = studentsInClass.associate { it.studentId to "出席" }.toMutableMap()
        attendanceRecords.forEach { record ->
            initialMap[record.studentId] = record.status
        }
        mutableStateOf(initialMap)
    }

    val options = listOf("出席", "遲到", "曠課", "病假", "事假", "公假")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("$classId 班 - 數位點名", style = MaterialTheme.typography.titleMedium)
                        Text(dateStr, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val records = studentsInClass.map { student ->
                                AttendanceRecord(
                                    studentId = student.studentId,
                                    classId = classId,
                                    date = date,
                                    status = localStatusMap[student.studentId] ?: "出席"
                                )
                            }
                            viewModel.submitAttendance(records, date)
                            onBack()
                        }
                    ) {
                        Text("送出", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("今日名單 (${studentsInClass.size} 人)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(studentsInClass) { student ->
                val currentStatus = localStatusMap[student.studentId] ?: "出席"
                val isAbnormal = currentStatus != "出席"
                
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
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            options.forEachIndexed { index, option ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    onClick = { 
                                        val newMap = localStatusMap.toMutableMap()
                                        newMap[student.studentId] = option
                                        localStatusMap = newMap
                                    },
                                    selected = currentStatus == option
                                ) {
                                    Text(option, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
