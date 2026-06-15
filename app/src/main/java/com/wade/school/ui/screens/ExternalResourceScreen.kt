package com.wade.school.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalResourceScreen(
    onBack: () -> Unit,
    viewModel: CounselorViewModel = viewModel()
) {
    val resources by viewModel.externalResources.collectAsState(initial = emptyList())
    val context = LocalContext.current

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
                    ResourceCard(resource) {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${resource.phone}"))
                        context.startActivity(intent)
                    }
                }
            }

            if (otherResources.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("🏢 常用輔導與社福機構", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(otherResources) { resource ->
                    ResourceCard(resource) {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${resource.phone}"))
                        context.startActivity(intent)
                    }
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
fun ResourceCard(resource: ExternalResource, onDial: () -> Unit) {
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
                FilledIconButton(
                    onClick = onDial,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (resource.isEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Call, contentDescription = "撥打")
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
