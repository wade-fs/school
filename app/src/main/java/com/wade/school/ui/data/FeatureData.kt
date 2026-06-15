package com.wade.school.ui.data

enum class BadgeType { NONE, URGENT, INFO, WARNING, SUCCESS }

data class FeatureItem(
    val title: String,
    val subtitle: String,
    val iconName: String, // Simplified for prototype
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
            "homeroom" -> homeroomFeatures
            "subject" -> subjectFeatures
            "admin" -> adminFeatures
            "counseling" -> counselingFeatures
            "dept_head" -> deptHeadFeatures
            "student" -> studentFeatures
            "parent" -> parentFeatures
            else -> emptyList()
        }
    }

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

    private val studentFeatures = listOf(
        FeatureGroup("學習生活", listOf(
            FeatureItem("今日課表", "下節：物理 (301教室)", "schedule", "14:10", BadgeType.INFO, "student/schedule"),
            FeatureItem("作業繳交", "物理作業：多項式 (待繳)", "upload_file", "1", BadgeType.URGENT, "student/homework")
        )),
        FeatureGroup("個人紀錄", listOf(
            FeatureItem("我的成績", "查看期中、小考紀錄", "assessment", null, BadgeType.NONE, "student/grades"),
            FeatureItem("出缺席狀況", "個人遲到/請假紀錄", "event_available", null, BadgeType.NONE, "student/attendance")
        )),
        FeatureGroup("互動", listOf(
            FeatureItem("提問箱", "向科任老師匿名或具名提問", "quiz", null, BadgeType.NONE, "student/ask"),
            FeatureItem("心情日記", "每日心情溫度計回報", "mood", null, BadgeType.NONE, "student/mood")
        ))
    )

    private val parentFeatures = listOf(
        FeatureGroup("孩子近況 (陳小明)", listOf(
            FeatureItem("電子聯絡簿", "簽閱今日導師留言", "signature", "待簽", BadgeType.URGENT, "parent/contact_book"),
            FeatureItem("出缺席即時通", "今日 08:05 已入校", "notifications_active", "正常", BadgeType.SUCCESS, "parent/attendance")
        )),
        FeatureGroup("溝通與表單", listOf(
            FeatureItem("親師私訊", "與導師或科任老師聯繫", "chat", "2", BadgeType.INFO, "parent/chat"),
            FeatureItem("線上請假", "幫孩子申請病/事假", "assignment_ind", null, BadgeType.NONE, "parent/leave")
        ))
    )

    // Simplified other roles for brevity in prototype...
    private val subjectFeatures = listOf(FeatureGroup("教學管理", listOf(FeatureItem("教案模板", "108課綱核心素養", "topic", null, BadgeType.NONE, "subject/template"))))
    private val adminFeatures = listOf(FeatureGroup("公文簽核", listOf(FeatureItem("待簽公文", "有 3 件急件", "draw", "3", BadgeType.URGENT, "admin/docs"))))
    private val counselingFeatures = listOf(FeatureGroup("個案管理", listOf(FeatureItem("加密個案紀錄", "需生物辨識解鎖", "security", "1", BadgeType.WARNING, "counseling/cases"))))
    private val deptHeadFeatures = listOf(FeatureGroup("科務發展", listOf(FeatureItem("課程地圖", "視覺化課程結構檢核", "map", null, BadgeType.NONE, "dept/map"))))
}
