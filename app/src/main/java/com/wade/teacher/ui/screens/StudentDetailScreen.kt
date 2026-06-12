package com.wade.teacher.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.teacher.data.local.entity.CaseLog
import com.wade.teacher.data.local.entity.CrisisEvent
import com.wade.teacher.data.local.entity.CounselorTeacherNote
import com.wade.teacher.data.local.entity.StudentWithProfile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    studentId: String, 
    studentName: String, 
    onBack: () -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    var sessionText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val studentWithProfile by viewModel.studentsWithProfiles.collectAsState()
    val currentEntry = studentWithProfile.find { it.student.studentId == studentId }
    val logs by viewModel.getLogsForStudent(studentId).collectAsState(initial = emptyList())
    val crisisEvents by viewModel.getCrisisEventsForStudent(studentId).collectAsState(initial = emptyList())
    val teacherNotes by viewModel.getNotesForStudent(studentId).collectAsState(initial = emptyList())

    var showStatusDialog by remember { mutableStateOf(false) }
    var editStatus by remember { mutableStateOf("Active") }
    var editLegal by remember { mutableStateOf("") }
    var editPriority by remember { mutableStateOf("Normal") }

    var showScheduleDialog by remember { mutableStateOf(false) }
    var scheduleType by remember { mutableStateOf("初談") }

    var showCrisisDialog by remember { mutableStateOf(false) }
    var crisisType by remember { mutableStateOf("其他") }
    var crisisSeverity by remember { mutableStateOf("一般") }
    var crisisDescription by remember { mutableStateOf("") }
    var crisisAction by remember { mutableStateOf("") }
    var crisisNotifiedParent by remember { mutableStateOf(false) }
    var crisisNotifiedPrincipal by remember { mutableStateOf(false) }
    var crisisReferralUnit by remember { mutableStateOf("") }

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteRequestType by remember { mutableStateOf("請多關心") }
    var noteSummary by remember { mutableStateOf("") }

    LaunchedEffect(currentEntry) {
        if (currentEntry != null) {
            editStatus = currentEntry.profile?.status ?: "Active"
            editLegal = currentEntry.profile?.legalStatus ?: "無"
            editPriority = currentEntry.profile?.priority ?: "Normal"
        }
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("編輯學生狀態") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statusOptions = listOf("Active", "休學", "轉學", "結案", "外部轉介")
                    var expandedStatus by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedStatus,
                        onExpandedChange = { expandedStatus = it }
                    ) {
                        OutlinedTextField(
                            value = editStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("輔導狀態") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStatus,
                            onDismissRequest = { expandedStatus = false }
                        ) {
                            statusOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { editStatus = option; expandedStatus = false }
                                )
                            }
                        }
                    }

                    Text("風險等級", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Normal", "Low", "Medium", "High").forEach { p ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { editPriority = p }) {
                                RadioButton(selected = editPriority == p, onClick = { editPriority = p })
                                Text(p, fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editLegal,
                        onValueChange = { editLegal = it },
                        label = { Text("法律狀態說明") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setStudentStatus(studentId, editStatus, editLegal, editPriority)
                    showStatusDialog = false
                }) { Text("儲存") }
            },
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) { Text("取消") }
            }
        )
    }

    if (showCrisisDialog) {
        AlertDialog(
            onDismissRequest = { showCrisisDialog = false },
            title = { Text("⚠ 通報危機事件", color = MaterialTheme.colorScheme.error) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        val typeOptions = listOf("自傷", "自殺意念", "霸凌", "家暴通報", "其他")
                        Text("事件類型", style = MaterialTheme.typography.labelSmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            typeOptions.take(3).forEach { type ->
                                FilterChip(
                                    selected = crisisType == type,
                                    onClick = { crisisType = type },
                                    label = { Text(type, fontSize = 10.sp) }
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            typeOptions.drop(3).forEach { type ->
                                FilterChip(
                                    selected = crisisType == type,
                                    onClick = { crisisType = type },
                                    label = { Text(type, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Text("嚴重程度", style = MaterialTheme.typography.labelSmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf("緊急", "嚴重", "一般").forEach { s ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { crisisSeverity = s }) {
                                    RadioButton(selected = crisisSeverity == s, onClick = { crisisSeverity = s })
                                    Text(s, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = crisisDescription,
                            onValueChange = { crisisDescription = it },
                            label = { Text("事件描述") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = crisisAction,
                            onValueChange = { crisisAction = it },
                            label = { Text("已採取行動") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = crisisNotifiedParent, onCheckedChange = { crisisNotifiedParent = it })
                            Text("已通知家長", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.width(8.dp))
                            Checkbox(checked = crisisNotifiedPrincipal, onCheckedChange = { crisisNotifiedPrincipal = it })
                            Text("已通知校長", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = crisisReferralUnit,
                            onValueChange = { crisisReferralUnit = it },
                            label = { Text("外部轉介機構 (選填)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("若無則留空") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reportCrisisEvent(
                            studentId = studentId,
                            eventType = crisisType,
                            severity = crisisSeverity,
                            actionTaken = crisisAction,
                            reportedBy = "Current Counselor", // Simplified
                            notifiedParent = crisisNotifiedParent,
                            notifiedPrincipal = crisisNotifiedPrincipal,
                            referralUnit = if (crisisReferralUnit.isBlank()) null else crisisReferralUnit
                        )
                        showCrisisDialog = false
                        Toast.makeText(context, "危機事件已記錄", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("送出通報") }
            },
            dismissButton = {
                TextButton(onClick = { showCrisisDialog = false }) { Text("取消") }
            }
        )
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("✉ 通知導師") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    val requestOptions = listOf("請多關心", "注意課堂行為", "避免點名", "其他")
                    Text("請求類型", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        requestOptions.take(2).forEach { type ->
                            FilterChip(
                                selected = noteRequestType == type,
                                onClick = { noteRequestType = type },
                                label = { Text(type, fontSize = 10.sp) }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        requestOptions.drop(2).forEach { type ->
                            FilterChip(
                                selected = noteRequestType == type,
                                onClick = { noteRequestType = type },
                                label = { Text(type, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = noteSummary,
                        onValueChange = { noteSummary = it },
                        label = { Text("備忘摘要") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("⚠ 請勿填寫個案細節") }
                    )
                    Text("提示：此欄位將傳送給導師，僅供課堂協作參考。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendNoteToTeacher(
                            studentId = studentId,
                            fromCounselorId = "Counselor_1", // Simplified
                            toTeacherId = currentEntry?.student?.currentClass ?: "Teacher_1",
                            summary = noteSummary,
                            requestType = noteRequestType
                        )
                        showNoteDialog = false
                        Toast.makeText(context, "已傳送給導師", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("送出") }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("取消") }
            }
        )
    }

    val calendar = Calendar.getInstance()
    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            viewModel.scheduleAppointment(studentId, calendar.timeInMillis, scheduleType)
            Toast.makeText(context, "預約已排程", Toast.LENGTH_SHORT).show()
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (showScheduleDialog) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text("新增預約") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val typeOptions = listOf("初談", "後續", "電訪", "家訪")
                    var expandedType by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedType,
                        onExpandedChange = { expandedType = it }
                    ) {
                        OutlinedTextField(
                            value = scheduleType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("預約類型") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false }
                        ) {
                            typeOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { scheduleType = option; expandedType = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showScheduleDialog = false
                    datePickerDialog.show()
                }) { Text("下一步 (選時間)") }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) { Text("取消") }
            }
        )
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening(speechRecognizer, { isRecording = it }, { text -> sessionText += " " + text })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$studentName ($studentId)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = {
                        if (sessionText.isNotBlank()) {
                            viewModel.saveCaseLog(studentId, sessionText)
                            sessionText = ""
                            Toast.makeText(context, "紀錄已儲存", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "儲存")
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isRecording) {
                        speechRecognizer.stopListening()
                        isRecording = false
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Clear else Icons.Default.Mic,
                    contentDescription = "語音輸入",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                // Student Info Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val status = currentEntry?.profile?.status ?: "Active"
                        val legal = currentEntry?.profile?.legalStatus ?: "無"
                        val priority = currentEntry?.profile?.priority ?: "Normal"
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("目前狀態: $status [$priority]", fontWeight = FontWeight.Bold)
                                Text("法律狀態: $legal")
                            }
                            IconButton(onClick = { showStatusDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "編輯狀態")
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                if (currentEntry?.profile?.nextAppointment != null) {
                                    val date = Date(currentEntry!!.profile!!.nextAppointment!!)
                                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    Text("下次預約: ${format.format(date)}")
                                } else {
                                    Text("尚未安排預約")
                                }
                            }
                            Button(onClick = { showScheduleDialog = true }) {
                                Text("新增預約")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showCrisisDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("⚠ 通報危機事件")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showNoteDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("✉ 通知導師")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("本次晤談輸入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = sessionText,
                    onValueChange = { sessionText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    placeholder = { Text("錄音辨識文字會出現在此，也可手動輸入...") }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text("歷史紀錄 (加密儲存)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (logs.isEmpty()) {
                item {
                    Text("暫無歷史紀錄", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(logs) { log ->
                    LogItem(log, viewModel)
                }
            }

            if (crisisEvents.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("危機事件記錄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(crisisEvents) { event ->
                    CrisisEventItem(event)
                }
            }

            if (teacherNotes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("導師協作備忘歷史", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(teacherNotes) { note ->
                    TeacherNoteItem(note)
                }
            }
        }
    }
}

@Composable
fun TeacherNoteItem(note: CounselorTeacherNote) {
    val date = Date(note.createdAt)
    val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = note.requestType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Text(text = format.format(date), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note.summary, style = MaterialTheme.typography.bodySmall)
            if (note.isRead) {
                Text(text = "導師已閱", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun CrisisEventItem(event: CrisisEvent) {
    val date = Date(event.occurredAt)
    val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = event.eventType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text(text = format.format(date), style = MaterialTheme.typography.labelSmall)
            }
            Text(text = "嚴重度: ${event.severity}", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = event.actionTaken, style = MaterialTheme.typography.bodySmall)
            if (event.externalReferral || !event.referralUnit.isNullOrBlank()) {
                Text(text = "已轉介: ${event.referralUnit ?: "外部單位"}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LogItem(log: CaseLog, viewModel: CounselorViewModel) {
    val date = Date(log.timestamp)
    val format = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = format.format(date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(text = "${log.academicYear}學年 ${if(log.semester==1) "上" else "下"}", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = viewModel.decryptLogContent(log), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun startListening(
    speechRecognizer: SpeechRecognizer,
    setRecordingState: (Boolean) -> Unit,
    onResult: (String) -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
    }

    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { setRecordingState(true) }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { setRecordingState(false) }
        override fun onError(error: Int) { setRecordingState(false) }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    speechRecognizer.startListening(intent)
}
