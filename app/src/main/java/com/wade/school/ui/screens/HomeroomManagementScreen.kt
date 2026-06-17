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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    onNavigate: (String) -> Unit = {},
    viewModel: CounselorViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 班級管理助手") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val groups = com.wade.school.ui.data.FeatureData.getFeaturesForRole("homeroom")
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                groups.forEach { group ->
                    item {
                        Text(text = group.groupTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(group.items.chunked(2)) { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            pair.forEach { feature ->
                                FeatureCard(feature, modifier = Modifier.weight(1f)) { route ->
                                    val finalRoute = if (route.contains("?")) {
                                        "$route&classId=$classId"
                                    } else {
                                        "$route?classId=$classId"
                                    }
                                    onNavigate(finalRoute)
                                }
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureCard(feature: com.wade.school.ui.data.FeatureItem, modifier: Modifier = Modifier, onClick: (String) -> Unit) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick(feature.route) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when(feature.iconName) {
                    "dashboard" -> Icons.Default.Dashboard
                    "contact_page" -> Icons.Default.ContactPage
                    "grid_view" -> Icons.Default.GridView
                    "how_to_reg" -> Icons.Default.HowToReg
                    "pending_actions" -> Icons.Default.PendingActions
                    "warning" -> Icons.Default.Warning
                    "bar_chart" -> Icons.Default.BarChart
                    "gavel" -> Icons.Default.Gavel
                    "rate_review" -> Icons.Default.RateReview
                    "edit_note" -> Icons.Default.EditNote
                    "book" -> Icons.Default.Book
                    "contact_phone" -> Icons.Default.ContactPhone
                    "groups" -> Icons.Default.Groups
                    "campaign" -> Icons.Default.Campaign
                    "badge" -> Icons.Default.Badge
                    "account_balance" -> Icons.Default.AccountBalance
                    "event" -> Icons.Default.Event
                    "emoji_events" -> Icons.Default.EmojiEvents
                    else -> Icons.Default.Star
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = feature.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (feature.badge != null) {
                Badge { Text(feature.badge) }
            }
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
        var position by remember { mutableStateOf("") }; var selectedStudent by remember { mutableStateOf<Student?>(null) }
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("設定幹部") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = position, onValueChange = { position = it }, label = { Text("職稱") }, modifier = Modifier.fillMaxWidth())
                LazyColumn(modifier = Modifier.height(200.dp)) { items(students) { student -> ListItem(headlineContent = { Text(student.name) }, modifier = Modifier.clickable { selectedStudent = student }, trailingContent = { if (selectedStudent == student) Icon(Icons.Default.Check, null) }) } }
            }
        }, confirmButton = { TextButton(onClick = { if (position.isNotBlank() && selectedStudent != null) { viewModel.upsertCadre(ClassCadre(classId = classId, position = position, studentId = selectedStudent!!.studentId, studentName = selectedStudent!!.name)); showAddDialog = false } }) { Text("確定") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTab(classId: String, viewModel: CounselorViewModel) {
    val activities by viewModel.getActivities(classId).collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<ClassActivity?>(null) }
    val localContext = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(activities) { activity ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { activityToEdit = activity },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activity.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            
                            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                            val dateText = if (activity.startDate == activity.endDate) {
                                sdf.format(Date(activity.startDate))
                            } else {
                                "${sdf.format(Date(activity.startDate))} ~ ${sdf.format(Date(activity.endDate))}"
                            }
                            Text(dateText, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        
                        IconButton(onClick = { viewModel.deleteActivity(activity.id) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, null) }
    }

    if (showAddDialog || activityToEdit != null) {
        val editing = activityToEdit
        var title by remember(editing) { mutableStateOf(editing?.title ?: "") }
        var location by remember(editing) { mutableStateOf(editing?.location ?: "") }
        var locationUrl by remember(editing) { mutableStateOf(editing?.locationUrl ?: "") }
        var desc by remember(editing) { mutableStateOf(editing?.description ?: "") }
        
        val datePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = editing?.startDate,
            initialSelectedEndDateMillis = editing?.endDate
        )
        var showDatePicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false; activityToEdit = null },
            title = { Text(if (editing == null) "紀錄班級活動" else "編輯活動詳情") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("活動名稱") }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        val start = datePickerState.selectedStartDateMillis
                        val end = datePickerState.selectedEndDateMillis
                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                        Text(if (start != null && end != null) "${sdf.format(Date(start))} ~ ${sdf.format(Date(end))}" else "選擇日期範圍")
                    }
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("地點名稱") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = locationUrl, onValueChange = { locationUrl = it }, label = { Text("Google Map 連結") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("內容描述/備註 (如店家電話)") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        val act = ClassActivity(
                            id = editing?.id ?: 0,
                            classId = classId,
                            title = title,
                            startDate = datePickerState.selectedStartDateMillis ?: System.currentTimeMillis(),
                            endDate = datePickerState.selectedEndDateMillis ?: datePickerState.selectedStartDateMillis ?: System.currentTimeMillis(),
                            location = location,
                            locationUrl = locationUrl.ifBlank { null },
                            description = desc
                        )
                        if (editing == null) viewModel.addActivity(act) else viewModel.updateActivity(act)
                        showAddDialog = false; activityToEdit = null
                    }
                }) { Text("儲存") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; activityToEdit = null }) { Text("取消") } }
        )

        if (showDatePicker) {
            DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("確定") } }) {
                DateRangePicker(state = datePickerState, modifier = Modifier.height(400.dp))
            }
        }
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
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
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
                        IconButton(onClick = { viewModel.deleteHonor(honor.id) }, modifier = Modifier.align(Alignment.End)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.EmojiEvents, null) }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }; var level by remember { mutableStateOf("校內") }; var recipientType by remember { mutableStateOf("Class") }; var selectedStudents by remember { mutableStateOf(setOf<String>()) }; var groupName by remember { mutableStateOf("") }; var showStudentPicker by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { showAddDialog = false }, title = { Text("新增優良事蹟") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("獎項名稱") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    FilterChip(selected = recipientType == "Class", onClick = { recipientType = "Class" }, label = { Text("全班") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = recipientType == "Students", onClick = { recipientType = "Students" }, label = { Text(if (selectedStudents.isEmpty()) "特定學生" else "已選 ${selectedStudents.size} 人") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = recipientType == "Group", onClick = { recipientType = "Group" }, label = { Text("團體/小組") })
                }
                if (recipientType == "Students") {
                    OutlinedButton(onClick = { showStudentPicker = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("選擇名單") }
                    if (selectedStudents.isNotEmpty()) Text("名單: " + students.filter { it.studentId in selectedStudents }.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else if (recipientType == "Group") OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("團體名稱") }, modifier = Modifier.fillMaxWidth())
                Row { listOf("校內", "全縣", "全國").forEach { l -> FilterChip(selected = level == l, onClick = { level = l }, label = { Text(l) }); Spacer(Modifier.width(8.dp)) } }
            }
        }, confirmButton = { TextButton(onClick = { if (title.isNotBlank()) { val finalRecipientName = when (recipientType) { "Class" -> "全班"; "Students" -> students.filter { it.studentId in selectedStudents }.joinToString(", ") { it.name }; "Group" -> groupName; else -> "全班" }; viewModel.addHonor(ClassHonor(classId = classId, studentId = if (recipientType == "Students") selectedStudents.joinToString(",") else null, studentName = finalRecipientName, awardTitle = title, level = level, category = "其他")); showAddDialog = false } }) { Text("儲存") } })
        if (showStudentPicker) AlertDialog(onDismissRequest = { showStudentPicker = false }, title = { Text("選擇獲獎學生") }, text = { LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) { items(students) { student -> val isSelected = student.studentId in selectedStudents; ListItem(headlineContent = { Text("${student.seatNo} 號 - ${student.name}") }, modifier = Modifier.clickable { selectedStudents = if (isSelected) selectedStudents - student.studentId else selectedStudents + student.studentId }, trailingContent = { Checkbox(checked = isSelected, onCheckedChange = null) }) } } }, confirmButton = { TextButton(onClick = { showStudentPicker = false }) { Text("確定") } })
    }
}
