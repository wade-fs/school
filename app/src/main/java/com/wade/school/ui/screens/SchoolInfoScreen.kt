package com.wade.school.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolInfoScreen(
    onBack: () -> Unit,
    viewModel: SchoolInfoViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showSchoolSelector by remember { mutableStateOf(false) }

    // 計算目前篩選後的公告和可用 tag
    val filtered = remember(state.announcements, state.selectedTag) {
        viewModel.filteredAnnouncements()
    }
    val tags = remember(state.announcements) {
        viewModel.availableTags()
    }

    if (showSchoolSelector) {
        SchoolSelectorDialog(
            schools = state.availableSchools,
            onDismiss = { showSchoolSelector = false },
            onSelect = {
                viewModel.selectSchool(it)
                showSchoolSelector = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.selectedSchool?.name ?: "學校資訊") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSchoolSelector = true }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "切換學校")
                    }
                    IconButton(onClick = { state.selectedSchool?.let { viewModel.loadAnnouncements(it, state.currentPage) } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新整理")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── 學校基本資訊卡片 ──────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = state.selectedSchool?.name ?: "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // 官網
                        val website = state.selectedSchool?.homeUrl ?: state.config?.schoolWebsite
                        if (!website.isNullOrBlank()) {
                            SchoolInfoRow(
                                icon = { Icon(Icons.Default.Language, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
                                label = "前往學校官網",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // 公告網址 (顯示目前的來源)
                        val announceUrl = state.selectedSchool?.announcementUrl
                        if (!announceUrl.isNullOrBlank()) {
                            SchoolInfoRow(
                                icon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) },
                                label = "開啟公告網頁",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(announceUrl))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // 地址 → Google Maps
                        val address = state.selectedSchool?.address ?: state.config?.address
                        if (!address.isNullOrBlank()) {
                            SchoolInfoRow(
                                icon = { Icon(Icons.Default.Place, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary) },
                                label = address,
                                onClick = {
                                    val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                                    val intent = Intent(Intent.ACTION_VIEW, geoUri)
                                    intent.setPackage("com.google.android.apps.maps")
                                    if (intent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(intent)
                                    } else {
                                        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                    }
                                }
                            )
                        }

                        // 電話
                        val phone = state.selectedSchool?.phone ?: state.config?.phone
                        if (!phone.isNullOrBlank()) {
                            SchoolInfoRow(
                                icon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary) },
                                label = phone,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }

            // ── 公告標題 ──────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Announcement, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "學校公告",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.isLoadingAnnouncements) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ── 分類 Tag 篩選列 ───────────────────────────────────────────
            if (tags.size > 1) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            FilterChip(
                                selected = state.selectedTag == tag,
                                onClick = { viewModel.selectTag(tag) },
                                label = { Text(tag, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }

            // ── 錯誤訊息 ──────────────────────────────────────────────────
            state.announcementError?.let { err ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            // 即使錯誤，也顯示官網連結
                            val website = state.selectedSchool?.homeUrl ?: state.config?.schoolWebsite
                            if (!website.isNullOrBlank()) {
                                SchoolInfoRow(
                                    icon = { Icon(Icons.Default.Language, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                                    label = "前往官網手動查詢",
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── 公告列表 ──────────────────────────────────────────────────
            if (filtered.isEmpty() && !state.isLoadingAnnouncements && state.announcementError == null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("目前沒有公告", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(filtered) { ann ->
                AnnouncementCard(ann = ann, onOpen = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ann.url))
                    context.startActivity(intent)
                })
            }

            // ── 分頁控制 ──────────────────────────────────────────────────
            if (state.totalPages > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.prevPage() },
                            enabled = state.currentPage > 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一頁")
                        }
                        Text(
                            "${state.currentPage} / ${state.totalPages}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(
                            onClick = { viewModel.nextPage() },
                            enabled = state.currentPage < state.totalPages
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一頁")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SchoolSelectorDialog(
    schools: List<SchoolWithAnnouncement>,
    onDismiss: () -> Unit,
    onSelect: (SchoolWithAnnouncement) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSchools = remember(searchQuery, schools) {
        if (searchQuery.isBlank()) schools
        else schools.filter { it.name.contains(searchQuery) || it.city.contains(searchQuery) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("選擇學校") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("搜尋學校名稱或縣市") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(filteredSchools) { school ->
                        ListItem(
                            headlineContent = { Text(school.name) },
                            supportingContent = { Text(school.city) },
                            leadingContent = { Icon(Icons.Default.School, null) },
                            modifier = Modifier.clickable { onSelect(school) }
                        )
                    }
                    if (filteredSchools.isEmpty()) {
                        item {
                            Text(
                                "找不到符合條件且有公告網址的學校",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SchoolInfoRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AnnouncementCard(
    ann: SchoolAnnouncement,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tag badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = ann.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = ann.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ann.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
