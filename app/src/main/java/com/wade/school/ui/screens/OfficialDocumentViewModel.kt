package com.wade.school.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wade.school.data.local.AppDatabase
import com.wade.school.data.local.entity.DocCategory
import com.wade.school.data.local.entity.DocStatus
import com.wade.school.data.local.entity.OfficialDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DocUiState(
    val docs: List<OfficialDocument> = emptyList(),
    val selectedTab: DocTab = DocTab.PENDING,
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val editingDoc: OfficialDocument? = null,   // null = new, non-null = editing
    val pendingUrgentCount: Int = 0
)

enum class DocTab(val label: String) {
    PENDING("待處理"),
    ALL("全部"),
    ARCHIVED("已歸檔")
}

class OfficialDocumentViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getDatabase(app).counselorDao()

    private val _tab         = MutableStateFlow(DocTab.PENDING)
    private val _searchQuery = MutableStateFlow("")
    private val _showAdd     = MutableStateFlow(false)
    private val _editingDoc  = MutableStateFlow<OfficialDocument?>(null)

    // Source flows from DB
    private val _active   = dao.getActiveDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _all      = dao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _archived = dao.getDocumentsByStatus(DocStatus.ARCHIVED.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _urgentCount = dao.getUrgentPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val uiState: StateFlow<DocUiState> = combine(
        _tab, _searchQuery, _showAdd, _editingDoc, _active, _all, _archived, _urgentCount
    ) { args ->
        val tab         = args[0] as DocTab
        val query       = args[1] as String
        val showAdd     = args[2] as Boolean
        val editing     = args[3] as? OfficialDocument
        @Suppress("UNCHECKED_CAST")
        val active      = args[4] as List<OfficialDocument>
        @Suppress("UNCHECKED_CAST")
        val all         = args[5] as List<OfficialDocument>
        @Suppress("UNCHECKED_CAST")
        val archived    = args[6] as List<OfficialDocument>
        val urgentCount = args[7] as Int

        val base = when (tab) {
            DocTab.PENDING  -> active
            DocTab.ALL      -> all
            DocTab.ARCHIVED -> archived
        }

        val filtered = if (query.isBlank()) base
        else base.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.docId.contains(query, ignoreCase = true) ||
                (it.note?.contains(query, ignoreCase = true) == true)
        }

        DocUiState(
            docs              = filtered,
            selectedTab       = tab,
            searchQuery       = query,
            showAddDialog     = showAdd,
            editingDoc        = editing,
            pendingUrgentCount= urgentCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocUiState())

    fun selectTab(tab: DocTab) { _tab.value = tab }
    fun setSearch(q: String)   { _searchQuery.value = q }
    fun openAddDialog()        { _editingDoc.value = null; _showAdd.value = true }
    fun openEditDialog(doc: OfficialDocument) { _editingDoc.value = doc; _showAdd.value = true }
    fun dismissDialog()        { _showAdd.value = false; _editingDoc.value = null }

    /** 新增或更新公文 */
    fun saveDocument(
        docId: String,
        title: String,
        category: DocCategory,
        status: DocStatus,
        isUrgent: Boolean,
        deadline: Long?,
        note: String?
    ) {
        viewModelScope.launch {
            val existing = _editingDoc.value
            val doc = OfficialDocument(
                docId        = docId.ifBlank { UUID.randomUUID().toString().take(8).uppercase() },
                title        = title,
                category     = category,
                status       = status,
                isUrgent     = isUrgent,
                receivedAt   = existing?.receivedAt ?: System.currentTimeMillis(),
                deadline     = deadline,
                signedAt     = if (status == DocStatus.SIGNED || status == DocStatus.SENT)
                                   existing?.signedAt ?: System.currentTimeMillis()
                               else existing?.signedAt,
                createdAt    = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt    = System.currentTimeMillis(),
                attachmentPath = existing?.attachmentPath,
                note         = note?.ifBlank { null }
            )
            dao.upsertDocument(doc)
            dismissDialog()
        }
    }

    /** 快速推進公文狀態（一鍵往下一步） */
    fun advanceStatus(doc: OfficialDocument) {
        val next = when (doc.status) {
            DocStatus.DRAFT        -> DocStatus.PENDING_SIGN
            DocStatus.PENDING_SIGN -> DocStatus.SIGNED
            DocStatus.SIGNED       -> DocStatus.SENT
            DocStatus.SENT         -> DocStatus.ARCHIVED
            DocStatus.REJECTED     -> DocStatus.DRAFT
            DocStatus.ARCHIVED     -> return   // 已歸檔，不做動作
        }
        viewModelScope.launch {
            dao.upsertDocument(
                doc.copy(
                    status    = next,
                    signedAt  = if (next == DocStatus.SIGNED) System.currentTimeMillis() else doc.signedAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteDocument(docId: String) {
        viewModelScope.launch { dao.deleteDocument(docId) }
    }

    /** 種下假資料方便 UI 開發測試 */
    fun seedSampleData() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val day = 86_400_000L
            listOf(
                OfficialDocument("教人字第001號", "113學年度教師資格審查通知", DocCategory.PERSONNEL, DocStatus.PENDING_SIGN, isUrgent = true,  receivedAt = now - day,     deadline = now + day),
                OfficialDocument("總字第015號",   "校園消防設備年度檢查配合事項", DocCategory.GENERAL_AFFAIRS, DocStatus.PENDING_SIGN, isUrgent = false, receivedAt = now - 2*day, deadline = now + 3*day),
                OfficialDocument("學務字第008號", "113上學期校外教學活動計畫書", DocCategory.STUDENT_AFFAIRS, DocStatus.SIGNED,       isUrgent = false, receivedAt = now - 5*day, deadline = now - day, signedAt = now - day),
                OfficialDocument("教學字第022號", "108課綱素養導向教案研習公文", DocCategory.ACADEMIC,        DocStatus.DRAFT,        isUrgent = false, receivedAt = now,         deadline = now + 7*day),
                OfficialDocument("輔字第003號",   "學生心理健康普查結果通報",     DocCategory.COUNSELING,       DocStatus.ARCHIVED,     isUrgent = false, receivedAt = now - 30*day, deadline = now - 20*day, signedAt = now - 25*day)
            ).forEach { dao.upsertDocument(it) }
        }
    }
}
