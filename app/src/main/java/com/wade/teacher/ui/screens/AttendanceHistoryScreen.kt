package com.wade.teacher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.teacher.data.local.entity.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    classId: String,
    onBack: () -> Unit,
    onEditDate: (Long, String) -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    val allRecords by viewModel.getAllAttendanceForClass(classId).collectAsState(initial = emptyList())
    val students by viewModel.students.collectAsState()
    val studentsInClass = students.filter { it.currentClass == classId }
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd (E)", Locale.getDefault()) }

    // Group records by date, then by period
    val groupedRecords = remember(allRecords) {
        allRecords.groupBy { record ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = record.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.mapValues { (_, records) ->
            records.groupBy { it.periodName }
        }.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 班 - 點名歷程") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (groupedRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("尚無點名紀錄", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val flatRecords = allRecords.groupBy { record ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = record.date
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }
                    AttendanceAnalysisCard(flatRecords, studentsInClass.size)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("歷史紀錄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                groupedRecords.forEach { (dateMillis, periodMap) ->
                    item {
                        Column {
                            Text(
                                text = sdf.format(Date(dateMillis)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            periodMap.forEach { (period, records) ->
                                val abnormalCount = records.count { it.status != "出席" }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onEditDate(dateMillis, period) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (abnormalCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(period, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("異常: $abnormalCount 人", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Icon(Icons.Default.Edit, contentDescription = "編輯", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
fun AttendanceAnalysisCard(groupedRecords: Map<Long, List<AttendanceRecord>>, totalStudents: Int) {
    val totalDays = groupedRecords.size
    val totalAbnormal = groupedRecords.values.flatten().count { it.status != "出席" }
    val avgAttendanceRate = if (totalDays > 0 && totalStudents > 0) {
        val totalPossible = totalDays * totalStudents
        val totalActual = totalPossible - totalAbnormal
        (totalActual.toFloat() / totalPossible.toFloat() * 100).toInt()
    } else 100

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("出席率分析 (最近 $totalDays 天)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("平均出席率", style = MaterialTheme.typography.labelSmall)
                    Text("$avgAttendanceRate%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("累計異常次數", style = MaterialTheme.typography.labelSmall)
                    Text("$totalAbnormal 次", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
