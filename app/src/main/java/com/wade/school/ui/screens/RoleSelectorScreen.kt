package com.wade.school.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.FragmentActivity
import com.wade.school.util.BiometricHelper

data class TeacherRole(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

val roles = listOf(
    TeacherRole("homeroom", "導師", "出缺席管理、家長聯繫", Icons.Default.Home),
    TeacherRole("subject", "科任教師", "教材管理、作業評量", Icons.Default.Edit),
    TeacherRole("admin", "行政教師", "公文流程、校務行事曆", Icons.Default.Settings),
    TeacherRole("counseling", "輔導教師", "個案管理、心理健康", Icons.Default.Favorite),
    TeacherRole("dept_head", "科主任", "課程規劃、教師督導", Icons.Default.Face),
    TeacherRole("school_info", "學校資訊", "公告、聯絡資訊、校網", Icons.Default.School),
    TeacherRole("student", "學生", "繳交作業、課表查詢", Icons.Default.Person),
    TeacherRole("parent", "家長", "聯絡簿簽閱、請假申請", Icons.Default.AccountCircle)
)

@Composable
fun RoleSelectorScreen(
    onRoleSelected: (String) -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val isConfigured = schoolConfig.accessPin != null
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var showSetupDialog by remember { mutableStateOf(false) }
    var showPinGate by remember { mutableStateOf<String?>(null) } // Store role ID being gated

    LaunchedEffect(isConfigured) {
        if (!isConfigured) {
            showSetupDialog = true
        }
    }

    if (showSetupDialog) {
        SecuritySetupDialog(
            onDismiss = { if (isConfigured) showSetupDialog = false },
            onSave = { name, pin, useBio ->
                viewModel.setupSecurity(name, pin, useBio)
                showSetupDialog = false
            }
        )
    }

    if (showPinGate != null) {
        PinGateDialog(
            onDismiss = { showPinGate = null },
            onSuccess = { 
                val roleId = showPinGate!!
                showPinGate = null
                onRoleSelected(roleId)
            },
            viewModel = viewModel,
            useBiometric = schoolConfig.useBiometric
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = if (schoolConfig.ownerName != null) "您好，${schoolConfig.ownerName} 老師" else "歡迎使用",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "中學教師助手",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "請選擇角色以進入工作台",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(roles) { role ->
                RoleCard(role = role, onClick = {
                    if (role.id == "school_info") {
                        // 學校資訊不需要身份驗證，直接進入
                        onRoleSelected(role.id)
                    } else if (isConfigured) {
                        showPinGate = role.id
                    } else {
                        onRoleSelected(role.id)
                    }
                })
            }
        }
    }
}

@Composable
fun SecuritySetupDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var useBiometric by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val canBio = remember { BiometricHelper.canAuthenticate(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("初次使用安全設定") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("請設定您的名稱與存取密碼，以保護學生隱私資料。", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("教師姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("設定 PIN 碼 (4-6 位數字)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text("確認 PIN 碼") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (canBio) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useBiometric, onCheckedChange = { useBiometric = it })
                        Text("同時啟用指紋/生物辨識解鎖")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank() && pin.length >= 4 && pin == confirmPin) {
                        onSave(name, pin, useBiometric)
                    }
                },
                enabled = name.isNotBlank() && pin.length >= 4 && pin == confirmPin
            ) {
                Text("完成設定")
            }
        }
    )
}

@Composable
fun PinGateDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CounselorViewModel,
    useBiometric: Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is android.content.ContextWrapper) {
            if (c is androidx.fragment.app.FragmentActivity) break
            c = c.baseContext
        }
        c as? androidx.fragment.app.FragmentActivity
    }

    // 自動啟動生物辨識
    LaunchedEffect(Unit) {
        if (useBiometric && activity != null) {
            BiometricHelper.showBiometricPrompt(
                activity,
                onSuccess = { onSuccess() },
                onError = { /* 失敗或取消則留在 PIN 碼輸入介面 */ }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("安全驗證")
                if (useBiometric) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        if (activity != null) {
                            BiometricHelper.showBiometricPrompt(
                                activity,
                                onSuccess = { onSuccess() },
                                onError = { }
                            )
                        }
                    }) {
                        Icon(Icons.Default.Fingerprint, contentDescription = "使用生物辨識", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("請輸入 PIN 碼或使用生物辨識進入系統", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { 
                        if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                            pin = it
                            error = false
                            // 當輸入達到 4-6 位且正確時自動驗證
                            if (it.length >= 4 && viewModel.verifyPin(it)) {
                                onSuccess()
                            }
                        }
                    },
                    label = { Text("PIN 碼") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    singleLine = true,
                    isError = error,
                    modifier = Modifier.width(150.dp)
                )
                if (error) {
                    Text("密碼錯誤", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if (viewModel.verifyPin(pin)) {
                    onSuccess()
                } else {
                    error = true
                }
            }) { Text("驗證") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun RoleCard(role: TeacherRole, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = role.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = role.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = role.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
