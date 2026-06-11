package com.wade.teacher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(role: String, onBack: () -> Unit) {
    val roleTitle = roles.find { it.id == role }?.title ?: "未知角色"
    var selectedTabIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(text = "$roleTitle 工作台", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "2026年6月11日 星期四", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "通知")
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Person, contentDescription = "個人資料")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("首頁") },
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 }
                )
                if (role != "student" && role != "parent") {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = { Text("科任班級") },
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 }
                    )
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = null) },
                    label = { Text("互動") },
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> RoleFeatureContent(role = role)
                1 -> SubjectClassSwitcher()
                2 -> InteractionHub()
            }
        }
    }
}

@Composable
fun SubjectClassSwitcher() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("科任班級管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        ClassCard("101 班", "物理 (一)", "待批改作業: 2")
        ClassCard("102 班", "物理 (一)", "今日有課 (14:10)")
        ClassCard("205 班", "進階物理", "已完成進度: 75%")
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
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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

@Composable
fun DashboardActionCard(title: String, description: String, actionText: String) {
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
            TextButton(onClick = { /* TODO */ }) {
                Text(actionText)
            }
        }
    }
}
