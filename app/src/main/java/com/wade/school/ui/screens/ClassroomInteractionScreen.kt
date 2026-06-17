package com.wade.school.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.ClassroomInteraction
import com.wade.school.data.local.entity.InteractionType
import com.wade.school.data.local.entity.Student
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomInteractionScreen(
    classId: String,
    viewModel: SubjectTeacherViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val students by viewModel.getStudentsInClass(classId).collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("隨機抽人", "排行榜", "計時器")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 課堂互動") },
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
                0 -> RandomPickerTab(classId, students, viewModel)
                1 -> LeaderboardTab(classId, viewModel)
                2 -> TimerTab()
            }
        }
    }
}

@Composable
fun RandomPickerTab(classId: String, students: List<Student>, viewModel: SubjectTeacherViewModel) {
    var isSpinning by remember { mutableStateOf(false) }
    var pickedStudent by remember { mutableStateOf<Student?>(null) }
    var currentDisplayIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(300.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isSpinning) {
                    Text(
                        text = students.getOrNull(currentDisplayIndex)?.name ?: "...",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = pickedStudent?.name ?: "點擊下方抽籤",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (students.isNotEmpty()) {
                    scope.launch {
                        isSpinning = true
                        repeat(20) { i ->
                            currentDisplayIndex = (0 until students.size).random()
                            delay(50L + (i * 10L))
                        }
                        pickedStudent = students.random()
                        isSpinning = false
                        
                        // Auto-record interaction
                        pickedStudent?.let {
                            viewModel.recordInteraction(
                                ClassroomInteraction(
                                    studentId = it.studentId,
                                    classId = classId,
                                    subjectName = "自訂科目",
                                    interactionType = InteractionType.RANDOM_PICK,
                                    score = 1
                                )
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = !isSpinning && students.isNotEmpty()
        ) {
            Icon(Icons.Default.Casino, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("開始隨機抽籤", fontSize = 20.sp)
        }

        if (pickedStudent != null && !isSpinning) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    viewModel.recordInteraction(
                        ClassroomInteraction(
                            studentId = pickedStudent!!.studentId,
                            classId = classId,
                            subjectName = "自訂科目",
                            interactionType = InteractionType.ANSWER_CORRECT,
                            score = 2,
                            note = "回答正確"
                        )
                    )
                }) {
                    Text("+2 答對")
                }
                OutlinedButton(onClick = {
                    viewModel.recordInteraction(
                        ClassroomInteraction(
                            studentId = pickedStudent!!.studentId,
                            classId = classId,
                            subjectName = "自訂科目",
                            interactionType = InteractionType.ANSWER_WRONG,
                            score = 0,
                            note = "回答錯誤"
                        )
                    )
                }) {
                    Text("+0 答錯")
                }
            }
        }
    }
}

@Composable
fun LeaderboardTab(classId: String, viewModel: SubjectTeacherViewModel) {
    val summary by viewModel.getInteractionSummary(classId).collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("本學期加分排行榜", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(summary) { item ->
            ListItem(
                headlineContent = { Text(item.studentId) },
                trailingContent = { 
                    Text("${item.totalScore}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun TimerTab() {
    var timeLeft by remember { mutableStateOf(60) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, timeLeft) {
        if (isRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft -= 1
        } else if (timeLeft == 0) {
            isRunning = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 120.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { isRunning = !isRunning }) {
                Text(if (isRunning) "暫停" else "開始")
            }
            OutlinedButton(onClick = { 
                isRunning = false
                timeLeft = 60 
            }) {
                Text("重設")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(30, 60, 180, 300).forEach { sec ->
                AssistChip(onClick = { timeLeft = sec }, label = { Text("${sec}s") })
            }
        }
    }
}
