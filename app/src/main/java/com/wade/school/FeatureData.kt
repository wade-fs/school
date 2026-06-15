package com.wade.school.ui.screens

// ============================================================
// 資料模型層 — Feature Card
// ============================================================

data class FeatureItem(
    val title: String,
    val subtitle: String,
    val badge: String? = null,      // "急件" / "2則未讀" 等動態狀態
    val badgeType: BadgeType = BadgeType.NONE,
    val route: String               // 未來 Navigation 路由
)

enum class BadgeType { NONE, URGENT, INFO, WARNING, SUCCESS }

data class FeatureGroup(
    val groupTitle: String,
    val items: List<FeatureItem>
)

// ============================================================
// 導師 (homeroom) 完整功能樹
// ============================================================
val homeroomFeatures = listOf(
    FeatureGroup("出缺席管理", listOf(
        FeatureItem("快速點名", "QR 掃碼 / 人臉辨識一鍵點名", badge = "今日未完成", badgeType = BadgeType.URGENT, route = "attendance/scan"),
        FeatureItem("請假審核", "待審核 2 件，需今日內處理", badge = "2件", badgeType = BadgeType.URGENT, route = "attendance/leave_review"),
        FeatureItem("出缺席統計", "本月全班出缺席趨勢圖表", route = "attendance/stats"),
        FeatureItem("家長自動通知", "設定曠課即時推播規則", route = "attendance/notify_settings"),
    )),
    FeatureGroup("家校聯繫", listOf(
        FeatureItem("聯絡簿訊息", "3 則未讀家長回覆", badge = "3則", badgeType = BadgeType.INFO, route = "comms/messages"),
        FeatureItem("廣播通知", "發送班級重要公告", route = "comms/broadcast"),
        FeatureItem("家長會管理", "排程、出席簽到、會議記錄", route = "comms/parent_meeting"),
        FeatureItem("已讀回執追蹤", "查看哪些家長尚未閱讀通知", route = "comms/read_receipt"),
    )),
    FeatureGroup("班級事務", listOf(
        FeatureItem("學生名冊快查", "緊急聯絡人、健康狀況備查", route = "class/roster"),
        FeatureItem("週記 / 日記批閱", "待批閱 15 本，可拍照批示", badge = "15本", badgeType = BadgeType.WARNING, route = "class/journal_review"),
        FeatureItem("榮譽 / 警告紀錄", "新增獎懲記錄、查看統計", route = "class/discipline"),
        FeatureItem("班費收支管理", "收費項目記錄與餘額報表", route = "class/fund"),
        FeatureItem("座位表管理", "拖曳式座位安排與分組", route = "class/seating"),
    )),
    FeatureGroup("學習歷程輔助（108課綱）", listOf(
        FeatureItem("學習歷程提醒", "提醒學生上傳期限（截止 3 天內）", badge = "3天後截止", badgeType = BadgeType.WARNING, route = "portfolio/reminder"),
        FeatureItem("班級上傳進度", "查看哪些學生尚未上傳", route = "portfolio/progress"),
    ))
)

// ============================================================
// 科任教師 (subject) 完整功能樹
// ============================================================
val subjectFeatures = listOf(
    FeatureGroup("教材管理", listOf(
        FeatureItem("教案模板庫", "依 108 課綱核心素養分類", route = "teaching/template"),
        FeatureItem("我的教材庫", "上傳、版本控制、跨班複用", route = "teaching/materials"),
        FeatureItem("素養題型參考", "各科會考與學測素養題範例", route = "teaching/question_bank"),
        FeatureItem("備課筆記", "個人備課日誌與想法記錄", route = "teaching/notes"),
    )),
    FeatureGroup("作業與評量", listOf(
        FeatureItem("指派作業", "線上派發、截止時間、附件", route = "assessment/assign"),
        FeatureItem("繳交狀況", "已收 40/43，查看未繳名單", badge = "3人未繳", badgeType = BadgeType.WARNING, route = "assessment/submission"),
        FeatureItem("AI 輔助批改", "初稿評分建議、關鍵字標記", route = "assessment/ai_grading"),
        FeatureItem("多元評量紀錄", "口語、實作、同儕互評整合", route = "assessment/portfolio"),
        FeatureItem("題庫出卷", "隨機出卷、難易度配比設定", route = "assessment/quiz_builder"),
    )),
    FeatureGroup("成績分析", listOf(
        FeatureItem("班級成績分佈", "雷達圖、箱型圖、常態分布", route = "grade/class_stats"),
        FeatureItem("個別學生追蹤", "單一學生跨學期學習曲線", route = "grade/student_detail"),
        FeatureItem("弱點診斷報告", "AI 分析班級共同學習落差", route = "grade/weakness"),
        FeatureItem("成績匯出", "匯出至 Excel / PDF 送教務組", route = "grade/export"),
    )),
    FeatureGroup("課堂互動", listOf(
        FeatureItem("即時搶答 / 票選", "投影幕同步顯示學生作答", route = "interactive/poll"),
        FeatureItem("隨機點名抽籤", "公平抽點、避免固定模式", route = "interactive/random_pick"),
        FeatureItem("課堂紀錄", "快速標記學生課堂表現", route = "interactive/class_log"),
    ))
)

// ============================================================
// 行政教師 (admin) 完整功能樹
// ============================================================
val adminFeatures = listOf(
    FeatureGroup("公文與簽核", listOf(
        FeatureItem("待簽公文", "2 件急件、4 件一般件", badge = "2急件", badgeType = BadgeType.URGENT, route = "admin/docs_pending"),
        FeatureItem("公文追蹤", "查看各公文流程目前位置", route = "admin/docs_tracking"),
        FeatureItem("表單申請", "請假、差旅、採購等一站申請", route = "admin/forms"),
        FeatureItem("電子簽名設定", "設定授權代理簽核人員", route = "admin/signature"),
    )),
    FeatureGroup("會議與行事曆", listOf(
        FeatureItem("校務行事曆", "全校行事同步、個人行程整合", route = "calendar/school"),
        FeatureItem("會議召集", "發送邀請、確認出席、議程管理", route = "calendar/meeting"),
        FeatureItem("會議紀錄", "即時記錄、決議追蹤、簽到", route = "calendar/minutes"),
    )),
    FeatureGroup("場地與設備", listOf(
        FeatureItem("場地借用申請", "體育館、視聽教室、電腦教室", route = "facility/booking"),
        FeatureItem("衝突檢查", "自動偵測時段重疊並提示", route = "facility/conflict"),
        FeatureItem("設備盤點", "IT 設備、體育器材庫存現況", route = "facility/inventory"),
        FeatureItem("維修申報", "回報損壞設備並追蹤維修進度", route = "facility/repair"),
    )),
    FeatureGroup("統計報表", listOf(
        FeatureItem("出缺席彙整", "全校 / 年級 / 班級報表匯出", route = "report/attendance"),
        FeatureItem("活動參與率", "課外活動、社團出席統計", route = "report/activity"),
        FeatureItem("自訂報表匯出", "Word / Excel / PDF 多格式", route = "report/custom_export"),
    ))
)

// ============================================================
// 輔導教師 (counseling) 完整功能樹
// ============================================================
val counselingFeatures = listOf(
    FeatureGroup("個案管理（加密）", listOf(
        FeatureItem("個案清單", "依追蹤等級（高/中/低）分類", route = "counsel/case_list"),
        FeatureItem("新增晤談紀錄", "加密儲存、SOAP 格式輔助", route = "counsel/new_record"),
        FeatureItem("跨師協作備忘", "與導師、行政共享摘要（去識別化）", route = "counsel/collab_note"),
        FeatureItem("轉介流程", "外部資源轉介與追蹤回報", route = "counsel/referral"),
    )),
    FeatureGroup("心理健康篩檢", listOf(
        FeatureItem("心情溫度計（全班）", "每週班級情緒健康指數", badge = "2人需關注", badgeType = BadgeType.URGENT, route = "mental/class_check"),
        FeatureItem("標準化量表施測", "PHQ-9、GAD-7、BAI 等線上施測", route = "mental/scale"),
        FeatureItem("高風險預警名單", "AI 分析行為指標推送警示", route = "mental/alert"),
        FeatureItem("危機處理 SOP", "快速查閱校內外危機應對流程", route = "mental/crisis_sop"),
    )),
    FeatureGroup("晤談排程", listOf(
        FeatureItem("我的晤談時段", "今日下午 2 場、明日 1 場", badge = "今日 2 場", badgeType = BadgeType.INFO, route = "schedule/my_slots"),
        FeatureItem("學生自助預約", "開放學生選擇時段入口設定", route = "schedule/student_booking"),
        FeatureItem("不赴約記錄", "追蹤未依約學生並自動提醒", route = "schedule/no_show"),
        FeatureItem("家長同意書", "電子版發送、簽署、歸檔", route = "schedule/consent"),
    )),
    FeatureGroup("資源庫", listOf(
        FeatureItem("校外資源轉介清單", "各縣市諮商資源、24小時專線", route = "resource/external"),
        FeatureItem("衛教教材", "心理健康班會教材下載庫", route = "resource/materials"),
    ))
)

// ============================================================
// 科主任 (dept_head) 完整功能樹
// ============================================================
val deptHeadFeatures = listOf(
    FeatureGroup("課程規劃", listOf(
        FeatureItem("108課綱學分檢核", "必選修學分結構合規性驗證", route = "curriculum/credit_check"),
        FeatureItem("選修課程管理", "開課申請、修課人數、停開警示", route = "curriculum/elective"),
        FeatureItem("課程地圖", "科目先備知識關聯視覺化", route = "curriculum/map"),
        FeatureItem("跨科協作空間", "與其他科共備、素養課程設計", route = "curriculum/collab"),
    )),
    FeatureGroup("教師督導與專業成長", listOf(
        FeatureItem("教學觀察紀錄", "課室觀察表填寫與回饋管理", route = "supervision/observation"),
        FeatureItem("同儕共備追蹤", "備課會議出席與成果記錄", route = "supervision/peer_collab"),
        FeatureItem("新進教師輔導", "輔導教師配對、里程碑追蹤", route = "supervision/mentoring"),
        FeatureItem("教師研習時數", "各教師研習記錄與達標提醒", route = "supervision/pd_hours"),
    )),
    FeatureGroup("成效分析", listOf(
        FeatureItem("學測 / 會考歷年趨勢", "本科跨年級成績走勢", route = "analytics/exam_trend"),
        FeatureItem("跨班教學成效對比", "同科不同教師班級成效比較", route = "analytics/cross_class"),
        FeatureItem("課程滿意度調查", "發送匿名問卷並分析結果", route = "analytics/satisfaction"),
        FeatureItem("外部評鑑自評資料", "整合校務評鑑所需教學佐證", route = "analytics/accreditation"),
    ))
)

// ============================================================
// 各角色對應功能群組取得函式
// ============================================================
fun getFeaturesForRole(role: String): List<FeatureGroup> = when (role) {
    "homeroom"   -> homeroomFeatures
    "subject"    -> subjectFeatures
    "admin"      -> adminFeatures
    "counseling" -> counselingFeatures
    "dept_head"  -> deptHeadFeatures
    else         -> emptyList()
}
