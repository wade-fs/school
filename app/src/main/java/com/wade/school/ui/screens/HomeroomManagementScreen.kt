package com.wade.school.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeroomManagementScreen(
    classId: String,
    onBack: () -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("每日叮嚀", "家長聯絡", "行為觀察", "幹部名單")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 班級管理助理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ChecklistTab(classId, viewModel)
                1 -> ContactLogTab(classId, viewModel)
                2 -> ObservationTab(classId, viewModel)
                3 -> CadreTab(classId, viewModel)
            }
        }
    }
}

@Composable
fun ChecklistTab(classId: String, viewModel: CounselorViewModel) {
    val items by viewModel.getChecklist(classId).collectAsState(initial = emptyList())
    var newItemText by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("新增叮嚀事項...") },
                singleLine = true
            )
            IconButton(onClick = {
                if (newItemText.isNotBlank()) {
                    viewModel.addChecklistItem(classId, newItemText)
                    newItemText = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = item.isDone, onCheckedChange = { viewModel.toggleChecklistItem(item) })
                        Text(
                            text = item.content,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = if (item.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                        )
                        IconButton(onClick = { viewModel.deleteChecklistItem(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactLogTab(classId: String, viewModel: CounselorViewModel) {
    val logs by viewModel.getAllContactLogs().collectAsState(initial = emptyList())
    val students by viewModel.homeroomStudents.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(log.studentName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge { Text(log.channel) }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(log.contactDate)), style = MaterialTheme.typography.labelSmall)
                        }
                        Text("事由: ${log.reason}", style = MaterialTheme.typography.bodyMedium)
                        Text(log.summary, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "新增紀錄")
        }
    }

    if (showAddDialog) {
        var selectedStudent by remember { mutableStateOf<Student?>(null) }
        var channel by remember { mutableStateOf("電話") }
        var reason by remember { mutableStateOf("") }
        var summary by remember { mutableStateOf("") }
        var expandedStudent by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新增聯絡紀錄") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandedStudent,
                        onExpandedChange = { expandedStudent = !expandedStudent }
                    ) {
                        OutlinedTextField(
                            value = selectedStudent?.name ?: "選擇學生",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStudent) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStudent,
                            onDismissRequest = { expandedStudent = false }
                        ) {
                            students.forEach { student ->
                                DropdownMenuItem(
                                    text = { Text(student.name) },
                                    onClick = {
                                        selectedStudent = student
                                        expandedStudent = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("電話", "Line", "親晤").forEach { c ->
                            FilterChip(
                                selected = channel == c,
                                onClick = { channel = c },
                                label = { Text(c) }
                            )
                        }
                    }

                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("事由") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("內容備註") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedStudent != null) {
                        viewModel.addContactLog(ParentContactLog(
                            studentId = selectedStudent!!.studentId,
                            studentName = selectedStudent!!.name,
                            channel = channel,
                            reason = reason,
                            summary = summary
                        ))
                        showAddDialog = false
                    }
                }) { Text("儲存") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
fun ObservationTab(classId: String, viewModel: CounselorViewModel) {
    val observations by viewModel.getAllObservations().collectAsState(initial = emptyList())
    val students by viewModel.homeroomStudents.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(observations) { obs ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(obs.studentName, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(onClick = {}, label = { Text(obs.category) })
                            Spacer(modifier = Modifier.weight(1f))
                            Text(SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(obs.date)), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(obs.content, style = MaterialTheme.typography.bodyMedium)
                        obs.tag?.let { Text("#$it", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Assignment, contentDescription = "新增觀察")
        }
    }

    if (showAddDialog) {
        var selectedStudent by remember { mutableStateOf<Student?>(null) }
        var category by remember { mutableStateOf("常規") }
        var content by remember { mutableStateOf("") }
        var tag by remember { mutableStateOf("正向") }
        var expandedStudent by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新增觀察紀錄") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Student Selector (same as above)
                    OutlinedTextField(
                        value = selectedStudent?.name ?: "選擇學生",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().clickable { expandedStudent = true }
                    )
                    
                    Row(modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                        listOf("學習", "常規", "人際", "優點").forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("觀察內容") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    
                    Row {
                        FilterChip(selected = tag == "正向", onClick = { tag = "正向" }, label = { Text("正向") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = tag == "待改進", onClick = { tag = "待改進" }, label = { Text("待改進") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedStudent != null) {
                        viewModel.addObservation(BehaviorObservations(
                            studentId = selectedStudent!!.studentId,
                            studentName = selectedStudent!!.name,
                            category = category,
                            content = content,
                            tag = tag
                        ))
                        showAddDialog = false
                    }
                }) { Text("儲存") }
            }
        )
    }
}

@Composable
fun CadreTab(classId: String, viewModel: CounselorViewModel) {
    val cadres by viewModel.getCadres(classId).collectAsState(initial = emptyList())
    val students by viewModel.homeroomStudents.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cadres) { cadre ->
                ListItem(
                    headlineContent = { Text(cadre.position, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(cadre.studentName) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteCadre(cadre.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider()
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.GroupAdd, contentDescription = "新增幹部")
        }
    }

    if (showAddDialog) {
        var position by remember { mutableStateOf("") }
        var selectedStudent by remember { mutableStateOf<Student?>(null) }
        var expandedStudent by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("設定班級幹部") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = position, onValueChange = { position = it }, label = { Text("職稱 (如：班長)") }, modifier = Modifier.fillMaxWidth())
                    
                    // Student selector (simplified for brevity)
                    Text("選擇學生：", style = MaterialTheme.typography.labelSmall)
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(students) { student ->
                            ListItem(
                                headlineContent = { Text(student.name) },
                                modifier = Modifier.clickable { selectedStudent = student },
                                trailingContent = { if (selectedStudent == student) Icon(Icons.Default.Check, contentDescription = null) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (position.isNotBlank() && selectedStudent != null) {
                        viewModel.upsertCadre(ClassCadre(
                            classId = classId,
                            position = position,
                            studentId = selectedStudent!!.studentId,
                            studentName = selectedStudent!!.name
                        ))
                        showAddDialog = false
                    }
                }) { Text("確定") }
            }
        )
    }
}
