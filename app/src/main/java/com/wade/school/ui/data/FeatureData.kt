package com.wade.school.ui.data

enum class BadgeType { NONE, URGENT, INFO, WARNING, SUCCESS }

data class FeatureItem(
    val title: String,
    val subtitle: String,
    val iconName: String,
    val badge: String? = null,
    val badgeType: BadgeType = BadgeType.NONE,
    val route: String
)

data class FeatureGroup(
    val groupTitle: String,
    val items: List<FeatureItem>
)

object FeatureData {
    fun getFeaturesForRole(role: String): List<FeatureGroup> {
        return when (role) {
            "homeroom"   -> homeroomFeatures
            "subject"    -> subjectFeatures
            "counseling" -> counselingFeatures
            "school_info"-> schoolInfoFeatures
            else         -> emptyList()
        }
    }

    // ── 導師 ─────────────────────────────────────────────────────────────────
    private val homeroomFeatures = listOf(
        FeatureGroup("出缺席管理", listOf(
            FeatureItem("快速點名", "QR 掃碼 / 人臉辨識", "qr_code", "HOT", BadgeType.URGENT, "attendance/quick"),
            FeatureItem("請假審核", "目前有 2 件待處理", "mail", "2", BadgeType.WARNING, "attendance/leave"),
            FeatureItem("出缺席統計", "月趨勢圖表分析", "bar_chart", null, BadgeType.NONE, "attendance/stats")
        )),
        FeatureGroup("家校聯繫", listOf(
            FeatureItem("數位聯絡簿", "家長簽閱狀況追蹤", "edit_note", "11人待簽", BadgeType.INFO, "contact/log"),
            FeatureItem("班級廣播", "發送重要通知給家長", "campaign", null, BadgeType.NONE, "contact/broadcast")
        )),
        FeatureGroup("班級事務", listOf(
            FeatureItem("學生名冊", "緊急聯絡人與健康快查", "contact_page", null, BadgeType.NONE, "class/students"),
            FeatureItem("座位表管理", "拖曳式更換座位", "grid_view", null, BadgeType.NONE, "class/seating")
        ))
    )

    // ── 科任教師 ─────────────────────────────────────────────────────────────
    private val subjectFeatures = listOf(
        FeatureGroup("教學管理", listOf(
            FeatureItem("教案模板", "108課綱核心素養", "topic", null, BadgeType.NONE, "subject/template")
        ))
    )

    // ── 輔導教師 ─────────────────────────────────────────────────────────────
    private val counselingFeatures = listOf(
        FeatureGroup("個案管理", listOf(
            FeatureItem("加密個案紀錄", "需生物辨識解鎖", "security", "1", BadgeType.WARNING, "counseling/cases")
        ))
    )

    // ── 學校資訊 ─────────────────────────────────────────────────────────────
    private val schoolInfoFeatures = listOf(
        FeatureGroup("校務資訊", listOf(
            FeatureItem("文件掃描", "拍照上傳紙本公文", "camera_alt", null, BadgeType.NONE, "school_info/scan")
        ))
    )
}
