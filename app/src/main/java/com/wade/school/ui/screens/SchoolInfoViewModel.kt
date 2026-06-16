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
import java.io.BufferedReader
import java.io.InputStreamReader

data class SchoolWithAnnouncement(
    val name: String,
    val homeUrl: String,
    val announcementUrl: String,
    val city: String = "",
    val address: String = "",
    val phone: String = ""
)

data class SchoolAnnouncement(
    val tag: String,
    val title: String,
    val date: String,
    val url: String
)

data class SchoolInfoUiState(
    val selectedSchool: SchoolWithAnnouncement? = null,
    val availableSchools: List<SchoolWithAnnouncement> = emptyList(),
    val announcements: List<SchoolAnnouncement> = emptyList(),
    val isLoadingAnnouncements: Boolean = false,
    val announcementError: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val selectedTag: String = "全部",
    val config: SchoolConfig? = null
)

class SchoolInfoViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getDatabase(app)
    private val _state = MutableStateFlow(SchoolInfoUiState())
    val state: StateFlow<SchoolInfoUiState> = _state

    init {
        loadSchoolsAndInitialize()
    }

    private fun loadSchoolsAndInitialize() {
        viewModelScope.launch(Dispatchers.IO) {
            val config = db.counselorDao().getSchoolConfig().first()
            
            // Load schools from high.csv in assets
            val allSchools = loadSchoolsFromAssets()
            val filteredSchools = allSchools.filter { it.announcementUrl.isNotBlank() }
            
            // Default to current school
            val currentSchoolName = config?.schoolName ?: ""
            var initialSchool = filteredSchools.find { it.name == currentSchoolName }
            
            // If not found in filtered list, try to create one from config
            if (initialSchool == null && !config?.schoolWebsite.isNullOrBlank()) {
                initialSchool = SchoolWithAnnouncement(
                    name = config?.schoolName ?: "本校",
                    homeUrl = config?.schoolWebsite ?: "",
                    announcementUrl = config?.schoolWebsite ?: "", // Fallback
                    city = "",
                    address = config?.address ?: "",
                    phone = config?.phone ?: ""
                )
            }

            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    config = config,
                    availableSchools = filteredSchools,
                    selectedSchool = initialSchool
                )
                
                if (initialSchool != null) {
                    loadAnnouncements(initialSchool, page = 1)
                }
            }
        }
    }

    private fun loadSchoolsFromAssets(): List<SchoolWithAnnouncement> {
        val result = mutableListOf<SchoolWithAnnouncement>()
        try {
            getApplication<Application>().assets.open("high.csv").use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                reader.readLine() // skip header
                reader.forEachLine { line ->
                    val tokens = line.split(",").map { it.trim().removeSurrounding("\"") }
                    if (tokens.size >= 9) {
                        val name = tokens[2]
                        val homeUrl = tokens[7]
                        val announceUrl = tokens[8]
                        if (name.isNotBlank()) {
                            result.add(SchoolWithAnnouncement(
                                name = name,
                                homeUrl = homeUrl,
                                announcementUrl = announceUrl,
                                city = tokens[4].replace(Regex("\\[.*?\\]"), ""),
                                address = tokens[5].replace(Regex("\\[.*?\\]"), ""),
                                phone = tokens[6]
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SchoolInfoViewModel", "Error loading high.csv", e)
        }
        return result
    }

    fun selectSchool(school: SchoolWithAnnouncement) {
        _state.value = _state.value.copy(
            selectedSchool = school,
            announcements = emptyList(),
            announcementError = null,
            currentPage = 1,
            totalPages = 1
        )
        loadAnnouncements(school, page = 1)
    }

    fun loadAnnouncements(school: SchoolWithAnnouncement, page: Int = 1) {
        val rawUrl = school.announcementUrl
        if (rawUrl.isBlank()) return
        
        _state.value = _state.value.copy(isLoadingAnnouncements = true, announcementError = null)

        viewModelScope.launch {
            try {
                // If it's a generic Rpage site and doesn't end with a specific page number, try to append page logic
                val url = if (rawUrl.contains("/p/428-1000-") && rawUrl.endsWith(".php")) {
                    rawUrl.replace(Regex("""/p/428-1000-\d+\.php"""), "/p/428-1000-$page.php")
                } else if (rawUrl.contains("/p/428-1000-")) {
                    // Just a guess if it contains the pattern but not the full filename
                    rawUrl.substringBefore("/p/428-1000-") + "/p/428-1000-$page.php"
                } else {
                    // For others, we might only support page 1 or have to implement custom logic
                    rawUrl
                }

                val (items, totalPages) = withContext(Dispatchers.IO) {
                    val doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(15_000)
                        .get()

                    // 解析公告 table 或 list
                    // 我們使用更寬鬆的解析方式
                    val announcements = mutableListOf<SchoolAnnouncement>()
                    
                    // 方式 1: Table 結構 (Rpage/iSchool 常見)
                    doc.select("table").forEach { table ->
                        table.select("tr").forEach { row ->
                            val cells = row.select("td")
                            if (cells.size >= 2) {
                                val possibleDates = cells.map { it.text().trim() }.filter {
                                    it.matches(Regex(""".*\d{4}[-/]\d{2}[-/]\d{2}.*|.*\d{3}[-/]\d{2}[-/]\d{2}.*"""))
                                }
                                
                                if (possibleDates.isNotEmpty()) {
                                    val dateText = possibleDates.first()
                                    val link = row.selectFirst("a")
                                    val title = link?.text()?.trim() ?: cells.firstOrNull { it.text().trim() != dateText }?.text()?.trim() ?: "公告"
                                    val href = link?.attr("href") ?: ""
                                    val fullUrl = urljoin(url, href)
                                    announcements.add(SchoolAnnouncement("公告", title, dateText, fullUrl))
                                }
                            }
                        }
                    }

                    // 方式 2: Div 結構 (板橋高中等常見)
                    if (announcements.isEmpty()) {
                        doc.select("div[class*=news], div[class*=item], div[class*=list]").forEach { div ->
                            val text = div.text().trim()
                            val dateMatch = Regex("""\d{4}[-/]\d{2}[-/]\d{2}|\d{3}[-/]\d{2}[-/]\d{2}""").find(text)
                            if (dateMatch != null) {
                                val link = div.selectFirst("a")
                                val title = link?.text()?.trim() ?: text.replace(dateMatch.value, "").trim()
                                val href = link?.attr("href") ?: ""
                                val fullUrl = urljoin(url, href)
                                announcements.add(SchoolAnnouncement("公告", title, dateMatch.value, fullUrl))
                            }
                        }
                    }

                    // 方式 3: 如果 Table/Div 沒抓到，試試 li
                    if (announcements.isEmpty()) {
                        doc.select("li").forEach { li ->
                            val link = li.selectFirst("a")
                            val text = li.text()
                            if (link != null && text.matches(Regex(""".*\d{4}[-/]\d{2}[-/]\d{2}.*|.*\d{3}[-/]\d{2}[-/]\d{2}.*"""))) {
                                val title = link.text().trim()
                                val href = link.attr("href")
                                val fullUrl = urljoin(url, href)
                                // 嘗試從文字中提取日期
                                val date = Regex("""\d{4}[-/]\d{2}[-/]\d{2}|\d{3}[-/]\d{2}[-/]\d{2}""").find(text)?.value ?: ""
                                announcements.add(SchoolAnnouncement("公告", title, date, fullUrl))
                            }
                        }
                    }

                    // 解析分頁 (Rpage 特色)
                    val maxPage = if (url.contains("/p/428-1000-")) {
                        val pageLinks = doc.select("a[href]")
                            .map { it.attr("href") }
                            .filter { it.contains("/p/428-1000-") }
                        pageLinks.mapNotNull { Regex("""/p/428-1000-(\d+)\.php""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
                            .maxOrNull() ?: page
                    } else page

                    Pair(announcements.distinctBy { it.url }, maxPage)
                }

                _state.value = _state.value.copy(
                    announcements = items,
                    currentPage = page,
                    totalPages = totalPages,
                    isLoadingAnnouncements = false,
                    announcementError = if (items.isEmpty()) "無法自動解析公告列表，請直接前往官網查閱。" else null
                )
            } catch (e: Exception) {
                android.util.Log.e("SchoolInfoViewModel", "Announcement load failed", e)
                _state.value = _state.value.copy(
                    isLoadingAnnouncements = false,
                    announcementError = "無法自動解析公告列表，請直接前往官網查閱。"
                )
            }
        }
    }

    private fun urljoin(base: String, relative: String): String {
        if (relative.startsWith("http")) return relative
        return try {
            val baseUrl = java.net.URL(base)
            java.net.URL(baseUrl, relative).toString()
        } catch (e: Exception) {
            relative
        }
    }

    fun nextPage() {
        val s = _state.value
        if (s.currentPage < s.totalPages && s.selectedSchool != null) {
            loadAnnouncements(s.selectedSchool, s.currentPage + 1)
        }
    }

    fun prevPage() {
        val s = _state.value
        if (s.currentPage > 1 && s.selectedSchool != null) {
            loadAnnouncements(s.selectedSchool, s.currentPage - 1)
        }
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
