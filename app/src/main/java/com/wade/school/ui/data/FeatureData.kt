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
            "admin"      -> adminFeatures
            "counseling" -> counselingFeatures
            "dept_head"  -> deptHeadFeatures
            "student"    -> studentFeatures
            "parent"     -> parentFeatures
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

    // ── 行政教師 ─────────────────────────────────────────────────────────────
    private val adminFeatures = listOf(
        FeatureGroup("公文簽核", listOf(
            FeatureItem("待簽公文",   "急件優先，依期限排序",     "draw",           "3",  BadgeType.URGENT,   "admin/docs/pending"),
            FeatureItem("公文查詢",   "依字號、日期、類別搜尋",   "search",         null, BadgeType.NONE,     "admin/docs/search"),
            FeatureItem("文件掃描",   "拍照上傳紙本公文",         "camera_alt",     null, BadgeType.NONE,     "admin/docs/scan"),
            FeatureItem("已歸檔公文", "已結案公文查閱",           "archive",        null, BadgeType.NONE,     "admin/docs/archive")
        )),
        FeatureGroup("行事曆與會議", listOf(
            FeatureItem("校務行事曆", "全校行程一覽",             "calendar_month", "今日 2 件", BadgeType.INFO,    "admin/calendar"),
            FeatureItem("會議出席簽到","待出席會議",              "how_to_reg",     "1",  BadgeType.WARNING,  "admin/meetings/checkin"),
            FeatureItem("會議紀錄",   "撰寫與查閱歷次紀錄",       "edit_note",      null, BadgeType.NONE,     "admin/meetings/minutes"),
            FeatureItem("代課申請",   "申請與審核代補課",         "swap_horiz",     null, BadgeType.NONE,     "admin/substitute")
        )),
        FeatureGroup("公告管理", listOf(
            FeatureItem("發布公告",   "起草並推播學校通知",       "campaign",       null, BadgeType.NONE,     "admin/announcements/new"),
            FeatureItem("公告草稿",   "尚未發布的草稿",           "drafts",         "2",  BadgeType.INFO,     "admin/announcements/drafts"),
            FeatureItem("歷史公告",   "查閱已發布公告",           "history",        null, BadgeType.NONE,     "admin/announcements/history"),
            FeatureItem("分類標籤",   "管理公告分類",             "label",          null, BadgeType.NONE,     "admin/announcements/tags")
        )),
        FeatureGroup("人事與排課", listOf(
            FeatureItem("教師名冊",   "科別、聯絡資料查詢",       "groups",         null, BadgeType.NONE,     "admin/personnel/list"),
            FeatureItem("課務時數",   "本學期排課統計",           "bar_chart",      null, BadgeType.NONE,     "admin/personnel/hours"),
            FeatureItem("代課審核",   "待審核申請",               "swap_horiz",     "1",  BadgeType.WARNING,  "admin/personnel/substitute"),
            FeatureItem("請假紀錄",   "教師出勤狀況查詢",         "event_busy",     null, BadgeType.NONE,     "admin/personnel/leave")
        )),
        FeatureGroup("學生事務", listOf(
            FeatureItem("獎懲紀錄",   "記錄功過與獎懲",           "gavel",          null, BadgeType.NONE,     "admin/student/discipline"),
            FeatureItem("活動報名",   "社團、競賽、校外教學",     "event",          null, BadgeType.NONE,     "admin/student/activities"),
            FeatureItem("表單審核",   "各類借用與申請",           "assignment",     "3",  BadgeType.INFO,     "admin/student/forms"),
            FeatureItem("統計報表",   "匯出 CSV 統計資料",        "summarize",      null, BadgeType.NONE,     "admin/student/reports")
        )),
        FeatureGroup("設備與場地", listOf(
            FeatureItem("場地借用",   "查詢與申請場地",           "meeting_room",   null, BadgeType.NONE,     "admin/facility/venue"),
            FeatureItem("設備借出",   "QR 掃碼借還設備",          "qr_code",        null, BadgeType.NONE,     "admin/facility/equipment"),
            FeatureItem("維修報修",   "填寫並追蹤報修單",         "build",          "1",  BadgeType.WARNING,  "admin/facility/repair"),
            FeatureItem("財產盤點",   "年度設備清點作業",         "inventory",      null, BadgeType.NONE,     "admin/facility/inventory")
        ))
    )

    // ── 輔導教師 ─────────────────────────────────────────────────────────────
    private val counselingFeatures = listOf(
        FeatureGroup("個案管理", listOf(
            FeatureItem("加密個案紀錄", "需生物辨識解鎖", "security", "1", BadgeType.WARNING, "counseling/cases")
        ))
    )

    // ── 科主任 ───────────────────────────────────────────────────────────────
    private val deptHeadFeatures = listOf(
        FeatureGroup("科務發展", listOf(
            FeatureItem("課程地圖", "視覺化課程結構檢核", "map", null, BadgeType.NONE, "dept/map")
        ))
    )

    // ── 學生 ─────────────────────────────────────────────────────────────────
    private val studentFeatures = listOf(
        FeatureGroup("學習生活", listOf(
            FeatureItem("今日課表",   "下節：物理 (301教室)", "schedule",     "14:10", BadgeType.INFO,  "student/schedule"),
            FeatureItem("作業繳交",   "物理作業：多項式 (待繳)", "upload_file","1",   BadgeType.URGENT,"student/homework")
        )),
        FeatureGroup("個人紀錄", listOf(
            FeatureItem("我的成績",   "查看期中、小考紀錄", "assessment",   null, BadgeType.NONE, "student/grades"),
            FeatureItem("出缺席狀況", "個人遲到/請假紀錄",  "event_available", null, BadgeType.NONE, "student/attendance")
        )),
        FeatureGroup("互動", listOf(
            FeatureItem("提問箱",   "向科任老師匿名或具名提問", "quiz",  null, BadgeType.NONE, "student/ask"),
            FeatureItem("心情日記", "每日心情溫度計回報",       "mood",  null, BadgeType.NONE, "student/mood")
        ))
    )

    // ── 家長 ─────────────────────────────────────────────────────────────────
    private val parentFeatures = listOf(
        FeatureGroup("孩子近況 (陳小明)", listOf(
            FeatureItem("電子聯絡簿",   "簽閱今日導師留言",   "signature",            "待簽", BadgeType.URGENT,  "parent/contact_book"),
            FeatureItem("出缺席即時通", "今日 08:05 已入校",  "notifications_active", "正常", BadgeType.SUCCESS, "parent/attendance")
        )),
        FeatureGroup("溝通與表單", listOf(
            FeatureItem("親師私訊", "與導師或科任老師聯繫", "chat",           "2",  BadgeType.INFO, "parent/chat"),
            FeatureItem("線上請假", "幫孩子申請病/事假",   "assignment_ind", null, BadgeType.NONE, "parent/leave")
        ))
    )
}
