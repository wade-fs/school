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
import com.wade.school.data.local.entity.ParentTeacherConference
import com.wade.school.data.local.entity.Student
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentConferenceScreen(
    classId: String,
    viewModel: CounselorViewModel = viewModel(),
    onBack: () -> Unit
) {
    val conferences by viewModel.getConferencesByClass(classId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 親師座談紀錄") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新增紀錄")
            }
        }
    ) { padding ->
        if (conferences.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("目前尚無座談紀錄", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(conferences) { conf ->
                    ConferenceCard(conf)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddConferenceDialog(
            classId = classId,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onSave = { record ->
                viewModel.saveConference(record)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ConferenceCard(conf: ParentTeacherConference) {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${conf.studentName} 家長座談", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(sdf.format(Date(conf.conferenceDate)), style = MaterialTheme.typography.labelSmall)
            }
            Text("出席人員: ${conf.attendees}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            ConferenceSection("學業討論", conf.academicDiscussion)
            ConferenceSection("行為品德", conf.behaviorDiscussion)
            ConferenceSection("家長反映", conf.parentConcerns)
            ConferenceSection("後續追蹤", conf.followUpActions)
            
            if (conf.followUpDate != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.Event, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(" 追蹤日期: ${sdf.format(Date(conf.followUpDate!!))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun ConferenceSection(label: String, content: String) {
    if (content.isNotBlank()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConferenceDialog(
    classId: String,
    viewModel: CounselorViewModel,
    onDismiss: () -> Unit,
    onSave: (ParentTeacherConference) -> Unit
) {
    val students by viewModel.homeroomStudents.collectAsState()
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var attendees by remember { mutableStateOf("") }
    var academic by remember { mutableStateOf("") }
    var behavior by remember { mutableStateOf("") }
    var concerns by remember { mutableStateOf("") }
    var followUp by remember { mutableStateOf("") }
    var expandedStudent by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增座談紀錄") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 450.dp)) {
                item {
                    ExposedDropdownMenuBox(expanded = expandedStudent, onExpandedChange = { expandedStudent = !expandedStudent }) {
                        OutlinedTextField(
                            value = selectedStudent?.name ?: "選擇學生",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("學生姓名") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStudent) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedStudent, onDismissRequest = { expandedStudent = false }) {
                            students.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedStudent = s; expandedStudent = false })
                            }
                        }
                    }
                    OutlinedTextField(value = attendees, onValueChange = { attendees = it }, label = { Text("出席人員") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = academic, onValueChange = { academic = it }, label = { Text("學業討論") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = behavior, onValueChange = { behavior = it }, label = { Text("行為品德") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = concerns, onValueChange = { concerns = it }, label = { Text("家長反映事項") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = followUp, onValueChange = { followUp = it }, label = { Text("後續追蹤事項") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedStudent?.let { s ->
                        onSave(ParentTeacherConference(
                            studentId = s.studentId,
                            studentName = s.name,
                            classId = classId,
                            conferenceDate = System.currentTimeMillis(),
                            attendees = attendees,
                            academicDiscussion = academic,
                            behaviorDiscussion = behavior,
                            parentConcerns = concerns,
                            followUpActions = followUp
                        ))
                    }
                },
                enabled = selectedStudent != null
            ) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
