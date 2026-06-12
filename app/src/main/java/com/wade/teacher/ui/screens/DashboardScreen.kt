package com.wade.teacher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    role: String, 
    onBack: () -> Unit, 
    onNavigateToStudent: (String, String) -> Unit = { _, _ -> },
    onNavigateToMoodCheck: () -> Unit = {},
    onNavigateToResources: () -> Unit = {},
    onNavigateToLessonPlans: () -> Unit = {},
    onNavigateToTagging: (String) -> Unit = {}
) {
    val viewModel: CounselorViewModel = viewModel()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val roleTitle = roles.find { it.id == role }?.title ?: "未知角色"
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        val moeSchools by viewModel.moeSchools.collectAsState()
        val isFetching by viewModel.isFetchingSchools.collectAsState()
        val periodTimes by viewModel.periodTimes.collectAsState(initial = emptyList())
        
        SchoolSettingsDialog(
            config = schoolConfig,
            moeSchools = moeSchools,
            isFetching = isFetching,
            periodTimes = periodTimes,
            onDismiss = { showSettingsDialog = false },
            onSave = { name, type, website, times ->
                viewModel.updateSchoolConfig(name, type, website)
                viewModel.updatePeriodTimes(times)
                showSettingsDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(text = schoolConfig.schoolName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        val academicInfo = "${com.wade.teacher.util.AcademicUtils.getAcademicString()} ($roleTitle)"
                        val websiteInfo = schoolConfig.schoolWebsite?.let { " | $it" } ?: ""
                        Text(text = academicInfo + websiteInfo, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "學校設定")
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Person, contentDescription = "個人資料")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("首頁") },
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                )
                if (role != "student" && role != "parent") {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = { Text("科任班級") },
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 }
                    )
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = null) },
                    label = { Text("互動") },
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    when (role) {
                        "counseling" -> CounselingDashboard(onNavigateToStudent, onNavigateToMoodCheck, onNavigateToResources, viewModel)
                        "subject" -> SubjectTeacherDashboard(
                            onNavigateToLessonPlans = onNavigateToLessonPlans,
                            onNavigateToTagging = onNavigateToTagging
                        )
                        else -> RoleFeatureContent(role = role)
                    }
                }
                1 -> SubjectClassSwitcher()
                2 -> InteractionHub()
            }
        }
    }
}

@Composable
fun SchoolSettingsDialog(
    config: com.wade.teacher.data.local.entity.SchoolConfig,
    moeSchools: List<MoeSchool>,
    isFetching: Boolean,
    periodTimes: List<com.wade.teacher.data.local.entity.PeriodTime>,
    onDismiss: () -> Unit,
    onSave: (String, com.wade.teacher.data.local.entity.SchoolType, String?, List<com.wade.teacher.data.local.entity.PeriodTime>) -> Unit
) {
    var name by remember { mutableStateOf(config.schoolName) }
    var selectedType by remember { mutableStateOf(config.schoolType) }
    var website by remember { mutableStateOf(config.schoolWebsite) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Period times local state
    var editablePeriodTimes by remember(periodTimes) { mutableStateOf(periodTimes) }

    val filteredSchools = if (searchQuery.length >= 2) {
        moeSchools.filter { it.name.contains(searchQuery) }
    } else emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("全校性設定") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("校名") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    if (website != null) {
                        Text("網站: $website", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("快速從教育部名單挑選 (輸入關鍵字):", style = MaterialTheme.typography.labelMedium)
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("搜尋學校") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isFetching) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    )
                }

                if (filteredSchools.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            LazyColumn {
                                items(filteredSchools) { school ->
                                    ListItem(
                                        headlineContent = { Text(school.name, fontSize = 14.sp) },
                                        supportingContent = { Text(school.city, fontSize = 12.sp) },
                                        modifier = Modifier.clickable {
                                            name = school.name
                                            website = school.website
                                            searchQuery = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("學校類型", style = MaterialTheme.typography.labelLarge)
                    com.wade.teacher.data.local.entity.SchoolType.values().forEach { type ->
                        val label = when (type) {
                            com.wade.teacher.data.local.entity.SchoolType.JUNIOR_HIGH -> "國中 (7-9年級)"
                            com.wade.teacher.data.local.entity.SchoolType.SENIOR_HIGH -> "高中 (10-12年級)"
                            com.wade.teacher.data.local.entity.SchoolType.COMPREHENSIVE -> "綜合高中 (7-12年級)"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { selectedType = type }
                        ) {
                            RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                            Text(label)
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text("作息時間設定 (節次時間)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(editablePeriodTimes.size) { index ->
                    val pt = editablePeriodTimes[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("第 ${pt.period} 節", modifier = Modifier.width(50.dp), fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = pt.startTime,
                            onValueChange = { newStart ->
                                editablePeriodTimes = editablePeriodTimes.toMutableList().apply {
                                    this[index] = pt.copy(startTime = newStart)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("開始") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pt.endTime,
                            onValueChange = { newEnd ->
                                editablePeriodTimes = editablePeriodTimes.toMutableList().apply {
                                    this[index] = pt.copy(endTime = newEnd)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("結束") },
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, selectedType, website, editablePeriodTimes) }) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CounselingDashboard(
    onNavigateToStudent: (String, String) -> Unit,
    onNavigateToMoodCheck: () -> Unit,
    onNavigateToResources: () -> Unit,
    viewModel: CounselorViewModel
) {
    val context = LocalContext.current
    val studentsWithProfiles by viewModel.studentsWithProfiles.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCsv(context, it) }
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredEntries = if (searchQuery.isBlank()) {
        studentsWithProfiles
    } else {
        studentsWithProfiles.filter { entry ->
            entry.student.name.contains(searchQuery, ignoreCase = true) || 
            entry.student.studentId.contains(searchQuery) ||
            (entry.profile?.status?.contains(searchQuery, ignoreCase = true) ?: false) ||
            (entry.profile?.legalStatus?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("輔導個案管理", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Row {
                        IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.Add, contentDescription = "匯入", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.clearAllStudents() }) {
                            Icon(Icons.Default.Delete, contentDescription = "清空", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Feature Cards
        item {
            val lastSession by viewModel.lastSession.collectAsState(null)
            val lastResponses by if (lastSession != null) {
                viewModel.getResponsesForSession(lastSession!!.id).collectAsState(emptyList())
            } else {
                remember { mutableStateOf(emptyList<com.wade.teacher.data.local.entity.MoodCheckResponse>()) }
            }

            val moodDesc = if (lastSession != null) {
                val days = (System.currentTimeMillis() - lastSession!!.conductedAt) / 86400000L
                val alertCount = lastResponses.count { it.score <= 3 }
                "上次施測: ${lastSession!!.classId} ($days 天前)，${alertCount} 人需關注"
            } else {
                "尚未進行任何心情施測"
            }

            DashboardActionCard(
                title = "心情溫度計",
                description = moodDesc,
                actionText = "開始施測",
                onClick = onNavigateToMoodCheck
            )
        }

        item {
            DashboardActionCard(
                title = "校外資源庫",
                description = "24 小時求助專線與社福機構資訊",
                actionText = "查看清單",
                onClick = onNavigateToResources
            )
        }

        // Mood Alerts
        item {
            val lastSession by viewModel.lastSession.collectAsState(null)
            if (lastSession != null) {
                val alerts by viewModel.getClassMoodAlerts(lastSession!!.classId).collectAsState(emptyList())
                if (alerts.isNotEmpty()) {
                    val alertNames = alerts.map { id -> studentsWithProfiles.find { it.student.studentId == id }?.student?.name ?: id }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("需關注: ${alertNames.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        // Today's Appointments
        item {
            val startOfToday = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayAppointments by viewModel.getTodayAppointments(startOfToday).collectAsState(emptyList())

            if (todayAppointments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("今日晤談 (${todayAppointments.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                todayAppointments.forEach { appointment ->
                    val studentName = studentsWithProfiles.find { it.student.studentId == appointment.studentId }?.student?.name ?: appointment.studentId
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(appointment.scheduledAt))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        onClick = { onNavigateToStudent(appointment.studentId, studentName) }
                    ) {
                        ListItem(
                            headlineContent = { Text("$studentName ($timeStr)", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("類型: ${appointment.type}") },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }

        // Admin/Demo Action
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { 
                    if (studentsWithProfiles.isNotEmpty()) {
                        val entry = studentsWithProfiles.first()
                        viewModel.setStudentStatus(entry.student.studentId, "Active", "法院審理中", "High")
                        viewModel.toggleKeyTracking(entry.student.studentId)
                        viewModel.scheduleAppointment(entry.student.studentId, System.currentTimeMillis() + 86400000)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("示範資料標記 (測試用)")
            }
        }

        // Sticky Search & Filter
        stickyHeader {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜尋姓名、學號、或狀態") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null) } }
                        } else null,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(selected = searchQuery == "", onClick = { searchQuery = "" }, label = { Text("全部") })
                        FilterChip(selected = searchQuery == "High", onClick = { searchQuery = "High" }, label = { Text("高風險") })
                        FilterChip(selected = searchQuery == "休學", onClick = { searchQuery = "休學" }, label = { Text("休學") })
                        FilterChip(selected = searchQuery == "法院", onClick = { searchQuery = "法院" }, label = { Text("法院/監獄") })
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        // Student List
        item {
            val listTitle = if (searchQuery.isEmpty()) "重點追蹤與全體學生" else "搜尋結果 (${filteredEntries.size})"
            Text(
                text = listTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredEntries.isNotEmpty()) {
            items(filteredEntries, key = { it.student.studentId }) { entry ->
                val semesterText = if (entry.student.currentSemester == 1) "上" else "下"
                DashboardActionCard(
                    title = "${entry.student.name} (${entry.student.currentGrade}年$semesterText ${entry.student.currentClass}班)",
                    description = "學號：${entry.student.studentId} | 狀態：${entry.profile?.status ?: "Active"} ${entry.profile?.legalStatus?.let {"($it)"} ?: ""}",
                    actionText = "查看",
                    onClick = { onNavigateToStudent(entry.student.studentId, entry.student.name) }
                )
            }
        } else {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (studentsWithProfiles.isEmpty()) "尚未匯入學生資料" else "找不到符合的學生",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
        }
    }
}

@Composable
fun SubjectTeacherDashboard(
    viewModel: SubjectTeacherViewModel = viewModel(),
    onNavigateToLessonPlans: () -> Unit = {},
    onNavigateToTagging: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val assignedClasses by viewModel.assignedClasses.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val selectedClass = assignedClasses.find { it.classId == selectedClassId }

    val studentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importStudents(context, it) }
    }
    val schedulePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importSchedule(context, it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("教學工作台", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Row {
                        IconButton(onClick = { studentPicker.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "匯入學生", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { schedulePicker.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.DateRange, contentDescription = "匯入課表", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = { viewModel.clearAllData() }) {
                            Icon(Icons.Default.Delete, contentDescription = "清空", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Horizontal Class Switcher (1-A)
        if (assignedClasses.isNotEmpty()) {
            item {
                Text("授課班級視角", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    assignedClasses.forEach { sc ->
                        FilterChip(
                            selected = selectedClassId == sc.classId,
                            onClick = { viewModel.selectClass(sc.classId) },
                            label = { Text("${sc.classId} ${sc.subjectName}") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else if (!isImporting) {
            item {
                Text("尚未匯入課表，請點擊上方日曆圖示進行匯入。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Current Course Card (1-B)
        if (selectedClass != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("即時課程狀態", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${selectedClass.classId} 班 - ${selectedClass.subjectName}", style = MaterialTheme.typography.titleMedium)
                        Text("教室: ${selectedClass.roomNumber} | 學生: ${selectedClass.studentCount} 位")
                        Text("下一堂課: ${selectedClass.nextLessonTime ?: "未排定"}", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Feature Shortcuts
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("教學捷徑", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            DashboardActionCard(
                title = "教案模板庫",
                description = "對接 108 課綱核心素養",
                actionText = "開啟",
                onClick = onNavigateToLessonPlans
            )
        }

        item {
            DashboardActionCard(
                title = "課堂表現快速標記",
                description = "即時記錄學生發言、分組表現",
                actionText = "進入記錄",
                onClick = { selectedClassId?.let { onNavigateToTagging(it) } }
            )
        }
        
        item {
            DashboardActionCard(
                title = "作業派發",
                description = "目前有 0 份作業待批改",
                actionText = "管理",
                onClick = { /* TODO Sprint 3 */ }
            )
        }
    }
}

@Composable
fun SubjectClassSwitcher(
    viewModel: SubjectTeacherViewModel = viewModel()
) {
    val assignedClasses by viewModel.assignedClasses.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("科任班級管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        assignedClasses.forEach { sc ->
            ClassCard(sc.classId, sc.subjectName, "下一堂課: ${sc.nextLessonTime}")
        }
    }
}

@Composable
fun ClassCard(className: String, subject: String, status: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = className, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = subject, style = MaterialTheme.typography.bodySmall)
                Text(text = status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun InteractionHub() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("親師生互動樞紐", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        DashboardActionCard("家長線上簽閱", "聯絡簿數位化簽收系統", "進入系統")
        DashboardActionCard("學生提問箱", "來自 101 班的 3 個物理疑問", "查看問題")
        DashboardActionCard("心情溫度計", "今日有 2 位學生回報低落", "關懷訪談")
    }
}

@Composable
fun DashboardActionCard(title: String, description: String, actionText: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) {
                Text(actionText)
            }
        }
    }
}
