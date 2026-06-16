package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.AssessmentResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentResultScreen(
    sessionId: String,
    templateId: String,
    studentId: String,
    onBack: () -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    val response by viewModel.getAssessmentResponse(sessionId, studentId).collectAsState(initial = null)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("測驗結果分析") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        }
    ) { paddingValues ->
        response?.let { res ->
            LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                item {
                    Text("受測日期: ${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(res.completedAt))}")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (templateId) {
                        "PHQ9_TW", "GAD7_TW" -> RiskScoreView(res)
                        "CAREER_INTEREST" -> { /* 原本的 CareerInterestResultScreen 邏輯可併入或呼叫 */ }
                        else -> Text("總分: ${res.totalScore ?: "無"}")
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun RiskScoreView(response: AssessmentResponse) {
    val score = response.totalScore ?: 0
    val riskLevel = when {
        score >= 15 -> "嚴重"
        score >= 10 -> "中度"
        score >= 5 -> "輕度"
        else -> "無明顯"
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("評估總分: $score", style = MaterialTheme.typography.headlineMedium)
            Text("風險程度: $riskLevel", style = MaterialTheme.typography.titleLarge)
        }
    }
}
