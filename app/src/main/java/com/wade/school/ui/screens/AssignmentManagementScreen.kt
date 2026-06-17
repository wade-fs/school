package com.wade.school.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.Assignment
import com.wade.school.data.local.entity.Submission
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
                    title = { Text("$classId 班 - 評量管理") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "發布評量")
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
                            Text("尚未發布任何評量，點擊右上角「+」開始。", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                onSave = { title, desc, dueDate, type ->
                    viewModel.createAssignment(classId, "物理", title, desc, dueDate, type)
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = assignment.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SuggestionChip(
                    onClick = { },
                    label = { Text(assignment.type, fontSize = 10.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        labelColor = when(assignment.type) {
                            "期中考", "期末考" -> MaterialTheme.colorScheme.error
                            "小考" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "截止日期: ${dateFormat.format(Date(assignment.dueDate))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val context = LocalContext.current
    val submissions by viewModel.getSubmissionsForAssignment(assignment.id).collectAsState(initial = emptyList())
    val isImporting by viewModel.isImporting.collectAsState()
    var selectedSubmission by remember { mutableStateOf<Submission?>(null) }

    val gradePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importGrades(assignment.id, context, it) }
    }

    // Mime types for CSV
    val csvMimeTypes = arrayOf("text/csv", "text/comma-separated-values", "application/csv", "*/*")
    
    var selectedFilter by remember { mutableStateOf("全部") }
    val filters = listOf("全部", "待繳", "已繳", "已批改", "遲交")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(assignment.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        IconButton(onClick = { gradePicker.launch(csvMimeTypes) }) {
                            Icon(Icons.Default.UploadFile, contentDescription = "匯入成績")
                        }
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
                    val overdue = submissions.count { it.status == "待繳" && (assignment.dueDate < System.currentTimeMillis()) }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$submitted / $total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(text = "已繳人數", style = MaterialTheme.typography.labelSmall)
                    }
                    if (overdue > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$overdue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text(text = "逾期未繳", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                if (submissions.any { it.status == "待繳" && (assignment.dueDate < System.currentTimeMillis()) }) {
                    Button(
                        onClick = { /* Prepare notification to Homeroom Teacher */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("通知導師未繳名單")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            val filteredSubmissions = if (selectedFilter == "全部") submissions else submissions.filter { it.status == selectedFilter }

            items(filteredSubmissions) { submission ->
                val isOverdue = submission.status == "待繳" && (assignment.dueDate < System.currentTimeMillis())
                ListItem(
                    headlineContent = { Text(submission.studentName) },
                    supportingContent = { Text("學號: ${submission.studentId}") },
                    trailingContent = {
                        Text(
                            text = if (isOverdue) "逾期" else submission.status,
                            color = when {
                                isOverdue -> MaterialTheme.colorScheme.error
                                submission.status == "已批改" -> Color(0xFF43A047)
                                submission.status == "已繳" -> MaterialTheme.colorScheme.primary
                                submission.status == "遲交" -> Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.outline
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
fun CreateAssignmentDialog(onDismiss: () -> Unit, onSave: (String, String, Long, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var daysPlus by remember { mutableStateOf("7") }
    var selectedType by remember { mutableStateOf("作業") }
    val types = listOf("作業", "小考", "期中考", "期末考", "其他")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("發布評量項目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("項目類型", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    types.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("評量名稱") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("說明") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = daysPlus, onValueChange = { daysPlus = it }, label = { Text("截止/考試天數 (從今日起)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val dueDate = System.currentTimeMillis() + (daysPlus.toLongOrNull() ?: 7) * 86400000L
                onSave(title, desc, dueDate, selectedType)
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
