package com.wade.school.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.AssessmentTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentManagementScreen(
    onBack: () -> Unit,
    onNavigateToSession: (String) -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    val templates by viewModel.assessmentTemplates.collectAsState()

    // 初始化範本
    LaunchedEffect(Unit) {
        viewModel.initBuiltInTemplates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("量表測驗管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                Text("內建測驗模板", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(templates) { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigateToSession(template.templateId) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(template.name, style = MaterialTheme.typography.titleMedium)
                        Text(template.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
