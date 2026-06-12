@file:OptIn(ExperimentalLayoutApi::class)
package com.wade.teacher.ui.screens

import androidx.compose.foundation.clickable
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
import com.wade.teacher.data.local.entity.LearningMaterial
import com.wade.teacher.data.local.entity.LessonPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlanScreen(
    onBack: () -> Unit,
    viewModel: SubjectTeacherViewModel = viewModel()
) {
    val lessonPlans by viewModel.allLessonPlans.collectAsState(initial = emptyList())
    var selectedPlan by remember { mutableStateOf<LessonPlan?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (selectedPlan != null) {
        LessonPlanDetailScreen(
            plan = selectedPlan!!,
            onBack = { selectedPlan = null },
            viewModel = viewModel
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("108 課綱教案庫") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "新增教案")
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (lessonPlans.isEmpty()) {
                    item {
                        Text("尚未建立教案，點擊右上角「+」開始。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                items(lessonPlans) { plan ->
                    LessonPlanCard(plan, onClick = { selectedPlan = plan })
                }
            }
        }

        if (showAddDialog) {
            AddLessonPlanDialog(
                onDismiss = { showAddDialog = false },
                onSave = { plan ->
                    viewModel.saveLessonPlan(plan, emptyList())
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun LessonPlanCard(plan: LessonPlan, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = plan.topic, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SuggestionChip(
                    onClick = { },
                    label = { Text("${plan.grade}年級", fontSize = 10.sp) }
                )
            }
            Text(text = plan.subjectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Competency Tags
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                plan.competencies.split(",").forEach { tag ->
                    if (tag.isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(tag, fontSize = 9.sp) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = plan.content, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            
            if (!plan.prepNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text(" 已有備課筆記", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlanDetailScreen(
    plan: LessonPlan,
    onBack: () -> Unit,
    viewModel: SubjectTeacherViewModel
) {
    val materials by viewModel.getMaterialsForPlan(plan.id).collectAsState(initial = emptyList())
    var showAddMaterial by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan.topic) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Info
            item {
                Text("基本資訊", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("科目: ${plan.subjectName} | 年級: ${plan.grade}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("教學目標與核心素養:", style = MaterialTheme.typography.labelLarge)
                        Text(plan.competencies, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Sprint 2-C: Preparation Notes
            item {
                Text("備課筆記 (私教紀錄)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = plan.prepNotes ?: "",
                    onValueChange = { /* In a real app, update via VM */ },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("記錄教學心得、班級微調重點") },
                    minLines = 3,
                    placeholder = { Text("本節課重點在於引發學生對慣性的興趣...") }
                )
            }

            // Sprint 2-B: Digital Materials
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("數位教材夾", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { showAddMaterial = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("新增教材")
                    }
                }
            }

            if (materials.isEmpty()) {
                item {
                    Text("目前沒有掛載教材，支援 PDF、影片連結等。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(materials) { material ->
                MaterialItem(material)
            }
        }
    }

    if (showAddMaterial) {
        AddMaterialDialog(
            onDismiss = { showAddMaterial = false },
            onSave = { title, type, url ->
                viewModel.addMaterialToPlan(LearningMaterial(lessonPlanId = plan.id, title = title, type = type, url = url))
                showAddMaterial = false
            }
        )
    }
}

@Composable
fun MaterialItem(material: LearningMaterial) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        ListItem(
            headlineContent = { Text(material.title) },
            supportingContent = { Text("${material.type}: ${material.url}", maxLines = 1) },
            leadingContent = {
                val icon = when (material.type) {
                    "Video" -> Icons.Default.PlayCircle
                    "PDF" -> Icons.Default.PictureAsPdf
                    else -> Icons.Default.Link
                }
                Icon(icon, contentDescription = null)
            },
            trailingContent = {
                IconButton(onClick = { /* Open URL */ }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = "開啟")
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun AddMaterialDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PDF") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增教材") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("教材名稱") }, modifier = Modifier.fillMaxWidth())
                Text("類型", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PDF", "Video", "Link").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                    }
                }
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("連結 / 路徑") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, type, url) }) { Text("新增") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun AddLessonPlanDialog(onDismiss: () -> Unit, onSave: (LessonPlan) -> Unit) {
    var topic by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("物理") }
    var grade by remember { mutableStateOf("10") }
    var competencies by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增 108 教案") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("主題/單元") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("科目") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("年級") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = competencies, onValueChange = { competencies = it }, label = { Text("核心素養 (以逗號分隔)") }, placeholder = { Text("例如：系統思考, 溝通互動") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("教學簡述") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(LessonPlan(topic = topic, subjectName = subject, grade = grade.toIntOrNull() ?: 10, competencies = competencies, content = content))
            }) { Text("儲存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
