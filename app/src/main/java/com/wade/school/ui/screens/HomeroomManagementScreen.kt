package com.wade.school.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.*
import java.text.SimpleDateFormat
import java.util.*

sealed class HomeroomFeature(val title: String, val icon: ImageVector, val id: String, val color: Color) {
    data object Checklist : HomeroomFeature("每日叮嚀", Icons.Default.FactCheck, "checklist", Color(0xFF4CAF50))
    data object Contact : HomeroomFeature("家長聯絡", Icons.Default.ContactPhone, "contact", Color(0xFF2196F3))
    data object Observation : HomeroomFeature("行為觀察", Icons.Default.Visibility, "observation", Color(0xFFFF9800))
    data object Cadre : HomeroomFeature("幹部名單", Icons.Default.Badge, "cadre", Color(0xFF9C27B0))
    data object Activity : HomeroomFeature("班級活動", Icons.Default.Event, "activity", Color(0xFFE91E63))
    data object Honor : HomeroomFeature("優良事蹟", Icons.Default.EmojiEvents, "honor", Color(0xFFFFC107))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeroomManagementScreen(
    classId: String,
    onBack: () -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    var activeFeature by remember { mutableStateOf<HomeroomFeature?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeFeature == null) "$classId 班級管理助手" else activeFeature!!.title) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeFeature == null) onBack() else activeFeature = null
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (activeFeature == null) {
                FeatureGrid(onFeatureSelect = { activeFeature = it })
            } else {
                when (activeFeature) {
                    HomeroomFeature.Checklist -> ChecklistTab(classId, viewModel)
                    HomeroomFeature.Contact -> ContactLogTab(classId, viewModel)
                    HomeroomFeature.Observation -> ObservationTab(classId, viewModel)
                    HomeroomFeature.Cadre -> CadreTab(classId, viewModel)
                    HomeroomFeature.Activity -> ActivityTab(classId, viewModel)
                    HomeroomFeature.Honor -> HonorTab(classId, viewModel)
                    null -> {} 
                }
            }
        }
    }
}

@Composable
fun FeatureGrid(onFeatureSelect: (HomeroomFeature) -> Unit) {
    val features = listOf(
        HomeroomFeature.Checklist, HomeroomFeature.Contact,
        HomeroomFeature.Observation, HomeroomFeature.Cadre,
        HomeroomFeature.Activity, HomeroomFeature.Honor
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(features) { feature ->
            FeatureCard(feature, onClick = { onFeatureSelect(feature) })
        }
    }
}

@Composable
fun FeatureCard(feature: HomeroomFeature, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = feature.color
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = feature.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ChecklistTab(classId: String, viewModel: CounselorViewModel) {
    val items by viewModel.getChecklist(classId).collectAsState(initial = emptyList())
    val students by viewModel.homeroomStudents.collectAsState()
    var newItemText by remember { mutableStateOf("") }
    var targetType by remember { mutableStateOf("Class") }
    var selectedStudents by remember { mutableStateOf(setOf<String>()) }
    var showStudentPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = newItemText,
            onValueChange = { newItemText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("新增叮嚀事項...") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = targetType == "Class", onClick = { targetType = "Class" }, label = { Text("全班") }, leadingIcon = { Icon(Icons.Default.Groups, null, Modifier.size(16.dp)) })
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(selected = targetType == "Students", onClick = { targetType = "Students" }, label = { Text(if (selectedStudents.isEmpty()) "指定學生" else "已選 ${selectedStudents.size} 人") }, leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(16.dp)) })
            if (targetType == "Students") IconButton(onClick = { showStudentPicker = true }) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                if (newItemText.isNotBlank()) {
                    val names = if (targetType == "Students") students.filter { it.studentId in selectedStudents }.joinToString(", ") { it.name } else null
                    viewModel.addChecklistItem(classId, newItemText, targetType, names)
                    newItemText = ""; selectedStudents = emptySet(); targetType = "Class"
                }
            }, enabled = newItemText.isNotBlank() && (targetType == "Class" || selectedStudents.isNotEmpty())) { Text("新增") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (item.isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.isDone, onCheckedChange = { viewModel.toggleChecklistItem(item) })
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (item.targetType == "Class") Icons.Default.Groups else Icons.Default.Person, null, Modifier.size(14.dp), tint = if (item.targetType == "Class") Color.Gray else MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (item.targetType == "Class") "全班" else item.assignedStudentNames ?: "", style = MaterialTheme.typography.labelSmall, color = if (item.targetType == "Class") Color.Gray else MaterialTheme.colorScheme.primary)
                            }
                            Text(text = item.content, style = MaterialTheme.typography.bodyLarge.copy(textDecoration = if (item.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null))
                            Text(text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.date)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        IconButton(onClick = { viewModel.deleteChecklistItem(item.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    if (showStudentPicker) {
        AlertDialog(onDismissRequest = { showStudentPicker = false }, title = { Text("選擇指定學生") }, text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(students) { student ->
                    val isSelected = student.studentId in selectedStudents
                    ListItem(headlineContent = { Text("${student.seatNo} 號 - ${student.name}") }, modifier = Modifier.clickable { selectedStudents = if (isSelected) selectedStudents - student.studentId else selectedStudents + student.studentId }, trailingContent = { Checkbox(checked = isSelected, onCheckedChange = null) })
                }
            }
        }, confirmButton = { TextButton(onClick = { showStudentPicker = false }) { Text("確定") } })
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
                            Text(log.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(log.contactDate)), style = MaterialTheme.typography.labelSmall)
                        }
                        Text("學生: ${log.studentName} | 對象: ${log.parentName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge { Text(log.channel) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("事由: ${log.reason}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(log.summary, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, null) }
    }

    if (showAddDialog) {
        var selectedStudent by remember { mutableStateOf<Student?>(null) }
        var title by remember { mutableStateOf("") }
        var parentName by remember { mutableStateOf("") }
        var channel by remember { mutableStateOf("電話") }
        var reason by remember { mutableStateOf("") }
        var summary by remember { mutableStateOf("") }
        var expandedStudent by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("新增聯絡紀錄") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expandedStudent, onExpandedChange = { expandedStudent = !expandedStudent }) {
                    OutlinedTextField(value = selectedStudent?.name ?: "選擇學生", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStudent) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expandedStudent, onDismissRequest = { expandedStudent = false }) {
                        students.forEach { student -> DropdownMenuItem(text = { Text(student.name) }, onClick = { selectedStudent = student; parentName = student.guardianName ?: ""; expandedStudent = false }) }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("主題") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = parentName, onValueChange = { parentName = it }, label = { Text("聯絡對象") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("電話", "Line", "親晤").forEach { c -> FilterChip(selected = channel == c, onClick = { channel = c }, label = { Text(c) }) } }
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("事由") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("備註") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        }, confirmButton = { TextButton(onClick = { if (selectedStudent != null && title.isNotBlank()) { viewModel.addContactLog(ParentContactLog(studentId = selectedStudent!!.studentId, studentName = selectedStudent!!.name, parentName = parentName, title = title, channel = channel, reason = reason, summary = summary)); showAddDialog = false } }) { Text("儲存") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservationTab(classId: String, viewModel: CounselorViewModel) {
    val observations by viewModel.getAllObservations().collectAsState(initial = emptyList())
    val students by viewModel.homeroomStudents.collectAsState()
    var sortOrder by remember { mutableStateOf("Time") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val sortedObservations = remember(observations, students, sortOrder) {
        when (sortOrder) {
            "Name" -> observations.sortedBy { it.studentName }
            "Seat" -> observations.sortedBy { obs -> students.find { it.studentId == obs.studentId }?.seatNo ?: 999 }
            else -> observations.sortedByDescending { it.date }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text("排序：", style = MaterialTheme.typography.labelMedium)
                Box {
                    AssistChip(onClick = { showSortMenu = true }, label = { Text(when(sortOrder) { "Name" -> "姓名"; "Seat" -> "座號"; else -> "時間" }) }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) })
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(text = { Text("時間") }, onClick = { sortOrder = "Time"; showSortMenu = false })
                        DropdownMenuItem(text = { Text("姓名") }, onClick = { sortOrder = "Name"; showSortMenu = false })
                        DropdownMenuItem(text = { Text("座號") }, onClick = { sortOrder = "Seat"; showSortMenu = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sortedObservations) { obs ->
                    val student = students.find { it.studentId == obs.studentId }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = if (student != null) "${student.seatNo} 號 - ${obs.studentName}" else obs.studentName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                AssistChip(onClick = {}, label = { Text(obs.category) })
                                Spacer(modifier = Modifier.weight(1f))
                                Text(SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(obs.date)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Text(obs.content, style = MaterialTheme.typography.bodyMedium)
                            obs.tag?.let { SuggestionChip(onClick = {}, label = { Text(it) }, colors = SuggestionChipDefaults.suggestionChipColors(labelColor = if (it == "正向") Color(0xFF2E7D32) else Color(0xFFD32F2F))) }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, null) }
    }

    if (showAddDialog) {
        var selectedStudent by remember { mutableStateOf<Student?>(null) }
        var category by remember { mutableStateOf("常規") }
        var content by remember { mutableStateOf("") }
        var tag by remember { mutableStateOf("正向") }
        var expandedStudent by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("新增觀察") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expandedStudent, onExpandedChange = { expandedStudent = !expandedStudent }) {
                    OutlinedTextField(value = selectedStudent?.name ?: "選擇學生", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStudent) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expandedStudent, onDismissRequest = { expandedStudent = false }) {
                        students.forEach { student -> DropdownMenuItem(text = { Text("${student.seatNo} 號 - ${student.name}") }, onClick = { selectedStudent = student; expandedStudent = false }) }
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) { listOf("學習", "常規", "人際", "優點").forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }); Spacer(Modifier.width(4.dp)) } }
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("內容") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Row { listOf("正向", "待改進").forEach { t -> FilterChip(selected = tag == t, onClick = { tag = t }, label = { Text(t) }); Spacer(Modifier.width(8.dp)) } }
            }
        }, confirmButton = { TextButton(onClick = { if (selectedStudent != null && content.isNotBlank()) { viewModel.addObservation(BehaviorObservations(studentId = selectedStudent!!.studentId, studentName = selectedStudent!!.name, category = category, content = content, tag = tag)); showAddDialog = false } }) { Text("儲存") } })
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
                ListItem(headlineContent = { Text(cadre.position, fontWeight = FontWeight.Bold) }, supportingContent = { Text(cadre.studentName) }, trailingContent = { IconButton(onClick = { viewModel.deleteCadre(cadre.id) }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) } })
                HorizontalDivider()
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.GroupAdd, null) }
    }

    if (showAddDialog) {
        var position by remember { mutableStateOf("") }
        var selectedStudent by remember { mutableStateOf<Student?>(null) }
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("設定幹部") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = position, onValueChange = { position = it }, label = { Text("職稱") }, modifier = Modifier.fillMaxWidth())
                LazyColumn(modifier = Modifier.height(200.dp)) { items(students) { student -> ListItem(headlineContent = { Text(student.name) }, modifier = Modifier.clickable { selectedStudent = student }, trailingContent = { if (selectedStudent == student) Icon(Icons.Default.Check, null) }) } }
            }
        }, confirmButton = { TextButton(onClick = { if (position.isNotBlank() && selectedStudent != null) { viewModel.upsertCadre(ClassCadre(classId = classId, position = position, studentId = selectedStudent!!.studentId, studentName = selectedStudent!!.name)); showAddDialog = false } }) { Text("確定") } })
    }
}

@Composable
fun ActivityTab(classId: String, viewModel: CounselorViewModel) {
    val activities by viewModel.getActivities(classId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(activities) { activity ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(activity.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(activity.date)), style = MaterialTheme.typography.labelSmall)
                        }
                        activity.location?.let { Text("地點: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                        Text(activity.description, style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { viewModel.deleteActivity(activity.id) }, modifier = Modifier.align(Alignment.End)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, null) }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("紀錄班級活動") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("活動名稱") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("地點") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("內容描述") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        }, confirmButton = { TextButton(onClick = { if (title.isNotBlank()) { viewModel.addActivity(ClassActivity(classId = classId, title = title, location = location, description = desc)); showAddDialog = false } }) { Text("儲存") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HonorTab(classId: String, viewModel: CounselorViewModel) {
    val honors by viewModel.getHonors(classId).collectAsState(initial = emptyList())
    val students by viewModel.homeroomStudents.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(honors) { honor ->
                Card(
                    modifier = Modifier.fillMaxWidth(), 
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, null, tint = Color(0xFFFFC107))
                            Spacer(Modifier.width(8.dp))
                            Text(honor.awardTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.weight(1f))
                            Badge { Text(honor.level) }
                        }
                        Text("獲獎對象: ${honor.studentName ?: "全班"}", style = MaterialTheme.typography.bodyMedium)
                        Text(SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(honor.awardDate)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        IconButton(onClick = { viewModel.deleteHonor(honor.id) }, modifier = Modifier.align(Alignment.End)) { 
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) 
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { 
            Icon(Icons.Default.EmojiEvents, null) 
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var level by remember { mutableStateOf("校內") }
        var recipientType by remember { mutableStateOf("Class") } // "Class", "Students", "Group"
        var selectedStudents by remember { mutableStateOf(setOf<String>()) }
        var groupName by remember { mutableStateOf("") }
        var showStudentPicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新增優良事蹟") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("獎項名稱 (如：大隊接力第一名)") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("獲獎對象:", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        FilterChip(
                            selected = recipientType == "Class", 
                            onClick = { recipientType = "Class" }, 
                            label = { Text("全班") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = recipientType == "Students", 
                            onClick = { recipientType = "Students" }, 
                            label = { Text(if (selectedStudents.isEmpty()) "特定學生" else "已選 ${selectedStudents.size} 人") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = recipientType == "Group", 
                            onClick = { recipientType = "Group" }, 
                            label = { Text("團體/小組") }
                        )
                    }

                    when (recipientType) {
                        "Students" -> {
                            OutlinedButton(onClick = { showStudentPicker = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.PersonAdd, null)
                                Spacer(Modifier.width(8.dp))
                                Text("選擇名單")
                            }
                            if (selectedStudents.isNotEmpty()) {
                                Text(
                                    "名單: " + students.filter { it.studentId in selectedStudents }.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        "Group" -> {
                            OutlinedTextField(
                                value = groupName, 
                                onValueChange = { groupName = it }, 
                                label = { Text("團體名稱 (如：合唱團小組)") }, 
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Text("獲獎等級:", style = MaterialTheme.typography.labelMedium)
                    Row {
                        listOf("校內", "全縣", "全國").forEach { l ->
                            FilterChip(selected = level == l, onClick = { level = l }, label = { Text(l) })
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        val finalRecipientName = when (recipientType) {
                            "Class" -> "全班"
                            "Students" -> students.filter { it.studentId in selectedStudents }.joinToString(", ") { it.name }
                            "Group" -> groupName
                            else -> "全班"
                        }
                        
                        viewModel.addHonor(ClassHonor(
                            classId = classId,
                            studentId = if (recipientType == "Students") selectedStudents.joinToString(",") else null,
                            studentName = finalRecipientName,
                            awardTitle = title,
                            level = level,
                            category = "其他"
                        ))
                        showAddDialog = false
                    }
                }) { Text("儲存") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )

        if (showStudentPicker) {
            AlertDialog(
                onDismissRequest = { showStudentPicker = false },
                title = { Text("選擇獲獎學生") },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(students) { student ->
                            val isSelected = student.studentId in selectedStudents
                            ListItem(
                                headlineContent = { Text("${student.seatNo} 號 - ${student.name}") },
                                modifier = Modifier.clickable {
                                    selectedStudents = if (isSelected) selectedStudents - student.studentId else selectedStudents + student.studentId
                                },
                                trailingContent = { Checkbox(checked = isSelected, onCheckedChange = null) }
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showStudentPicker = false }) { Text("確定") } }
            )
        }
    }
}
