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
        FeatureGroup("今日課務", listOf(
            FeatureItem("我的課表",   "今日課程與下節提醒",       "calendar_today",  "下節 14:10", BadgeType.INFO,    "subject/timetable"),
            FeatureItem("課堂互動",   "隨機抽人、加分、計時器",   "casino",          null,         BadgeType.NONE,    "subject/interaction"),
            FeatureItem("科任點名",   "本節出缺席快速記錄",        "how_to_reg",      null,         BadgeType.NONE,    "subject/attendance")
        )),
        FeatureGroup("成績管理", listOf(
            FeatureItem("輸入考試成績", "段考/小考成績批次輸入",   "edit",            "待輸入 2 班", BadgeType.WARNING, "subject/grades/input"),
            FeatureItem("成績加權計算", "學期總成績自動計算",      "calculate",       null,          BadgeType.NONE,    "subject/grades/calc"),
            FeatureItem("成績分析",     "分布圖、PR 值、低標追蹤", "analytics",       null,          BadgeType.NONE,    "subject/grades/analysis"),
            FeatureItem("補考管理",     "缺考學生補考排程",         "assignment_late", "3",           BadgeType.URGENT,  "subject/makeup")
        )),
        FeatureGroup("作業與評量", listOf(
            FeatureItem("作業管理",   "發布作業與批改追蹤",        "assignment",      "未批改 47 份", BadgeType.WARNING, "subject/assignments"),
            FeatureItem("評量出題",   "建立題庫與小考",            "quiz",            null,           BadgeType.NONE,    "subject/quizzes"),
            FeatureItem("未繳追蹤",   "逾期未繳學生一覽",          "assignment_late", "12",           BadgeType.URGENT,  "subject/assignments/overdue")
        )),
        FeatureGroup("教學準備", listOf(
            FeatureItem("108 課綱教案", "素養導向教案庫",          "topic",           null,           BadgeType.NONE,    "subject/lesson_plans"),
            FeatureItem("教學省思",    "課後省思四格日誌",          "rate_review",     null,           BadgeType.NONE,    "subject/reflection"),
            FeatureItem("教材資源",    "上傳與管理教學素材",        "attach_file",     null,           BadgeType.NONE,    "subject/materials")
        )),
        FeatureGroup("學生學習", listOf(
            FeatureItem("學生表現",   "課堂積分與學習態度紀錄",    "star",            null,           BadgeType.NONE,    "subject/performance"),
            FeatureItem("成績通知",   "傳送成績給學生/家長",        "send",            null,           BadgeType.NONE,    "subject/notify"),
            FeatureItem("低標名單",   "本學期成績低標學生清單",     "warning",         "5",            BadgeType.WARNING, "subject/at_risk")
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
            FeatureItem("文件掃描", "拍照上傳紙本公文", "camera_alt", null, BadgeType.NONE, "school_info/scan"),
            FeatureItem("使用手冊", "查看 App 操作說明", "help", null, BadgeType.NONE, "manual")
        ))
    )
}
