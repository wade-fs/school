package com.wade.school.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.ExternalResource
import com.wade.school.data.local.entity.ReferralRecord
import com.wade.school.data.local.entity.StudentWithProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralDialog(
    resourceName: String,
    resourceId: Int,
    students: List<StudentWithProfile>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("轉介個案至 $resourceName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 學生選擇器
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStudentId?.let { id -> students.find { it.student.studentId == id }?.student?.name } ?: "選擇學生",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("目標學生") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        students.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.student.name} (${s.student.studentId})") },
                                onClick = { 
                                    selectedStudentId = s.student.studentId
                                    expanded = false 
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("轉介原因") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                selectedStudentId?.let { id ->
                    onSave(id, reason)
                }
            }, enabled = selectedStudentId != null) { Text("確認轉介") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalResourceScreen(
    onBack: () -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    val resources by viewModel.externalResources.collectAsState(initial = emptyList())
    val students by viewModel.studentsWithProfiles.collectAsState()
    val context = LocalContext.current
    var showReferralDialog by remember { mutableStateOf<ExternalResource?>(null) }

    if (showReferralDialog != null) {
        ReferralDialog(
            resourceName = showReferralDialog!!.name,
            resourceId = showReferralDialog!!.id,
            students = students,
            onDismiss = { showReferralDialog = null },
            onSave = { studentId, reason ->
                viewModel.insertReferral(ReferralRecord(
                    studentId = studentId,
                    resourceId = showReferralDialog!!.id,
                    resourceName = showReferralDialog!!.name,
                    referredBy = "counselor_01",
                    reason = reason
                ))
                android.widget.Toast.makeText(context, "已新增轉介紀錄", android.widget.Toast.LENGTH_SHORT).show()
                showReferralDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("校外輔導資源庫", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val emergencyResources = resources.filter { it.isEmergency }
            val otherResources = resources.filter { !it.isEmergency }

            if (emergencyResources.isNotEmpty()) {
                item {
                    Text("🚨 緊急求助專線 (24小時)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(emergencyResources) { resource ->
                    ResourceCard(
                        resource = resource,
                        onDial = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${resource.phone}"))
                            context.startActivity(intent)
                        },
                        onRefer = { showReferralDialog = resource }
                    )
                }
            }

            if (otherResources.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("🏢 常用輔導與社福機構", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(otherResources) { resource ->
                    ResourceCard(
                        resource = resource,
                        onDial = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${resource.phone}"))
                            context.startActivity(intent)
                        },
                        onRefer = { showReferralDialog = resource }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("提示：點擊撥打按鈕將直接開啟撥號介面。部分專線可能因地區而有不同撥打方式。", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceCard(resource: ExternalResource, onDial: () -> Unit, onRefer: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (resource.isEmergency) 
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                 else 
                    CardDefaults.cardColors()
    ) {
        ListItem(
            headlineContent = { Text(resource.name, fontWeight = FontWeight.Bold) },
            supportingContent = { 
                Column {
                    Text("電話: ${resource.phone}")
                    Text("類型: ${resource.type}", style = MaterialTheme.typography.labelSmall)
                    resource.city?.let { Text("地區: $it", style = MaterialTheme.typography.labelSmall) }
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = onDial,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (resource.isEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "撥打")
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onRefer) {
                        Icon(Icons.Default.Add, contentDescription = "轉介")
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
