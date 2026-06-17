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
import com.wade.school.data.local.entity.LeaveRequest
import com.wade.school.data.local.entity.LeaveStatus
import com.wade.school.data.local.entity.LeaveType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestScreen(
    classId: String,
    viewModel: CounselorViewModel = viewModel(),
    onBack: () -> Unit
) {
    val pendingLeaves by viewModel.getPendingLeaves(classId).collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("待審核", "歷史記錄")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 假單審核") },
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
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                                if (index == 0 && pendingLeaves.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge { Text("${pendingLeaves.size}") }
                                }
                            }
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                PendingLeavesTab(pendingLeaves, viewModel)
            } else {
                // Historical leaves could be another flow from VM
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("歷史假單功能開發中", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun PendingLeavesTab(leaves: List<LeaveRequest>, viewModel: CounselorViewModel) {
    if (leaves.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("目前無待審核假單", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(leaves) { leave ->
                LeaveRequestCard(leave, viewModel)
            }
        }
    }
}

@Composable
fun LeaveRequestCard(leave: LeaveRequest, viewModel: CounselorViewModel) {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    var showReviewDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${leave.studentName} (${leave.studentId})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SuggestionChip(onClick = {}, label = { Text(leave.leaveType.label) })
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("日期: ${sdf.format(Date(leave.startDate))} - ${sdf.format(Date(leave.endDate))}", style = MaterialTheme.typography.bodySmall)
            Text("節次: ${leave.periodNames} (共 ${leave.totalPeriods} 節)", style = MaterialTheme.typography.bodySmall)
            Text("原因: ${leave.reason}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = { viewModel.reviewLeave(leave.id, LeaveStatus.REJECTED, "導師不核准") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("不核准")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.reviewLeave(leave.id, LeaveStatus.APPROVED, "准假") }) {
                    Text("核准")
                }
            }
        }
    }
}
