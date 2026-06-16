package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.AssessmentQuestion
import com.wade.school.data.local.entity.AssessmentResponse
import com.wade.school.data.local.entity.QuestionType
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentResponseScreen(
    sessionId: String,
    templateId: String,
    studentId: String,
    onBack: () -> Unit,
    onNavigateToResult: (String) -> Unit, // 新增
    viewModel: CounselorViewModel = viewModel()
) {
    val questions by viewModel.getQuestions(templateId).collectAsState(initial = emptyList())
    val answers = remember { mutableStateMapOf<Int, Int>() }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("填寫測驗") }) }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            items(questions) { q ->
                // ... (RadioButton logic)
            }
            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val json = JSONObject()
                        var totalScore = 0
                        var q9Score = 0
                        answers.forEach { (qId, score) ->
                            json.put(qId.toString(), score)
                            totalScore += score
                            if (questions.find { it.id == qId }?.order == 9) q9Score = score
                        }
                        
                        viewModel.saveAssessmentResponse(
                            AssessmentResponse(
                                sessionId = sessionId,
                                studentId = studentId,
                                answersJson = json.toString(),
                                totalScore = totalScore,
                                riskFlagged = (q9Score > 0 || totalScore >= 15)
                            )
                        )
                        
                        // 若為生涯量表，導向結果頁
                        if (templateId == "CAREER_INTEREST") {
                            onNavigateToResult(json.toString())
                        } else {
                            android.widget.Toast.makeText(context, "測驗已送出", android.widget.Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    }
                ) { Text("送出測驗") }
            }
        }
    }
}
