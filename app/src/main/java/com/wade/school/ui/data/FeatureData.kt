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
        FeatureGroup("班級總覽", listOf(
            FeatureItem("班級儀表板",   "今日出缺席、待辦摘要",       "dashboard",      null,        BadgeType.NONE,    "homeroom/dashboard"),
            FeatureItem("學生名冊",     "健康資訊、緊急聯絡人、快查",  "contact_page",   null,        BadgeType.NONE,    "homeroom/students"),
            FeatureItem("座位表",       "拖曳更換座位",                "grid_view",      null,        BadgeType.NONE,    "homeroom/seating")
        )),
        FeatureGroup("出缺席管理", listOf(
            FeatureItem("快速點名",     "早自習全班一鍵點名",          "how_to_reg",     null,        BadgeType.URGENT,  "homeroom/attendance"),
            FeatureItem("假單審核",     "家長請假申請待審核",           "pending_actions","2",         BadgeType.WARNING, "homeroom/leave"),
            FeatureItem("異常預警",     "曠課/連假異常通報",            "warning",        "1",         BadgeType.URGENT,  "homeroom/attendance/alerts"),
            FeatureItem("出缺席統計",   "學期出席率趨勢與報表",         "bar_chart",      null,        BadgeType.NONE,    "homeroom/attendance/stats")
        )),
        FeatureGroup("獎懲與操行", listOf(
            FeatureItem("獎懲記錄",     "新增功過、查閱全班獎懲",       "gavel",          null,        BadgeType.NONE,    "homeroom/discipline"),
            FeatureItem("操行成績",     "學期操行計算與評語填寫",        "rate_review",    "未完成 5 人", BadgeType.INFO,  "homeroom/semester"),
            FeatureItem("學期評語",     "AI 輔助填寫評語，進度追蹤",    "edit_note",      "未填 12 人", BadgeType.WARNING,"homeroom/semester/comments")
        )),
        FeatureGroup("親師溝通", listOf(
            FeatureItem("數位聯絡簿",   "填寫作業與注意事項",           "book",           "8 人未簽",  BadgeType.INFO,    "homeroom/contact_book"),
            FeatureItem("家長聯絡",     "電話/Line/親晤記錄",           "contact_phone",  null,        BadgeType.NONE,    "homeroom/communication"),
            FeatureItem("親師座談",     "座談紀錄與後續追蹤",           "groups",         "1 件待追蹤", BadgeType.INFO,  "homeroom/conference"),
            FeatureItem("班級廣播",     "推播重要通知給全班家長",        "campaign",       null,        BadgeType.NONE,    "homeroom/broadcast")
        )),
        FeatureGroup("班級事務", listOf(
            FeatureItem("幹部名單",     "班級幹部職務管理",             "badge",          null,        BadgeType.NONE,    "homeroom/cadre"),
            FeatureItem("班費管理",     "收支記錄與帳目公告",           "account_balance","NT$ 3,240", BadgeType.INFO,   "homeroom/fund"),
            FeatureItem("班級活動",     "校外教學、班遊、競賽",          "event",          null,        BadgeType.NONE,    "homeroom/activities"),
            FeatureItem("優良事蹟",     "個人/班級榮譽記錄",             "emoji_events",   null,        BadgeType.NONE,    "homeroom/honors")
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
