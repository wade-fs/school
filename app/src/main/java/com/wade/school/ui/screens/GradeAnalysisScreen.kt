@file:OptIn(ExperimentalLayoutApi::class)
package com.wade.school.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.Submission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeAnalysisScreen(
    classId: String,
    onBack: () -> Unit,
    viewModel: SubjectTeacherViewModel = viewModel()
) {
    val submissions by viewModel.getAllSubmissionsForClass(classId).collectAsState(initial = emptyList())
    val avgScore by viewModel.getAverageScoreForClass(classId).collectAsState(initial = 0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 班 - 成績分析") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO Export CSV */ }) {
                        Icon(Icons.Default.Download, contentDescription = "匯出報表")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Stats Overview
            item {
                StatsOverviewCard(avgScore, submissions.size)
            }

            // Grade Distribution Chart
            item {
                Text("成績分佈圖 (人)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                GradeDistributionChart(submissions)
            }

            // Individual Student List
            item {
                Text("學生個別表現", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            val studentGrades = submissions.groupBy { it.studentId }
            items(studentGrades.keys.toList()) { studentId ->
                val sSubmissions = studentGrades[studentId] ?: emptyList()
                val sName = sSubmissions.firstOrNull()?.studentName ?: "未知學生"
                val sAvg = sSubmissions.filter { it.score != null }.map { it.score!! }.average()
                
                StudentGradeItem(sName, studentId, sAvg)
            }
        }
    }
}

@Composable
fun StatsOverviewCard(avg: Double, totalSubmissions: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "%.1f".format(avg), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = "班級平均", style = MaterialTheme.typography.labelSmall)
            }
            VerticalDivider(modifier = Modifier.height(40.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$totalSubmissions", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(text = "累計作業數", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun GradeDistributionChart(submissions: List<Submission>) {
    val scores = submissions.mapNotNull { it.score }
    val distribution = IntArray(10) { 0 } // 0-9, 10-19... 90-100
    scores.forEach { 
        val index = (it / 10).coerceIn(0, 9)
        distribution[index]++
    }

    val maxCount = distribution.maxOrNull()?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.secondary

    Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 8.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barWidth = width / 10f
            
            distribution.forEachIndexed { index, count ->
                val barHeight = (count.toFloat() / maxCount) * (height - 40.dp.toPx())
                val left = index * barWidth
                val top = height - barHeight - 20.dp.toPx()
                
                drawRect(
                    color = barColor,
                    topLeft = Offset(left + 4.dp.toPx(), top),
                    size = androidx.compose.ui.geometry.Size(barWidth - 8.dp.toPx(), barHeight)
                )
                
                // Labels
                drawContext.canvas.nativeCanvas.drawText(
                    "${index * 10}",
                    left + barWidth / 2,
                    height - 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        color = android.graphics.Color.GRAY
                    }
                )
            }
        }
    }
}

@Composable
fun StudentGradeItem(name: String, id: String, avg: Double) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text("學號: $id") },
        trailingContent = {
            Text(
                text = if (avg.isNaN()) "無成績" else "%.1f".format(avg),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (avg < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    )
}
