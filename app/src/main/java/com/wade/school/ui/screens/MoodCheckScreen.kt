package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    // Map to store scores and notes: studentId -> (score, note)
    val moodData = remember { mutableStateMapOf<String, Pair<Int, String>>() }

    val filteredStudents = students.filter { it.currentClass == selectedClass }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("心情溫度計施測") },
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
                                initialData = moodData[student.studentId],
                                onDataChange = { score, note ->
                                    moodData[student.studentId] = score to note
                                }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            moodData.forEach { (studentId, data) ->
                                viewModel.recordMoodResponse(activeSessionId!!, studentId, data.first, data.second)
                            }
                            viewModel.finishMoodCheckSession()
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        enabled = moodData.size >= filteredStudents.size // All students must have a score
                    ) {
                        Text("完成並送出 (已填寫 ${moodData.size}/${filteredStudents.size})")
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
    initialData: Pair<Int, String>?,
    onDataChange: (Int, String) -> Unit
) {
    var score by remember { mutableFloatStateOf(initialData?.first?.toFloat() ?: 5f) }
    var note by remember { mutableStateOf(initialData?.second ?: "") }

    LaunchedEffect(score, note) {
        onDataChange(score.toInt(), note)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${student.name} (${student.studentId})", fontWeight = FontWeight.Bold)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("心情分數: ${score.toInt()}", modifier = Modifier.width(100.dp))
                Slider(
                    value = score,
                    onValueChange = { score = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.weight(1f)
                )
            }
            
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("備註 (選填)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )
        }
    }
}
