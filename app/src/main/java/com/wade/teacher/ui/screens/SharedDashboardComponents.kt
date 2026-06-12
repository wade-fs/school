package com.wade.teacher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DashboardActionCard(title: String, description: String, actionText: String, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClick) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun SubjectClassSwitcher(
    viewModel: SubjectTeacherViewModel = viewModel()
) {
    // Correctly collect the Flow as State
    val assignedClasses by viewModel.assignedClasses.collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("科任班級管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        assignedClasses.forEach { sc ->
            ClassCard(sc.classId, sc.subjectName, "下一堂課: ${sc.nextLessonTime}")
        }
    }
}

@Composable
fun ClassCard(className: String, subject: String, status: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = className, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = subject, style = MaterialTheme.typography.bodySmall)
                Text(text = status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun InteractionHub() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("親師生互動樞紐", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        DashboardActionCard("家長線上簽閱", "聯絡簿數位化簽收系統", "進入系統")
        DashboardActionCard("學生提問箱", "來自 101 班的 3 個物理疑問", "查看問題")
        DashboardActionCard("心情溫度計", "今日有 2 位學生回報低落", "關懷訪談")
    }
}
