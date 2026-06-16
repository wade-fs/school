package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        "CAREER_INTEREST" -> CareerInterestAnalysisView(res)
                        "STRESS_SCHOOL" -> StressAnalysisView(res)
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
fun CareerInterestAnalysisView(response: AssessmentResponse) {
    // 這裡我們需要一個 Holland 的雷達圖或是長條圖來顯示各類型分數
    // 我們可以調用之前寫好的邏輯
    val scores = remember(response.answersJson) { com.wade.school.util.HollandScoring.calculateScores(response.answersJson) }
    
    Column {
        Text(" Holland 類型得分：", style = MaterialTheme.typography.titleMedium)
        scores.forEach { (type, score) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(type, modifier = Modifier.width(30.dp), fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { score / 25f },
                    modifier = Modifier.weight(1f).height(8.dp).padding(vertical = 4.dp)
                )
                Text(score.toString(), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun StressAnalysisView(response: AssessmentResponse) {
    val score = response.totalScore ?: 0
    val risk = score >= 35 // 簡單門檻
    
    Column {
        Text("學業壓力分析", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { score / 60f },
            modifier = Modifier.fillMaxWidth().height(16.dp),
            color = if (risk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Text("總分: $score / 60", style = MaterialTheme.typography.bodyMedium)
        if (risk) {
            Text("壓力指數偏高，建議介入輔導。", color = MaterialTheme.colorScheme.error)
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
