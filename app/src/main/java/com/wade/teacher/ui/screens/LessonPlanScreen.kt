package com.wade.teacher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.teacher.data.local.entity.LessonPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlanScreen(
    onBack: () -> Unit,
    viewModel: SubjectTeacherViewModel = viewModel()
) {
    val lessonPlans by viewModel.allLessonPlans.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("108 課綱教案庫") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "新增教案")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (lessonPlans.isEmpty()) {
                item {
                    Text("尚未建立教案，點擊右上角「+」開始。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(lessonPlans) { plan ->
                LessonPlanCard(plan)
            }
        }
    }

    if (showAddDialog) {
        AddLessonPlanDialog(
            onDismiss = { showAddDialog = false },
            onSave = { plan ->
                viewModel.saveLessonPlan(plan, emptyList())
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LessonPlanCard(plan: LessonPlan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = plan.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SuggestionChip(
                    onClick = { },
                    label = { Text("${plan.grade}年級", fontSize = 10.sp) }
                )
            }
            Text(text = plan.subjectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Competency Tags
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                plan.competencies.split(",").forEach { tag ->
                    if (tag.isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(tag, fontSize = 9.sp) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = plan.content, style = MaterialTheme.typography.bodySmall, maxLines = 3)
        }
    }
}

@Composable
fun AddLessonPlanDialog(onDismiss: () -> Unit, onSave: (LessonPlan) -> Unit) {
    var topic by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("物理") }
    var grade by remember { mutableStateOf("10") }
    var competencies by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增 108 教案") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("主題/單元") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("科目") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("年級") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = competencies, onValueChange = { competencies = it }, label = { Text("核心素養 (以逗號分隔)") }, placeholder = { Text("例如：系統思考, 溝通互動") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("教學簡述") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(LessonPlan(topic = topic, subjectName = subject, grade = grade.toIntOrNull() ?: 10, competencies = competencies, content = content))
            }) { Text("儲存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
