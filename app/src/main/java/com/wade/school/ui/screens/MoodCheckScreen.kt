package com.wade.school.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodCheckScreen(
    viewModel: CounselorViewModel = viewModel(),
    onBack: () -> Unit,
    preSelectedClassId: String? = null
) {
    val classes by viewModel.classes.collectAsState()
    val students by viewModel.students.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()

    var selectedClass by remember { mutableStateOf(preSelectedClassId ?: "") }
    var expanded by remember { mutableStateOf(false) }

    // Map to store mood data: studentId -> MoodCheckItemData
    val moodData = remember { mutableStateMapOf<String, MoodCheckItemData>() }

    val filteredStudents = students.filter { it.currentClass == selectedClass }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("心情溫度計施測 (情緒四象限 + BSRS-5)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (preSelectedClassId == null) {
                Text("選擇施測班級", style = MaterialTheme.typography.labelLarge)
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedClass,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("班級") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        classes.forEach { className ->
                            DropdownMenuItem(
                                text = { Text(className) },
                                onClick = {
                                    selectedClass = className
                                    expanded = false
                                    moodData.clear()
                                }
                            )
                        }
                    }
                }
            } else {
                Text("施測班級: $preSelectedClassId", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedClass.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("學生清單 (${filteredStudents.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    if (activeSessionId == null) {
                        Button(
                            onClick = {
                                viewModel.startMoodCheckSession(selectedClass, "counselor_01")
                            }
                        ) {
                            Text("開始施測")
                        }
                    } else {
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text("施測中 (Session ID: $activeSessionId)", modifier = Modifier.padding(4.dp))
                        }
                    }
                }

                if (activeSessionId != null) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredStudents) { student ->
                            MoodInputCard(
                                student = student,
                                initialData = moodData[student.studentId] ?: MoodCheckItemData(),
                                onDataChange = { updatedData ->
                                    moodData[student.studentId] = updatedData
                                }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            moodData.forEach { (studentId, data) ->
                                viewModel.saveMoodCheckResponse(
                                    sessionId = activeSessionId!!.toLong(),
                                    studentId = studentId,
                                    selectedEmotion = data.selectedEmotion,
                                    emotionQuadrant = data.emotionQuadrant,
                                    bsrsScores = data.bsrsScores,
                                    note = data.note
                                )
                            }
                            viewModel.finishMoodCheckSession()
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        // Only enable if ALL students have at least selected an emotion
                        enabled = moodData.size >= filteredStudents.size && moodData.values.all { it.isComplete }
                    ) {
                        val completedCount = moodData.values.count { it.isComplete }
                        Text("完成並送出 (已填寫 $completedCount/${filteredStudents.size})")
                    }
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("點擊「開始施測」來輸入資料", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("請先選擇班級", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun MoodInputCard(
    student: Student, 
    initialData: MoodCheckItemData,
    onDataChange: (MoodCheckItemData) -> Unit
) {
    var data by remember { mutableStateOf(initialData) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Emotion, 1: BSRS

    // Need BSRS if blue or red is selected
    val needsBsrs = data.emotionQuadrant == "BLUE" || data.emotionQuadrant == "RED"

    LaunchedEffect(data) {
        onDataChange(data)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = if (data.isComplete) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${student.name} (${student.studentId})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("情緒四象限 (必填)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            "心情溫度計 BSRS-5",
                            color = if (needsBsrs && data.bsrsScores == null) MaterialTheme.colorScheme.error else Color.Unspecified
                        ) 
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> EmotionQuadrantSelector(
                        selectedEmotion = data.selectedEmotion,
                        onEmotionSelected = { emotion, quadrant ->
                            data = data.copy(
                                selectedEmotion = emotion,
                                emotionQuadrant = quadrant,
                                // Auto-initialize BSRS if required and not yet set
                                bsrsScores = if ((quadrant == "BLUE" || quadrant == "RED") && data.bsrsScores == null) List(6) { 0 } else data.bsrsScores
                            )
                            if (quadrant == "BLUE" || quadrant == "RED") {
                                selectedTab = 1 // Auto-switch to BSRS tab to prompt filling
                            }
                        }
                    )
                1 -> BsrsQuestionnaire(
                        scores = data.bsrsScores ?: List(6) { 0 },
                        onScoresChange = { newScores ->
                            data = data.copy(bsrsScores = newScores)
                        }
                    )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = data.note,
                onValueChange = { data = data.copy(note = it) },
                label = { Text("輔導備註 (選填)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
    }
}

@Composable
fun EmotionQuadrantSelector(
    selectedEmotion: String?,
    onEmotionSelected: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("請選擇符合當下心情的詞彙：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            QuadrantBox(modifier = Modifier.weight(1f), title = "黃色 (高能量/正向)", color = Color(0xFFFFF59D), emotions = EMOTION_YELLOW, selectedEmotion = selectedEmotion) { onEmotionSelected(it, "YELLOW") }
            Spacer(modifier = Modifier.width(8.dp))
            QuadrantBox(modifier = Modifier.weight(1f), title = "紅色 (高能量/負向)", color = Color(0xFFFFCDD2), emotions = EMOTION_RED, selectedEmotion = selectedEmotion) { onEmotionSelected(it, "RED") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            QuadrantBox(modifier = Modifier.weight(1f), title = "綠色 (低能量/正向)", color = Color(0xFFC8E6C9), emotions = EMOTION_GREEN, selectedEmotion = selectedEmotion) { onEmotionSelected(it, "GREEN") }
            Spacer(modifier = Modifier.width(8.dp))
            QuadrantBox(modifier = Modifier.weight(1f), title = "藍色 (低能量/負向)", color = Color(0xFFBBDEFB), emotions = EMOTION_BLUE, selectedEmotion = selectedEmotion) { onEmotionSelected(it, "BLUE") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuadrantBox(modifier: Modifier, title: String, color: Color, emotions: List<String>, selectedEmotion: String?, onSelect: (String) -> Unit) {
    Column(
        modifier = modifier
            .background(color = color.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            emotions.forEach { emotion ->
                val isSelected = emotion == selectedEmotion
                Box(
                    modifier = Modifier
                        .clickable { onSelect(emotion) }
                        .background(if (isSelected) color else Color.White, shape = RoundedCornerShape(4.dp))
                        .border(1.dp, if (isSelected) Color.DarkGray else Color.LightGray, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(emotion, fontSize = 12.sp, color = if (isSelected) Color.Black else Color.DarkGray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
fun BsrsQuestionnaire(
    scores: List<Int>,
    onScoresChange: (List<Int>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("過去一週中，您感覺到以下狀況的嚴重程度？", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        
        BSRS_QUESTIONS.forEachIndexed { index, question ->
            val isSuicideQuestion = index == 5
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(question, fontSize = 14.sp, color = if (isSuicideQuestion) MaterialTheme.colorScheme.error else Color.Unspecified)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("完全沒有", "輕微", "中等", "嚴重", "非常嚴重").forEachIndexed { scoreIndex, label ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                            val newScores = scores.toMutableList()
                            newScores[index] = scoreIndex
                            onScoresChange(newScores)
                        }.padding(4.dp)) {
                            RadioButton(
                                selected = scores[index] == scoreIndex,
                                onClick = {
                                    val newScores = scores.toMutableList()
                                    newScores[index] = scoreIndex
                                    onScoresChange(newScores)
                                }
                            )
                            Text(label, fontSize = 10.sp, textAlign = TextAlign.Center)
                            Text("($scoreIndex)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
            if (index < 5) Divider(modifier = Modifier.padding(vertical = 4.dp))
        }
        
        val totalScore = scores.sum()
        val totalColor = when {
            totalScore >= 15 -> Color.Red
            totalScore >= 10 -> Color(0xFFFFA000) // Orange
            totalScore >= 6 -> Color(0xFFFBC02D) // Yellow
            else -> Color(0xFF4CAF50) // Green
        }
        val advice = when {
            totalScore >= 15 -> "重度情緒困擾：強烈建議轉介專業醫療機構或精神科協助。"
            totalScore >= 10 -> "中度情緒困擾：建議安排心理諮商或輔導介入。"
            totalScore >= 6 -> "輕度情緒困擾：建議給予支持關懷，提供紓壓建議。"
            else -> "正常範圍：適應良好。"
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = totalColor.copy(alpha = 0.1f)), border = CardDefaults.outlinedCardBorder()) {
            Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                Text("BSRS-5 總分: $totalScore", fontWeight = FontWeight.Bold, color = totalColor)
                Text(advice, fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
}
