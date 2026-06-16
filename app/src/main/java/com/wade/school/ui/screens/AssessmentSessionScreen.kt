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
import com.wade.school.data.local.entity.AssessmentSession
import com.wade.school.data.local.entity.SessionStatus
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentSessionScreen(
    templateId: String,
    onBack: () -> Unit,
    onNavigateToStudentPicker: (String, String, String) -> Unit, // sessionId, templateId, classId
    viewModel: CounselorViewModel = viewModel()
) {
    val classes by viewModel.classes.collectAsState()
    var selectedClass by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("發起施測") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
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
            Text("選擇目標班級", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn {
                items(classes) { className ->
                    ListItem(
                        headlineContent = { Text(className) },
                        modifier = Modifier.clickable { selectedClass = className }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    selectedClass?.let { className ->
                        val sessionId = UUID.randomUUID().toString()
                        val session = AssessmentSession(
                            sessionId = sessionId,
                            templateId = templateId,
                            targetClass = className,
                            conductedBy = "counselor_01",
                            scheduledAt = System.currentTimeMillis()
                        )
                        viewModel.startAssessmentSession(session)
                        onNavigateToStudentPicker(sessionId, templateId, className)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedClass != null
            ) {
                Text("選擇學生並開始")
            }
        }
    }
}
