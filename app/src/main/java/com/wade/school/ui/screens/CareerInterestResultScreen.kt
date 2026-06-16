package com.wade.school.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.wade.school.util.HollandScoring
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerInterestResultScreen(
    answersJson: String,
    onBack: () -> Unit
) {
    val scores = remember(answersJson) { HollandScoring.calculateScores(answersJson) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生涯興趣分析結果") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            Text("您的 Holland 興趣類型分佈：", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            // 簡易雷達圖
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                RadarChart(scores = scores)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            val sortedScores = scores.toList().sortedByDescending { it.second }
            Text("建議方向：${sortedScores[0].first} + ${sortedScores[1].first}", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun RadarChart(scores: Map<String, Int>) {
    val types = listOf("R", "I", "A", "S", "E", "C")
    val maxScore = 25f // 5題 * 5分
    
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        val radius = min(size.width, size.height) / 2
        
        // 繪製雷達網格 (簡單版)
        for (i in 0 until 6) {
            val angle = i * PI / 3
            val x = center.x + radius * cos(angle).toFloat()
            val y = center.y + radius * sin(angle).toFloat()
            drawLine(Color.Gray, center, androidx.compose.ui.geometry.Offset(x, y))
        }
        
        // 繪製分數路徑
        val path = Path()
        types.forEachIndexed { i, type ->
            val angle = i * PI / 3
            val score = scores[type]?.toFloat() ?: 0f
            val r = (score / maxScore) * radius
            val x = center.x + r * cos(angle).toFloat()
            val y = center.y + r * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, Color.Blue, style = Stroke(width = 4f))
    }
}
