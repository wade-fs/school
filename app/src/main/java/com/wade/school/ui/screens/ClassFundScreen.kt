package com.wade.school.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.ClassFundTransaction
import com.wade.school.data.local.entity.FundTransactionType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassFundScreen(
    classId: String,
    viewModel: CounselorViewModel = viewModel(),
    onBack: () -> Unit
) {
    val transactions by viewModel.getFundRecords(classId).collectAsState(initial = emptyList())
    val balance by viewModel.getFundBalance(classId).collectAsState(initial = 0)
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$classId 班費管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新增收支")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Balance Summary
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("目前結餘", style = MaterialTheme.typography.labelLarge)
                    Text("NT$ ${balance ?: 0}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                }
            }

            Text("收支紀錄", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
            
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("目前尚無任何收支紀錄", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(transactions) { trans ->
                        FundTransactionItem(trans)
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFundDialog(
            classId = classId,
            onDismiss = { showAddDialog = false },
            onSave = { transaction ->
                viewModel.saveFundTransaction(transaction)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun FundTransactionItem(trans: ClassFundTransaction) {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    ListItem(
        headlineContent = { Text(trans.description, fontWeight = FontWeight.Medium) },
        supportingContent = { Text("${sdf.format(Date(trans.transactionDate))} | ${trans.category}") },
        trailingContent = {
            Text(
                text = "${if (trans.type == FundTransactionType.INCOME) "+" else "-"}${trans.amount}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (trans.type == FundTransactionType.INCOME) Color(0xFF43A047) else MaterialTheme.colorScheme.error
            )
        }
    )
}

@Composable
fun AddFundDialog(classId: String, onDismiss: () -> Unit, onSave: (ClassFundTransaction) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("班費收繳") }
    var type by remember { mutableStateOf(FundTransactionType.INCOME) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增收支項目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = type == FundTransactionType.INCOME,
                        onClick = { type = FundTransactionType.INCOME },
                        label = { Text("收入") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = type == FundTransactionType.EXPENSE,
                        onClick = { type = FundTransactionType.EXPENSE },
                        label = { Text("支出") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金額 (NT$)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("類別 (如：文具、活動)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("說明") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toIntOrNull() ?: 0
                onSave(ClassFundTransaction(classId = classId, type = type, amount = amt, category = category, description = description))
            }) { Text("儲存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
