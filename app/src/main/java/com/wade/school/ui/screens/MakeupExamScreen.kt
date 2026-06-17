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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.MakeupExam
import com.wade.school.data.local.entity.MakeupExamStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeupExamScreen(
    viewModel: SubjectTeacherViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val pendingMakeups by viewModel.pendingMakeups.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("待處理", "已排定", "已完成")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("補考管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                                if (index == 0 && pendingMakeups.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge { Text("${pendingMakeups.size}") }
                                }
                            }
                        }
                    )
                }
            }

            val filteredMakeups = when (selectedTab) {
                0 -> pendingMakeups.filter { it.status == MakeupExamStatus.PENDING }
                1 -> pendingMakeups.filter { it.status == MakeupExamStatus.SCHEDULED }
                2 -> pendingMakeups.filter { it.status == MakeupExamStatus.DONE }
                else -> emptyList()
            }

            if (filteredMakeups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("無相關資料", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(filteredMakeups) { makeup ->
                        MakeupExamCard(makeup, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MakeupExamCard(makeup: MakeupExam, viewModel: SubjectTeacherViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${makeup.classId} ${makeup.studentName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SuggestionChip(onClick = {}, label = { Text(makeup.status.name) })
            }
            Text("原因: ${makeup.reason}", style = MaterialTheme.typography.bodySmall)
            
            if (makeup.status == MakeupExamStatus.PENDING) {
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.saveMakeupExam(makeup.copy(status = MakeupExamStatus.SCHEDULED)) }) {
                        Text("安排補考")
                    }
                    OutlinedButton(onClick = { viewModel.saveMakeupExam(makeup.copy(status = MakeupExamStatus.WAIVED)) }) {
                        Text("免除補考")
                    }
                }
            } else if (makeup.status == MakeupExamStatus.SCHEDULED) {
                Text("補考日期: ${makeup.scheduledDate?.let { java.text.SimpleDateFormat("yyyy/MM/dd").format(it) } ?: "未排定"}")
                Button(
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = { viewModel.saveMakeupExam(makeup.copy(status = MakeupExamStatus.DONE)) }
                ) {
                    Text("輸入成績")
                }
            } else {
                Text("補考成績: ${makeup.makeupScore ?: "未輸入"}", fontWeight = FontWeight.Bold)
            }
        }
    }
}
