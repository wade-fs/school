package com.wade.school.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wade.school.data.local.AppDatabase
import com.wade.school.data.local.entity.MoeSchool
import com.wade.school.data.local.entity.SchoolConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class SchoolAnnouncement(
    val tag: String,
    val title: String,
    val date: String,
    val url: String
)

data class SchoolInfoUiState(
    val school: MoeSchool? = null,
    val config: SchoolConfig? = null,
    val announcements: List<SchoolAnnouncement> = emptyList(),
    val isLoadingAnnouncements: Boolean = false,
    val announcementError: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val selectedTag: String = "全部"
)

class SchoolInfoViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getDatabase(app)
    private val _state = MutableStateFlow(SchoolInfoUiState())
    val state: StateFlow<SchoolInfoUiState> = _state

    init {
        viewModelScope.launch {
            val config = db.counselorDao().getSchoolConfig().first()
            _state.value = _state.value.copy(config = config)

            // 用校名從 moe_schools 找出完整資料
            config?.schoolName?.let { name ->
                val moe = db.counselorDao().searchSchools(name).first().firstOrNull()
                _state.value = _state.value.copy(school = moe)
            }

            // 根據 config 裡的官網推導公告 URL
            loadAnnouncements(page = 1)
        }
    }

    /**
     * 依學校官網 URL 推導彙整公告頁。
     *
     * 已知台灣校務行政系統（校園網站）的公告頁 URL 格式：
     *   https://<domain>/p/428-1000-<page>.php
     * 其中 428 = 「彙整公告模組」的固定 module id。
     */
    private fun buildAnnouncementListUrl(baseUrl: String, page: Int): String {
        val clean = baseUrl.trimEnd('/')
        return "$clean/p/428-1000-$page.php"
    }

    fun loadAnnouncements(page: Int = 1) {
        val baseUrl = _state.value.config?.schoolWebsite ?: return
        _state.value = _state.value.copy(isLoadingAnnouncements = true, announcementError = null)

        viewModelScope.launch {
            try {
                val url = buildAnnouncementListUrl(baseUrl, page)
                val (items, totalPages) = withContext(Dispatchers.IO) {
                    val doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Android 14; Mobile)")
                        .timeout(12_000)
                        .get()

                    // 解析公告 table：每 row 3 格 = tag / title / date
                    val rows = doc.select("table tr").filter { row ->
                        val cells = row.select("td")
                        cells.size == 3 &&
                            cells[2].text().trim().matches(Regex("""\d{4}-\d{2}-\d{2}"""))
                    }

                    val announcements = rows.map { row ->
                        val cells = row.select("td")
                        val tag   = cells[0].text().trim()
                        val link  = cells[1].selectFirst("a")
                        val title = link?.text()?.trim() ?: cells[1].text().trim()
                        val href  = link?.attr("href") ?: ""
                        val date  = cells[2].text().trim()
                        val clean2 = baseUrl.trimEnd('/')
                        val fullUrl = if (href.startsWith("http")) href else "$clean2$href"
                        SchoolAnnouncement(tag, title, date, fullUrl)
                    }

                    // 解析分頁
                    val pageLinks = doc.select("a[href]")
                        .map { it.attr("href") }
                        .filter { it.contains("/p/428-1000-") }
                    val maxPage = pageLinks
                        .mapNotNull { Regex("""/p/428-1000-(\d+)\.php""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
                        .maxOrNull() ?: page

                    Pair(announcements, maxPage)
                }

                _state.value = _state.value.copy(
                    announcements = items,
                    currentPage = page,
                    totalPages = totalPages,
                    isLoadingAnnouncements = false,
                    announcementError = if (items.isEmpty()) "無法取得公告，請確認網路或學校官網設定。" else null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoadingAnnouncements = false,
                    announcementError = "網路錯誤：${e.message}"
                )
            }
        }
    }

    fun nextPage() {
        val s = _state.value
        if (s.currentPage < s.totalPages) loadAnnouncements(s.currentPage + 1)
    }

    fun prevPage() {
        val s = _state.value
        if (s.currentPage > 1) loadAnnouncements(s.currentPage - 1)
    }

    fun selectTag(tag: String) {
        _state.value = _state.value.copy(selectedTag = tag)
    }

    fun filteredAnnouncements(): List<SchoolAnnouncement> {
        val s = _state.value
        return if (s.selectedTag == "全部") s.announcements
        else s.announcements.filter { it.tag == s.selectedTag }
    }

    fun availableTags(): List<String> =
        listOf("全部") + _state.value.announcements.map { it.tag }.distinct().sorted()
}
