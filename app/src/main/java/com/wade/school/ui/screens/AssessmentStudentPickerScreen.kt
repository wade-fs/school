package com.wade.school.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentStudentPickerScreen(
    sessionId: String,
    templateId: String,
    classId: String,
    onBack: () -> Unit,
    onNavigateToResponse: (String, String, String) -> Unit, // sessionId, templateId, studentId
    viewModel: CounselorViewModel = viewModel()
) {
    val students by viewModel.getStudentsByClass(classId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("選擇受測學生 - $classId") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            items(students) { student ->
                ListItem(
                    headlineContent = { Text(student.name) },
                    supportingContent = { Text("學號: ${student.studentId}") },
                    modifier = Modifier.clickable { 
                        onNavigateToResponse(sessionId, templateId, student.studentId) 
                    }
                )
            }
        }
    }
}
