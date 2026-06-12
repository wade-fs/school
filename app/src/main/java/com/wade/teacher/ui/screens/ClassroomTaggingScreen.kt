@file:OptIn(ExperimentalLayoutApi::class)
package com.wade.teacher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.teacher.data.local.entity.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomTaggingScreen(
    classId: String,
    onBack: () -> Unit,
    viewModel: SubjectTeacherViewModel = viewModel()
) {
    val students by viewModel.getStudentsInClass(classId).collectAsState(initial = emptyList())
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 班 - 課堂表現標記") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text("點擊學生頭像進行快速標記", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students) { student ->
                    StudentGridItem(student) {
                        selectedStudent = student
                        showTagDialog = true
                    }
                }
            }
        }
    }

    if (showTagDialog && selectedStudent != null) {
        PerformanceTagDialog(
            studentName = selectedStudent!!.name,
            onDismiss = { showTagDialog = false },
            onTagSelected = { tagName ->
                viewModel.markStudentPerformance(selectedStudent!!.studentId, classId, tagName)
                showTagDialog = false
            }
        )
    }
}

@Composable
fun StudentGridItem(student: Student, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = student.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "${student.seatNo}號", fontSize = 10.sp)
        }
    }
}

@Composable
fun PerformanceTagDialog(studentName: String, onDismiss: () -> Unit, onTagSelected: (String) -> Unit) {
    val positiveTags = listOf("發言踴躍", "小組領導", "助人為樂", "創意亮點")
    val neutralTags = listOf("未帶課本", "作業遲交", "分心提醒")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("為 $studentName 標記表現") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("正面表現", style = MaterialTheme.typography.labelSmall, color = Color(0xFF43A047))
                FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                    positiveTags.forEach { tag ->
                        SuggestionChip(
                            onClick = { onTagSelected(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
                
                Text("待改進項目", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                    neutralTags.forEach { tag ->
                        SuggestionChip(
                            onClick = { onTagSelected(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun FlowRow(
    mainAxisSpacing: androidx.compose.ui.unit.Dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
        verticalArrangement = Arrangement.spacedBy(crossAxisSpacing)
    ) {
        content()
    }
}
