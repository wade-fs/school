package com.wade.school.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.StudentWithProfile
import com.wade.school.data.local.entity.MoeSchool
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    role: String,
    onBack: () -> Unit,
    onNavigateToStudent: (String, String) -> Unit,
    onNavigateToMoodCheck: (String?) -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToLessonPlans: () -> Unit,
    onNavigateToTagging: (String) -> Unit,
    onNavigateToAssignments: (String) -> Unit,
    onNavigateToAnalysis: (String) -> Unit,
    onNavigateToAttendance: (String) -> Unit,
    onNavigateToAttendanceHistory: (String) -> Unit,
    onNavigateToBulletins: (String) -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: CounselorViewModel = viewModel()
) {
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val periodTimes by viewModel.periodTimes.collectAsState()
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when(role) {
                            "counseling" -> "輔導主任工作台"
                            "subject" -> "任課教師工作台"
                            "homeroom" -> "導師工作台"
                            else -> "智慧教師助理"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("工作台") },
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Class, contentDescription = null) },
                    label = { Text("班級") },
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
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
            if (showSettingsDialog) {
                SchoolSettingsDialog(
                    config = schoolConfig,
                    periodTimes = periodTimes,
                    onDismiss = { showSettingsDialog = false },
                    onSave = { name, type, ownerName, website, homeroom, address, phone, times ->
                        viewModel.updateSchoolConfig(name, type, ownerName, website, homeroom, address, phone)
                        viewModel.updatePeriodTimes(times)
                        showSettingsDialog = false
                    },
                    viewModel = viewModel
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    when (role) {
                        "counseling" -> CounselingDashboard(
                            onNavigateToStudent = onNavigateToStudent, 
                            onNavigateToMoodCheck = { onNavigateToMoodCheck(null) }, 
                            onNavigateToResources = onNavigateToResources,
                            onNavigateToScan = { onNavigate("school_info/scan") },
                            onNavigateToManual = { onNavigate("manual") },
                            viewModel = viewModel
                        )
                        "subject" -> SubjectTeacherDashboard(
                            onNavigateToLessonPlans = onNavigateToLessonPlans,
                            onNavigateToTagging = onNavigateToTagging,
                            onNavigateToAssignments = onNavigateToAssignments,
                            onNavigateToAnalysis = onNavigateToAnalysis,
                            onNavigateToScan = { onNavigate("school_info/scan") },
                            onNavigateToManual = { onNavigate("manual") }
                        )
                        "homeroom" -> HomeroomDashboard(
                            onNavigateToStudent = onNavigateToStudent,
                            onNavigateToMoodCheck = { onNavigateToMoodCheck(schoolConfig.homeroomClass) },
                            onNavigateToAttendance = onNavigateToAttendance,
                            onNavigateToAttendanceHistory = onNavigateToAttendanceHistory,
                            onNavigateToBulletins = onNavigateToBulletins,
                            onNavigateToScan = { onNavigate("school_info/scan") },
                            onNavigateToManual = { onNavigate("manual") },
                            onEditClass = { showSettingsDialog = true },
                            viewModel = viewModel
                        )
                        else -> RoleFeatureContent(role = role, onNavigate = onNavigate)
                    }
                }
                1 -> SubjectClassSwitcher()
                2 -> InteractionHub()
            }
        }
    }
}

@Composable
fun SchoolInfoCard(config: com.wade.school.data.local.entity.SchoolConfig) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(config.schoolName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(8.dp))
            
            config.schoolWebsite?.let { url ->
                Row(
                    modifier = Modifier.clickable { 
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Language, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("前往學校官網", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            config.address?.let { addr ->
                Row(
                    modifier = Modifier.clickable { 
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$addr"))
                        context.startActivity(intent)
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Place, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(addr, style = MaterialTheme.typography.bodySmall)
                }
            }

            config.phone?.let { tel ->
                Row(
                    modifier = Modifier.clickable { 
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$tel"))
                        context.startActivity(intent)
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text(tel, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSettingsDialog(
    config: com.wade.school.data.local.entity.SchoolConfig,
    periodTimes: List<com.wade.school.data.local.entity.PeriodTime>,
    onDismiss: () -> Unit,
    onSave: (String, com.wade.school.data.local.entity.SchoolType, String?, String?, String, String?, String?, List<com.wade.school.data.local.entity.PeriodTime>) -> Unit,
    viewModel: CounselorViewModel
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(config.schoolName) }
    var ownerName by remember { mutableStateOf(config.ownerName ?: "") }
    var selectedType by remember { mutableStateOf(config.schoolType) }
    var website by remember { mutableStateOf(config.schoolWebsite ?: "") }
    var address by remember { mutableStateOf(config.address ?: "") }
    var phone by remember { mutableStateOf(config.phone ?: "") }
    var homeroom by remember { mutableStateOf(config.homeroomClass) }
    var searchQuery by remember { mutableStateOf("") }
    
    val moeSchools by remember(searchQuery) { viewModel.searchMoeSchools(searchQuery) }.collectAsState(initial = emptyList())
    val isFetchingMoe by viewModel.isFetchingMoe.collectAsState()
    val moeSchoolCount by viewModel.moeSchoolCount.collectAsState()

    var editablePeriodTimes by remember(periodTimes) { mutableStateOf(periodTimes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("全校性設定") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val year = remember { com.wade.school.util.AcademicUtils.getCurrentAcademicYear() }
                    Column {
                        Button(
                            onClick = { viewModel.fetchMoeSchools(context) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isFetchingMoe
                        ) {
                            if (isFetchingMoe) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else Text("同步全國學校資料庫 ($year 學年度)")
                        }
                        Text(
                            text = "目前資料庫內共有 $moeSchoolCount 間學校",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp),
                            color = if (moeSchoolCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
                item {
                    OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("您的姓名 (教師姓名)") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("搜尋並選擇學校") }, placeholder = { Text("例如：清水") }, modifier = Modifier.fillMaxWidth())
                }
                if (moeSchools.isNotEmpty()) {
                    items(moeSchools) { school ->
                        ListItem(
                            headlineContent = { Text(school.name, fontSize = 14.sp) },
                            supportingContent = { Text("${school.city} | ${school.address}", fontSize = 12.sp) },
                            modifier = Modifier.clickable { 
                                name = school.name
                                website = school.website ?: ""
                                address = school.address
                                phone = school.phone
                                searchQuery = ""
                            }
                        )
                    }
                }
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("校名") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("地址") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("聯絡電話") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("官網網址") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = homeroom, onValueChange = { homeroom = it }, label = { Text("我的導師班級") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Text("學制設定", style = MaterialTheme.typography.labelLarge)
                    com.wade.school.data.local.entity.SchoolType.values().forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedType = type }) {
                            RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                            Text(when(type) {
                                com.wade.school.data.local.entity.SchoolType.JUNIOR_HIGH -> "國中 (7-9年級)"
                                com.wade.school.data.local.entity.SchoolType.SENIOR_HIGH -> "高中 (10-12年級)"
                                com.wade.school.data.local.entity.SchoolType.COMPREHENSIVE -> "綜合高中 (7-12年級)"
                            })
                        }
                    }
                }
                item { HorizontalDivider(Modifier.padding(vertical = 12.dp)); Text("作息時間", style = MaterialTheme.typography.labelLarge) }
                items(editablePeriodTimes.size) { index ->
                    val pt = editablePeriodTimes[index]
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("第 ${pt.period} 節", modifier = Modifier.width(50.dp))
                        OutlinedTextField(value = pt.startTime, onValueChange = { newStart -> editablePeriodTimes = editablePeriodTimes.toMutableList().apply { this[index] = pt.copy(startTime = newStart) } }, modifier = Modifier.weight(1f), label = { Text("開始") })
                        OutlinedTextField(value = pt.endTime, onValueChange = { newEnd -> editablePeriodTimes = editablePeriodTimes.toMutableList().apply { this[index] = pt.copy(endTime = newEnd) } }, modifier = Modifier.weight(1f), label = { Text("結束") })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, selectedType, ownerName, website, homeroom, address, phone, editablePeriodTimes) }) { Text("儲存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CounselingDashboard(
    onNavigateToStudent: (String, String) -> Unit,
    onNavigateToMoodCheck: () -> Unit,
    onNavigateToResources: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToManual: () -> Unit,
    viewModel: CounselorViewModel
) {
    val context = LocalContext.current
    val activeStudents by viewModel.activeCounselingStudents.collectAsState()
    val allStudents by viewModel.studentsWithProfiles.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    
    var showAllStudents by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCsv(context, it) }
    }

    var searchQuery by remember { mutableStateOf("") }
    val baseList = if (showAllStudents || searchQuery.isNotEmpty()) allStudents else activeStudents
    
    val filteredEntries = if (searchQuery.isBlank()) {
        baseList
    } else {
        baseList.filter { entry ->
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
            SchoolInfoCard(config = schoolConfig)
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Text("常用工具", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            DashboardActionCard("文件掃描", "拍照上傳紙本公文", "開始", onNavigateToScan)
            DashboardActionCard("使用手冊", "查看 App 操作說明", "查看", onNavigateToManual)
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (showAllStudents) "所有學生" else "重點個案",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.FileUpload, contentDescription = "匯入學籍 CSV")
                }
                TextButton(onClick = { showAllStudents = !showAllStudents }) {
                    Text(if (showAllStudents) "顯示個案" else "顯示全部")
                }
            }
        }
        
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("搜尋姓名、學號或狀態...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
        }

        items(filteredEntries) { entry ->
            StudentCounselingCard(
                entry = entry,
                onClick = { onNavigateToStudent(entry.student.studentId, entry.student.name) }
            )
        }
    }
}

@Composable
fun StudentCounselingCard(entry: StudentWithProfile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.student.currentClass}班 ${entry.student.seatNo}號 ${entry.student.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val status = entry.profile?.status ?: "一般"
                    Badge(
                        containerColor = when(status) {
                            "Crisis" -> MaterialTheme.colorScheme.error
                            "Active" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    ) {
                        Text(status, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.profile?.legalStatus ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeroomDashboard(
    onNavigateToStudent: (String, String) -> Unit,
    onNavigateToMoodCheck: () -> Unit,
    onNavigateToAttendance: (String) -> Unit,
    onNavigateToAttendanceHistory: (String) -> Unit,
    onNavigateToBulletins: (String) -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToManual: () -> Unit,
    onEditClass: () -> Unit,
    viewModel: CounselorViewModel
) {
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val classId = schoolConfig.homeroomClass
    val students by viewModel.homeroomStudents.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val context = LocalContext.current

    val studentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importStudentsForClass(context, it, classId) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SchoolInfoCard(config = schoolConfig)
            Spacer(modifier = Modifier.height(8.dp))
            Text("常用工具", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            DashboardActionCard("文件掃描", "拍照上傳紙本公文", "開始", onNavigateToScan)
            DashboardActionCard("使用手冊", "查看 App 操作說明", "查看", onNavigateToManual)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${classId} 班級導師工作台", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onEditClass) {
                    Icon(Icons.Default.Edit, contentDescription = "更換班級", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = { 
                    studentPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv")) 
                }) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "匯入班級學生")
                }
                IconButton(onClick = { viewModel.clearStudentsForClass(classId) }) {
                    Icon(Icons.Default.Delete, contentDescription = "清空班級學生", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onNavigateToAttendance(classId) },
                    modifier = Modifier.weight(1f)
                ) { Text("今日點名") }
                
                OutlinedButton(
                    onClick = { onNavigateToAttendanceHistory(classId) },
                    modifier = Modifier.weight(1f)
                ) { Text("出缺席紀錄") }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNavigateToMoodCheck,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) { Text("心情檢核") }
                
                Button(
                    onClick = { onNavigateToBulletins(classId) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("班級管理助手") }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        item {
            Text("班級學生名單 (${students.size} 人)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (students.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("尚未匯入班級學生資料", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(students) { student ->
                StudentCard(
                    student = student,
                    onClick = { onNavigateToStudent(student.studentId, student.name) }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StudentCard(student: com.wade.school.data.local.entity.Student, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${student.seatNo}",
                modifier = Modifier.width(30.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = student.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (student.gender == "M") Icons.Default.Male else Icons.Default.Female,
                contentDescription = null,
                tint = if (student.gender == "M") Color.Blue.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
