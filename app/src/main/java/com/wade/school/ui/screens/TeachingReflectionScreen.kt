package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.TeachingReflection
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingReflectionScreen(
    classId: String,
    viewModel: SubjectTeacherViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val reflections by viewModel.getReflectionsByClass(classId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 教學省思") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新增省思")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(reflections) { reflection ->
                ReflectionCard(reflection)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showAddDialog) {
        AddReflectionDialog(
            classId = classId,
            onDismiss = { showAddDialog = false },
            onSave = { reflection ->
                viewModel.saveReflection(reflection)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ReflectionCard(reflection: TeachingReflection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(reflection.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(SimpleDateFormat("yyyy/MM/dd").format(reflection.teachingDate), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            ReflectionSection("✅ 做得好的地方", reflection.whatWentWell)
            ReflectionSection("🔧 可以改進", reflection.whatToImprove)
            ReflectionSection("👥 學生反應", reflection.studentResponse)
            ReflectionSection("➡️ 下次調整", reflection.nextSteps)
        }
    }
}

@Composable
fun ReflectionSection(label: String, content: String) {
    if (content.isNotBlank()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AddReflectionDialog(classId: String, onDismiss: () -> Unit, onSave: (TeachingReflection) -> Unit) {
    var topic by remember { mutableStateOf("") }
    var well by remember { mutableStateOf("") }
    var improve by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增教學省思") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("本節主題") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = well, onValueChange = { well = it }, label = { Text("做得好的地方") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = improve, onValueChange = { improve = it }, label = { Text("可以改進") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = response, onValueChange = { response = it }, label = { Text("學生反應") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = next, onValueChange = { next = it }, label = { Text("下次調整") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    TeachingReflection(
                        classId = classId,
                        subjectName = "自訂科目",
                        teachingDate = System.currentTimeMillis(),
                        topic = topic,
                        whatWentWell = well,
                        whatToImprove = improve,
                        studentResponse = response,
                        nextSteps = next
                    )
                )
            }) {
                Text("儲 : 存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
