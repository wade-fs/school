package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.AlertSeverity
import com.wade.school.data.local.entity.RiskAlert
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskAlertScreen(
    onBack: () -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    val alerts by viewModel.unreadAlerts.collectAsState()
    val format = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("高風險警示名單") },
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
            items(alerts) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (alert.severity) {
                            AlertSeverity.URGENT -> MaterialTheme.colorScheme.errorContainer
                            AlertSeverity.WARNING -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = when (alert.severity) {
                                    AlertSeverity.URGENT -> "緊急警示"
                                    AlertSeverity.WARNING -> "警告"
                                    else -> "注意"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (alert.severity == AlertSeverity.URGENT) MaterialTheme.colorScheme.error else Color.Unspecified
                            )
                            Text(format.format(Date(alert.triggeredAt)), style = MaterialTheme.typography.labelSmall)
                        }
                        Text(text = "學生 ID: ${alert.studentId}", style = MaterialTheme.typography.titleSmall)
                        Text(text = alert.reason, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                        
                        Button(
                            onClick = { viewModel.markAlertAsRead(alert.id) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("標記已處理")
                        }
                    }
                }
            }
        }
    }
}
