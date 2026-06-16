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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentSessionListScreen(
    onBack: () -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    val sessions by viewModel.allSessions.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("施測場次進度") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            items(sessions) { session ->
                val completedCount by viewModel.getCompletedCount(session.sessionId).collectAsState(initial = 0)
                
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("場次: ${session.targetClass}", style = MaterialTheme.typography.titleMedium)
                        Text("時間: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(session.scheduledAt))}")
                        Text("完成進度: $completedCount 人", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
