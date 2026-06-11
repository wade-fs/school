package com.wade.teacher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(role: String, onBack: () -> Unit) {
    val roleTitle = roles.find { it.id == role }?.title ?: "未知角色"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$roleTitle 工作台", fontWeight = FontWeight.Bold) },
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (role) {
                "homeroom" -> HomeroomDashboard()
                "subject" -> SubjectDashboard()
                "admin" -> AdminDashboard()
                "counseling" -> CounselingDashboard()
                "dept_head" -> DeptHeadDashboard()
                else -> Text("正在開發中...", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun HomeroomDashboard() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("今日重點", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        DashboardActionCard("快速點名", "班級出缺席即時紀錄", "30秒完成")
        DashboardActionCard("家長聯絡簿", "有 3 條未讀訊息", "前往回覆")
        DashboardActionCard("週記批閱", "目前剩餘 15 本待處理", "開始批閱")
    }
}

@Composable
fun SubjectDashboard() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("課程與評量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        DashboardActionCard("教材準備", "高一數學：多項式 (108課綱)", "查看模板")
        DashboardActionCard("AI 輔助批改", "已收集 40 份作業", "啟動分析")
        DashboardActionCard("學生成績分佈", "查看最近一次小考趨勢", "查看報表")
    }
}

@Composable
fun AdminDashboard() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("校務行政", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        DashboardActionCard("公文簽核", "有 2 件急件待處理", "立即處理")
        DashboardActionCard("場地借用", "體育館/視聽教室預約狀態", "管理預約")
        DashboardActionCard("校務行事曆", "下週：期中考週準備會議", "查看詳情")
    }
}

@Composable
fun CounselingDashboard() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("學生輔導 (加密環境)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(12.dp))
        DashboardActionCard("個案紀錄", "安全儲存與權限控管", "查看清單")
        DashboardActionCard("心情溫度計", "全班情緒預警分析", "查看警示")
        DashboardActionCard("晤談預約", "今日下午有 2 場預約", "管理時段")
    }
}

@Composable
fun DeptHeadDashboard() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("科務管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        DashboardActionCard("108課綱檢核", "選修課程學分結構檢索", "開始檢核")
        DashboardActionCard("跨科共備", "物理/化學聯合備課社群", "進入空間")
        DashboardActionCard("教學觀察", "本學期進度追蹤", "查看日誌")
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
