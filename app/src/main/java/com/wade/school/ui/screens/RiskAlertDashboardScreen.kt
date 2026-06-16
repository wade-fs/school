package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.AlertSeverity
import com.wade.school.data.local.entity.RiskAlert
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskAlertDashboardScreen(
    onBack: () -> Unit,
    onNavigateToStudent: (String) -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    val alerts by viewModel.unreadAlerts.collectAsState()
    val format = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("輔導風險警示中心") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        }
    ) { paddingValues ->
        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("目前無待處理警示，一切安好。", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alerts) { alert ->
                    AlertCard(
                        alert = alert,
                        onViewStudent = { onNavigateToStudent(alert.studentId) },
                        onMarkHandled = { viewModel.markAlertAsRead(alert.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertCard(alert: RiskAlert, onViewStudent: () -> Unit, onMarkHandled: () -> Unit) {
    val format = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity) {
                AlertSeverity.URGENT -> MaterialTheme.colorScheme.errorContainer
                AlertSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = when (alert.severity) {
                        AlertSeverity.URGENT -> "🔴 緊急警示"
                        AlertSeverity.WARNING -> "🟡 警告"
                        else -> "🔵 注意"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(format.format(Date(alert.triggeredAt)), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "學生 ID: ${alert.studentId}", style = MaterialTheme.typography.titleSmall)
            Text(text = alert.reason, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onViewStudent) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("查看個案")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onMarkHandled) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("處理完成")
                }
            }
        }
    }
}
