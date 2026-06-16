package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var manualContent by remember { mutableStateOf("載入中...") }

    LaunchedEffect(Unit) {
        // 從 assets 讀取 USER_MANUAL.md
        // 請確保您已將 docs/USER_MANUAL.md 複製到 app/src/main/assets/USER_MANUAL.md
        try {
            context.assets.open("USER_MANUAL.md").use { inputStream ->
                manualContent = inputStream.bufferedReader().use(BufferedReader::readText)
            }
        } catch (e: Exception) {
            manualContent = "無法載入使用手冊: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("使用手冊") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 簡單的 Markdown 顯示方式 (處理標題與分段)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            manualContent.split("\n").forEach { line ->
                when {
                    line.startsWith("# ") -> Text(line.removePrefix("# "), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 8.dp))
                    line.startsWith("## ") -> Text(line.removePrefix("## "), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 4.dp))
                    line.startsWith("* ") -> Text("• ${line.removePrefix("* ")}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 16.dp, vertical = 2.dp))
                    line.startsWith("|") -> {} // 簡單過濾表格，後續可擴充
                    line.isBlank() -> Spacer(modifier = Modifier.height(8.dp))
                    else -> Text(line, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}
