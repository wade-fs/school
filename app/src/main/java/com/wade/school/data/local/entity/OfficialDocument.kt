package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DocStatus {
    DRAFT,          // 起草中
    PENDING_SIGN,   // 待簽核
    SIGNED,         // 已簽核
    SENT,           // 已發文
    ARCHIVED,       // 已歸檔
    REJECTED        // 退件
}

enum class DocCategory(val label: String) {
    PERSONNEL("人事"),
    ACADEMIC("教學"),
    GENERAL_AFFAIRS("總務"),
    STUDENT_AFFAIRS("學務"),
    COUNSELING("輔導"),
    OTHER("其他")
}

@Entity(tableName = "official_documents")
data class OfficialDocument(
    @PrimaryKey val docId: String,           // 公文字號（或 UUID）
    val title: String,                        // 主旨
    val category: DocCategory,               // 業務類別
    val status: DocStatus,                   // 目前狀態
    val isUrgent: Boolean = false,           // 急件標示
    val receivedAt: Long? = null,            // 收文時間（毫秒）
    val deadline: Long? = null,              // 辦理期限（毫秒）
    val signedAt: Long? = null,              // 簽核時間（毫秒）
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val attachmentPath: String? = null,      // 掃描檔本地路徑
    val note: String? = null                 // 備註
)
