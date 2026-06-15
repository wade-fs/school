package com.wade.school.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wade.school.data.local.entity.DocCategory
import com.wade.school.data.local.entity.DocStatus
import com.wade.school.data.local.entity.OfficialDocument
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("MM/dd", Locale.TAIWAN)
private val dateFullFmt = SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialDocumentScreen(
    onBack: () -> Unit,
    viewModel: OfficialDocumentViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // 若 DB 是空的，種入範例資料以便看效果
    LaunchedEffect(Unit) {
        viewModel.seedSampleData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("公文管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.pendingUrgentCount > 0) {
                        BadgedBox(badge = {
                            Badge { Text(state.pendingUrgentCount.toString()) }
                        }) {
                            Icon(Icons.Default.NotificationImportant, contentDescription = "急件", tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = { viewModel.openAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "新增公文")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Tab 列 ──
            TabRow(selectedTabIndex = DocTab.entries.indexOf(state.selectedTab)) {
                DocTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick  = { viewModel.selectTab(tab) },
                        text     = { Text(tab.label) }
                    )
                }
            }

            // ── 搜尋欄 ──
            OutlinedTextField(
                value         = state.searchQuery,
                onValueChange = { viewModel.setSearch(it) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder   = { Text("搜尋公文字號或主旨…") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                trailingIcon  = {
                    if (state.searchQuery.isNotEmpty())
                        IconButton(onClick = { viewModel.setSearch("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                },
                singleLine    = true
            )

            // ── 公文列表 ──
            if (state.docs.isEmpty()) {
                Box(
                    modifier            = Modifier.fillMaxSize(),
                    contentAlignment    = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "目前沒有公文",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.docs, key = { it.docId }) { doc ->
                        DocCard(
                            doc      = doc,
                            onEdit   = { viewModel.openEditDialog(doc) },
                            onAdvance= { viewModel.advanceStatus(doc) },
                            onDelete = { viewModel.deleteDocument(doc.docId) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // ── 新增 / 編輯 Dialog ──
        if (state.showAddDialog) {
            DocEditDialog(
                existing   = state.editingDoc,
                onDismiss  = { viewModel.dismissDialog() },
                onSave     = { id, title, cat, status, urgent, deadline, note ->
                    viewModel.saveDocument(id, title, cat, status, urgent, deadline, note)
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DocCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DocCard(
    doc      : OfficialDocument,
    onEdit   : () -> Unit,
    onAdvance: () -> Unit,
    onDelete : () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (doc.isUrgent)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ── 頂列：字號 + 急件 + 狀態 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (doc.isUrgent) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            "急件",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    doc.docId,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                DocStatusChip(doc.status)
            }

            Spacer(Modifier.height(6.dp))

            // ── 主旨 ──
            Text(
                doc.title,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines   = if (expanded) Int.MAX_VALUE else 2,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.clickable { expanded = !expanded }
            )

            Spacer(Modifier.height(6.dp))

            // ── 下方資訊列 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 類別 badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        doc.category.label,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                // 期限
                doc.deadline?.let { dl ->
                    val isOverdue = dl < System.currentTimeMillis() && doc.status !in listOf(DocStatus.ARCHIVED, DocStatus.SENT)
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint     = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        dateFmt.format(Date(dl)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))

                // 操作按鈕
                if (doc.status != DocStatus.ARCHIVED) {
                    TextButton(
                        onClick      = onAdvance,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            nextStepLabel(doc.status),
                            fontSize = 12.sp
                        )
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, modifier = Modifier.size(16.dp))
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "編輯", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "刪除", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }

            // ── 備註（展開後顯示）──
            if (expanded && !doc.note.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text(
                    doc.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DocStatusChip(status: DocStatus) {
    val (label, containerColor, contentColor) = when (status) {
        DocStatus.DRAFT        -> Triple("起草中",  Color(0xFFE3F2FD), Color(0xFF1565C0))
        DocStatus.PENDING_SIGN -> Triple("待簽核",  Color(0xFFFFF3E0), Color(0xFFE65100))
        DocStatus.SIGNED       -> Triple("已簽核",  Color(0xFFE8F5E9), Color(0xFF2E7D32))
        DocStatus.SENT         -> Triple("已發文",  Color(0xFFF3E5F5), Color(0xFF6A1B9A))
        DocStatus.ARCHIVED     -> Triple("已歸檔",  Color(0xFFF5F5F5), Color(0xFF616161))
        DocStatus.REJECTED     -> Triple("退件",    Color(0xFFFFEBEE), Color(0xFFC62828))
    }
    Surface(shape = MaterialTheme.shapes.small, color = containerColor) {
        Text(
            label,
            style    = MaterialTheme.typography.labelSmall,
            color    = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun nextStepLabel(status: DocStatus) = when (status) {
    DocStatus.DRAFT        -> "送簽"
    DocStatus.PENDING_SIGN -> "完成簽核"
    DocStatus.SIGNED       -> "標記發文"
    DocStatus.SENT         -> "歸檔"
    DocStatus.REJECTED     -> "重新起草"
    DocStatus.ARCHIVED     -> ""
}

// ─────────────────────────────────────────────────────────────────────────────
// DocEditDialog — 新增 / 編輯公文
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocEditDialog(
    existing : OfficialDocument?,
    onDismiss: () -> Unit,
    onSave   : (String, String, DocCategory, DocStatus, Boolean, Long?, String?) -> Unit
) {
    var docId    by remember { mutableStateOf(existing?.docId ?: "") }
    var title    by remember { mutableStateOf(existing?.title ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: DocCategory.OTHER) }
    var status   by remember { mutableStateOf(existing?.status ?: DocStatus.DRAFT) }
    var isUrgent by remember { mutableStateOf(existing?.isUrgent ?: false) }
    var deadlineText by remember {
        mutableStateOf(existing?.deadline?.let { dateFullFmt.format(Date(it)) } ?: "")
    }
    var note     by remember { mutableStateOf(existing?.note ?: "") }

    var catExpanded  by remember { mutableStateOf(false) }
    var statExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "新增公文" else "編輯公文") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value         = docId,
                        onValueChange = { docId = it },
                        label         = { Text("公文字號") },
                        placeholder   = { Text("留空自動產生") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )
                }
                item {
                    OutlinedTextField(
                        value         = title,
                        onValueChange = { title = it },
                        label         = { Text("主旨 *") },
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
                item {
                    // 類別 dropdown
                    ExposedDropdownMenuBox(
                        expanded        = catExpanded,
                        onExpandedChange= { catExpanded = !catExpanded }
                    ) {
                        OutlinedTextField(
                            value            = category.label,
                            onValueChange    = {},
                            readOnly         = true,
                            label            = { Text("業務類別") },
                            trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                            modifier         = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded        = catExpanded,
                            onDismissRequest= { catExpanded = false }
                        ) {
                            DocCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text    = { Text(cat.label) },
                                    onClick = { category = cat; catExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    // 狀態 dropdown
                    ExposedDropdownMenuBox(
                        expanded        = statExpanded,
                        onExpandedChange= { statExpanded = !statExpanded }
                    ) {
                        OutlinedTextField(
                            value            = status.toLabel(),
                            onValueChange    = {},
                            readOnly         = true,
                            label            = { Text("目前狀態") },
                            trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(statExpanded) },
                            modifier         = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded        = statExpanded,
                            onDismissRequest= { statExpanded = false }
                        ) {
                            DocStatus.entries.forEach { s ->
                                DropdownMenuItem(
                                    text    = { Text(s.toLabel()) },
                                    onClick = { status = s; statExpanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value         = deadlineText,
                        onValueChange = { deadlineText = it },
                        label         = { Text("辦理期限 (yyyy-MM-dd)") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                        Text("急件", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    OutlinedTextField(
                        value         = note,
                        onValueChange = { note = it },
                        label         = { Text("備註") },
                        modifier      = Modifier.fillMaxWidth(),
                        minLines      = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    val deadline = runCatching { dateFullFmt.parse(deadlineText)?.time }.getOrNull()
                    onSave(docId, title, category, status, isUrgent, deadline, note)
                },
                enabled = title.isNotBlank()
            ) { Text("儲存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun DocStatus.toLabel() = when (this) {
    DocStatus.DRAFT        -> "起草中"
    DocStatus.PENDING_SIGN -> "待簽核"
    DocStatus.SIGNED       -> "已簽核"
    DocStatus.SENT         -> "已發文"
    DocStatus.ARCHIVED     -> "已歸檔"
    DocStatus.REJECTED     -> "退件"
}
