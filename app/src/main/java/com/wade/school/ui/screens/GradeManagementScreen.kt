package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.*
import com.wade.school.util.AcademicUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeManagementScreen(
    classId: String,
    viewModel: SubjectTeacherViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("成績輸入", "加權計算", "成績分析")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 班級成績管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> GradeInputTab(classId, viewModel)
                1 -> GradeWeightTab(classId, viewModel)
                2 -> GradeAnalysisTab(classId, viewModel)
            }
        }
    }
}

@Composable
fun GradeInputTab(classId: String, viewModel: SubjectTeacherViewModel) {
    val exams by viewModel.getExamsByClass(classId).collectAsState(initial = emptyList())
    var showCreateExamDialog by remember { mutableStateOf(false) }
    var selectedExam by remember { mutableStateOf<ExamRecord?>(null) }

    val students by viewModel.getStudentsInClass(classId).collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("考試記錄", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showCreateExamDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("新增考試")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (exams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("尚未建立考試記錄", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(exams) { exam ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { selectedExam = exam }
                    ) {
                        ListItem(
                            headlineContent = { Text(exam.examName) },
                            supportingContent = { Text("${exam.examType} - ${java.text.SimpleDateFormat("yyyy/MM/dd").format(exam.examDate)}") },
                            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateExamDialog) {
        CreateExamDialog(
            onDismiss = { showCreateExamDialog = false },
            onConfirm = { name, type, date ->
                viewModel.createExam(
                    ExamRecord(
                        classId = classId,
                        subjectName = "自訂科目", // Should be dynamic
                        examName = name,
                        examType = type,
                        examDate = date,
                        academicYear = AcademicUtils.getCurrentAcademicYear(),
                        semester = AcademicUtils.getCurrentSemester()
                    ),
                    students.map { it.studentId }
                )
                showCreateExamDialog = false
            }
        )
    }

    if (selectedExam != null) {
        ExamScoreEntryDialog(
            exam = selectedExam!!,
            viewModel = viewModel,
            onDismiss = { selectedExam = null }
        )
    }
}

@Composable
fun CreateExamDialog(onDismiss: () -> Unit, onConfirm: (String, ExamType, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ExamType.QUIZ) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增考試記錄") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("考試名稱（如：第一次段考）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("考試類型")
                Row {
                    ExamType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, selectedType, System.currentTimeMillis()) }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun ExamScoreEntryDialog(exam: ExamRecord, viewModel: SubjectTeacherViewModel, onDismiss: () -> Unit) {
    val scores by viewModel.getScoresByExam(exam.id).collectAsState(initial = emptyList())
    var editableScores by remember { mutableStateOf(scores) }

    LaunchedEffect(scores) {
        if (editableScores.isEmpty()) editableScores = scores
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${exam.examName} 分數輸入") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(editableScores.indices.toList()) { index ->
                    val score = editableScores[index]
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${score.studentId}", modifier = Modifier.width(60.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = score.score.toString(),
                            onValueChange = { newValue ->
                                val floatValue = newValue.toFloatOrNull() ?: 0f
                                editableScores = editableScores.toMutableList().apply {
                                    this[index] = this[index].copy(score = floatValue)
                                }
                            },
                            label = { Text("分數") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Checkbox(
                            checked = score.isAbsent,
                            onCheckedChange = { checked ->
                                editableScores = editableScores.toMutableList().apply {
                                    this[index] = this[index].copy(isAbsent = checked)
                                }
                            }
                        )
                        Text("缺考")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.updateExamScores(editableScores)
                onDismiss()
            }) {
                Text("儲存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("關閉") }
        }
    )
}

@Composable
fun GradeWeightTab(classId: String, viewModel: SubjectTeacherViewModel) {
    var weight by remember { mutableStateOf<GradeWeight?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(classId) {
        weight = viewModel.getGradeWeight(classId, "自訂科目", AcademicUtils.getCurrentSemester())
            ?: GradeWeight(
                classId = classId,
                subjectName = "自訂科目",
                academicYear = AcademicUtils.getCurrentAcademicYear(),
                semester = AcademicUtils.getCurrentSemester()
            )
    }

    if (weight != null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("學期成績佔比設定", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            WeightSlider("平時成績 (Daily)", weight!!.dailyWeight) { weight = weight!!.copy(dailyWeight = it) }
            WeightSlider("期中考試 (Midterm)", weight!!.midtermWeight) { weight = weight!!.copy(midtermWeight = it) }
            WeightSlider("期末考試 (Final)", weight!!.finalWeight) { weight = weight!!.copy(finalWeight = it) }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("平時成績細項佔比", style = MaterialTheme.typography.titleSmall)
            WeightSlider("作業 (Homework)", weight!!.homeworkWeight) { weight = weight!!.copy(homeworkWeight = it) }
            WeightSlider("課堂表現 (Participation)", weight!!.participationWeight) { weight = weight!!.copy(participationWeight = it) }
            WeightSlider("小考 (Quiz)", weight!!.quizWeight) { weight = weight!!.copy(quizWeight = it) }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { viewModel.saveGradeWeight(weight!!) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("儲存設定")
            }
        }
    }
}

@Composable
fun WeightSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("${(value * 100).toInt()}%", fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f)
    }
}

@Composable
fun GradeAnalysisTab(classId: String, viewModel: SubjectTeacherViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("班級成績分析", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Placeholder for distribution chart
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("成績分佈直方圖 (Canvas 繪製)", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("統計摘要", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("平均分", "72.5", Modifier.weight(1f))
            StatCard("及格率", "85%", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("最高分", "98", Modifier.weight(1f))
            StatCard("最低分", "42", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("低標警示學生", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        
        val atRiskStudents = listOf("張小明 (42)", "李小華 (55)") // In real app, calculate from scores
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(atRiskStudents.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // In real app, trigger viewModel.sendRiskAlerts(atRiskStudents)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("推送到輔導室警示")
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
