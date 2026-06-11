package com.wade.teacher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(role: String, onBack: () -> Unit, onNavigateToStudent: (String, String) -> Unit = { _, _ -> }) {
    val viewModel: CounselorViewModel = viewModel()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val roleTitle = roles.find { it.id == role }?.title ?: "未知角色"
    var selectedTabIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var showSettingsDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showSettingsDialog) {
        SchoolSettingsDialog(
            config = schoolConfig,
            onDismiss = { showSettingsDialog = false },
            onSave = { name, type ->
                viewModel.updateSchoolConfig(name, type)
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
                        Text(text = "${com.wade.teacher.util.AcademicUtils.getAcademicString()} ($roleTitle)", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                    if (role == "counseling") {
                        CounselingDashboard(onNavigateToStudent)
                    } else {
                        RoleFeatureContent(role = role)
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
    onDismiss: () -> Unit,
    onSave: (String, com.wade.teacher.data.local.entity.SchoolType) -> Unit
) {
    var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(config.schoolName) }
    var selectedType by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(config.schoolType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("全校性設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("校名") },
                    modifier = Modifier.fillMaxWidth()
                )
                
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
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, selectedType) }) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounselingDashboard(
    onNavigateToStudent: (String, String) -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importCsv(context, it) }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("輔導個案管理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            if (isImporting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Row {
                    IconButton(onClick = { filePickerLauncher.launch("text/csv") }) {
                        Icon(Icons.Default.Add, contentDescription = "匯入學生 CSV", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.clearAllStudents() }) {
                        Icon(Icons.Default.Delete, contentDescription = "清空所有資料", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜尋姓名、學號、或狀態 (如: 休學)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = true, onClick = {}, label = { Text("全部") })
            FilterChip(selected = false, onClick = {}, label = { Text("高風險") })
            FilterChip(selected = false, onClick = {}, label = { Text("法院/監獄") })
            FilterChip(selected = false, onClick = {}, label = { Text("休學") })
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("即將到來的預約", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        
        val upcomingAppointments = students.filter { it.nextAppointment != null }.sortedBy { it.nextAppointment }
        if (upcomingAppointments.isNotEmpty()) {
            upcomingAppointments.take(1).forEach { student ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    onClick = { onNavigateToStudent(student.studentId, student.name) }
                ) {
                    ListItem(
                        headlineContent = { Text("${student.name} (${student.studentId})", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("預約時間: 2026-06-12 14:30") }, // Simplified date display
                        trailingContent = { Text("明日", color = MaterialTheme.colorScheme.error) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        } else {
            Text("目前無預約", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
        }
        
        // Admin Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.promoteAllStudents() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("全體升級", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { 
                    if (students.isNotEmpty()) {
                        // Demo: Mark first student as High Risk and Key Tracking
                        val s = students.first()
                        viewModel.setStudentStatus(s.studentId, "Active", "法院審理中", "High")
                        viewModel.toggleKeyTracking(s.studentId)
                        viewModel.scheduleAppointment(s.studentId, System.currentTimeMillis() + 86400000)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("示範標記", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text("重點追蹤個案", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        val keyStudents = students.filter { it.isKeyTracking || it.priority == "High" }
        if (keyStudents.isNotEmpty()) {
            keyStudents.forEach { student ->
                val semesterText = if (student.currentSemester == 1) "上" else "下"
                DashboardActionCard(
                    title = "${student.name} (${student.currentGrade}年$semesterText ${student.currentClass}班)",
                    description = "狀態：${student.status} ${student.legalStatus?.let {"($it)"} ?: ""} [${student.priority}]",
                    actionText = "查看",
                    onClick = { onNavigateToStudent(student.studentId, student.name) }
                )
            }
        } else {
            Text("暫無重點追蹤對象", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun SubjectClassSwitcher() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("科任班級管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        ClassCard("101 班", "物理 (一)", "待批改作業: 2")
        ClassCard("102 班", "物理 (一)", "今日有課 (14:10)")
        ClassCard("205 班", "進階物理", "已完成進度: 75%")
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
