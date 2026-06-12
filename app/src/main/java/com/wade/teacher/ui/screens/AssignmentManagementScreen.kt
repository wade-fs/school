package com.wade.teacher.ui.screens

import androidx.compose.foundation.clickable
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
import com.wade.teacher.data.local.entity.Assignment
import com.wade.teacher.data.local.entity.Submission
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentManagementScreen(
    classId: String,
    onBack: () -> Unit,
    viewModel: SubjectTeacherViewModel = viewModel()
) {
    val assignments by viewModel.getAssignmentsForClass(classId).collectAsState(initial = emptyList())
    var selectedAssignment by remember { mutableStateOf<Assignment?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (selectedAssignment != null) {
        SubmissionListScreen(
            assignment = selectedAssignment!!,
            onBack = { selectedAssignment = null },
            viewModel = viewModel
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("$classId 班 - 作業管理") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "派發作業")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (assignments.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("尚未派發作業，點擊右上角「+」開始。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(assignments) { assignment ->
                    AssignmentCard(assignment, onClick = { selectedAssignment = assignment })
                }
            }
        }

        if (showCreateDialog) {
            CreateAssignmentDialog(
                onDismiss = { showCreateDialog = false },
                onSave = { title, desc, dueDate ->
                    viewModel.createAssignment(classId, "物理", title, desc, dueDate)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun AssignmentCard(assignment: Assignment, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = assignment.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "截止日期: ${dateFormat.format(Date(assignment.dueDate))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = assignment.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionListScreen(
    assignment: Assignment,
    onBack: () -> Unit,
    viewModel: SubjectTeacherViewModel
) {
    val submissions by viewModel.getSubmissionsForAssignment(assignment.id).collectAsState(initial = emptyList())
    var selectedSubmission by remember { mutableStateOf<Submission?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(assignment.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text("繳交狀況統計", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val submitted = submissions.count { it.status != "待繳" }
                    val total = submissions.size
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$submitted / $total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(text = "繳交人數", style = MaterialTheme.typography.labelSmall)
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(submissions) { submission ->
                ListItem(
                    headlineContent = { Text(submission.studentName) },
                    supportingContent = { Text("學號: ${submission.studentId}") },
                    trailingContent = {
                        Text(
                            text = submission.status,
                            color = when(submission.status) {
                                "已批改" -> Color(0xFF43A047)
                                "已繳" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.clickable { selectedSubmission = submission }
                )
            }
        }
    }

    if (selectedSubmission != null) {
        GradeDialog(
            submission = selectedSubmission!!,
            onDismiss = { selectedSubmission = null },
            onSave = { score, feedback ->
                viewModel.gradeSubmission(selectedSubmission!!, score, feedback)
                selectedSubmission = null
            }
        )
    }
}

@Composable
fun CreateAssignmentDialog(onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var daysPlus by remember { mutableStateOf("7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("派發新作業") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("作業標題") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("作業說明") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(value = daysPlus, onValueChange = { daysPlus = it }, label = { Text("截止天數 (從今日起)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val dueDate = System.currentTimeMillis() + (daysPlus.toLongOrNull() ?: 7) * 86400000L
                onSave(title, desc, dueDate)
            }) { Text("發布") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun GradeDialog(submission: Submission, onDismiss: () -> Unit, onSave: (Int, String) -> Unit) {
    var score by remember { mutableStateOf(submission.score?.toString() ?: "100") }
    var feedback by remember { mutableStateOf(submission.feedback ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批改: ${submission.studentName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = score, onValueChange = { score = it }, label = { Text("評分 (0-100)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = feedback, onValueChange = { feedback = it }, label = { Text("批改回饋") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(score.toIntOrNull() ?: 0, feedback)
            }) { Text("儲存成績") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
