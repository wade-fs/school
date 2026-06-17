# 導師平台 — 功能深化規劃文件

> 專案：中學教師助手 App（`com.wade.school`）
> 更新日期：2026-06-17
> 狀態：規劃中
> 基礎版本：school3.zip（DB version 15）

---

## 一、現況盤點

### 已有的基礎

| 功能 | Entity | Screen | 完整度 |
|------|--------|--------|--------|
| 出缺席點名 | `AttendanceRecord` | `AttendanceScreen` | ★★★☆☆ |
| 家長聯絡記錄 | `ParentContactLog` | `HomeroomManagement/ContactLogTab` | ★★★☆☆ |
| 行為觀察 | `BehaviorObservations` | `HomeroomManagement/ObservationTab` | ★★☆☆☆ |
| 幹部名單 | `ClassCadre` | `HomeroomManagement/CadreTab` | ★★★☆☆ |
| 班級活動 | `ClassActivity` | `HomeroomManagement/ActivityTab` | ★★★☆☆ |
| 榮譽事蹟 | `ClassHonor` | `HomeroomManagement/HonorTab` | ★★★☆☆ |
| 數位聯絡簿 | `ContactBookEntry` | `ClassBulletinScreen` | ★★☆☆☆ |
| 學生名冊 | `Student` | 部分 | ★★☆☆☆ |

### `homeroomFeatures`（現況）

```kotlin
private val homeroomFeatures = listOf(
    FeatureGroup("出缺席管理", listOf(
        FeatureItem("快速點名",    "QR 掃碼 / 人臉辨識", "qr_code",    "HOT",    BadgeType.URGENT,  "attendance/quick"),
        FeatureItem("請假審核",    "目前有 2 件待處理",  "mail",       "2",      BadgeType.WARNING, "attendance/leave"),
        FeatureItem("出缺席統計",  "月趨勢圖表分析",     "bar_chart",  null,     BadgeType.NONE,    "attendance/stats")
    )),
    FeatureGroup("家校聯繫", listOf(
        FeatureItem("數位聯絡簿",  "家長簽閱狀況追蹤",   "edit_note",  "11人待簽", BadgeType.INFO,  "contact/log"),
        FeatureItem("班級廣播",    "發送重要通知給家長",  "campaign",   null,     BadgeType.NONE,    "contact/broadcast")
    )),
    FeatureGroup("班級事務", listOf(
        FeatureItem("學生名冊",    "緊急聯絡人與健康快查", "contact_page", null,  BadgeType.NONE,    "class/students"),
        FeatureItem("座位表管理",  "拖曳式更換座位",       "grid_view",    null,  BadgeType.NONE,    "class/seating")
    ))
)
```

---

## 二、台灣高中導師真實需求分析

### 典型工作情境

- 每天早自習前完成**全班點名**並上傳校務系統
- 處理學生**請假單**（需審核、通知家長確認）
- 填寫**獎懲紀錄**（功過、警告、記過），提報至學務處
- 追蹤學生的**出缺席異常**（連續曠課 3 節以上需通報）
- 每學期兩次**親師座談**，記錄與整理
- 管理班費、班級基金
- 學期末填寫**學生評語**、操行成績
- 協助有特殊需求學生進行**輔導轉介**
- 追蹤各科成績低標生，與科任教師溝通

---

## 三、新增 Entity 規劃（`HomeroomTeacherEntities.kt`）

### 3-1 學生獎懲記錄 `DisciplineRecord`

台灣高中的獎懲制度分為：功（嘉獎/小功/大功）、過（警告/小過/大過）、獎狀、申誡等。

```kotlin
enum class DisciplineType(val label: String, val score: Int) {
    COMMENDATION("嘉獎",   1),
    MINOR_MERIT("小功",    3),
    MAJOR_MERIT("大功",    9),
    WARNING("警告",       -1),
    MINOR_DEMERIT("小過", -3),
    MAJOR_DEMERIT("大過", -9),
    SPECIAL_AWARD("獎狀",  0),  // 競賽/校外獎項
    ADMONITION("申誡",    -1)   // 口頭告誡
}

@Entity(tableName = "discipline_records")
data class DisciplineRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val type: DisciplineType,
    val reason: String,                  // 事由
    val sourceType: String = "HOMEROOM", // HOMEROOM / SUBJECT / ADMIN / SELF
    val recordDate: Long = System.currentTimeMillis(),
    val academicYear: Int,
    val semester: Int,
    val isReported: Boolean = false,     // 是否已送學務處
    val reportedAt: Long? = null,
    val note: String? = null
)
```

**計算邏輯：**
- 學期操行成績 = 基準分（85） + Σ(各獎懲分數)
- 大功抵大過，功過相抵後剩餘才計分

---

### 3-2 請假申請管理 `LeaveRequest`

```kotlin
enum class LeaveType(val label: String) {
    SICK("病假"),
    PERSONAL("事假"),
    OFFICIAL("公假"),
    FUNERAL("喪假"),
    LATE("遲到"),
    EARLY_LEAVE("早退"),
    ABSENCE("曠課")       // 未請假直接不到
}

enum class LeaveStatus(val label: String) {
    PENDING("待審核"),
    APPROVED("已核准"),
    REJECTED("未核准"),
    CANCELLED("已取消")
}

@Entity(tableName = "leave_requests")
data class LeaveRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val leaveType: LeaveType,
    val startDate: Long,
    val endDate: Long,
    val periodNames: String,             // 請假節次，逗號分隔："第一節,第二節"
    val totalPeriods: Int,
    val reason: String,
    val applicantType: String = "PARENT",// PARENT / STUDENT / TEACHER
    val attachmentPath: String? = null,  // 診斷書等附件
    val status: LeaveStatus = LeaveStatus.PENDING,
    val reviewedAt: Long? = null,
    val reviewNote: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

---

### 3-3 出缺席異常通報 `AttendanceAlert`

```kotlin
enum class AlertRuleType(val label: String) {
    ABSENT_3_PERIODS("單日曠課 3 節以上"),
    ABSENT_3_DAYS("連續曠課 3 日"),
    LATE_5_TIMES("本月遲到 5 次"),
    ABSENT_RATE("出缺席率低於 80%"),
    CUSTOM("自訂規則")
}

@Entity(tableName = "attendance_alerts")
data class AttendanceAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val ruleType: AlertRuleType,
    val description: String,             // 觸發原因說明
    val triggeredAt: Long = System.currentTimeMillis(),
    val isHandled: Boolean = false,
    val handledNote: String? = null,
    val notifiedParent: Boolean = false, // 是否已通知家長
    val notifiedAt: Long? = null
)
```

---

### 3-4 學生健康資訊 `StudentHealthInfo`

```kotlin
@Entity(tableName = "student_health_info",
    indices = [Index(value = ["studentId"], unique = true)])
data class StudentHealthInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val bloodType: String? = null,
    val allergies: String? = null,       // 過敏原（逗號分隔）
    val chronicDisease: String? = null,  // 慢性病記錄
    val medication: String? = null,      // 長期用藥
    val emergencyContact1: String? = null,   // 緊急聯絡人1
    val emergencyPhone1: String? = null,
    val emergencyContact2: String? = null,
    val emergencyPhone2: String? = null,
    val specialNeeds: String? = null,    // 特殊需求說明
    val iepStudent: Boolean = false,     // 是否有個別化教育計畫
    val counselingReferral: Boolean = false, // 已轉介輔導
    val updatedAt: Long = System.currentTimeMillis()
)
```

---

### 3-5 操行成績與學期評語 `SemesterRecord`

```kotlin
@Entity(tableName = "semester_records",
    indices = [Index(value = ["studentId", "academicYear", "semester"], unique = true)])
data class SemesterRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val academicYear: Int,
    val semester: Int,
    // 操行
    val conductScore: Float? = null,     // 操行成績（自動計算或手動輸入）
    val conductBase: Float = 85f,        // 操行基準分
    val conductNote: String? = null,     // 操行說明
    // 學期評語
    val teacherComment: String? = null,  // 導師評語（呈現於成績單）
    val strengthNote: String? = null,    // 優點特質
    val improvementNote: String? = null, // 待加強處
    // 其他
    val absenceDays: Int = 0,
    val lateTimes: Int = 0,
    val isFinalized: Boolean = false,    // 是否已送出
    val finalizedAt: Long? = null
)
```

---

### 3-6 班費管理 `ClassFund`

```kotlin
enum class FundTransactionType(val label: String) {
    INCOME("收入"),
    EXPENSE("支出")
}

@Entity(tableName = "class_fund_transactions")
data class ClassFundTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val type: FundTransactionType,
    val amount: Int,                     // 金額（元）
    val category: String,               // 班費收繳/文具/活動/其他
    val description: String,
    val receiptPath: String? = null,     // 收據照片
    val transactionDate: Long = System.currentTimeMillis(),
    val recordedBy: String = "導師"
)
```

---

### 3-7 親師座談記錄 `ParentTeacherConference`

```kotlin
@Entity(tableName = "parent_teacher_conferences")
data class ParentTeacherConference(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val conferenceDate: Long,
    val attendees: String,               // 出席者姓名（逗號分隔）
    val academicDiscussion: String = "", // 學業討論
    val behaviorDiscussion: String = "", // 行為品德
    val parentConcerns: String = "",     // 家長反映事項
    val followUpActions: String = "",    // 後續追蹤事項
    val followUpDate: Long? = null,
    val isFollowUpDone: Boolean = false
)
```

---

## 四、六大功能模組詳細規劃

---

### 4-1 班級儀表板（首頁重設計）

**設計目標：** 早自習前 30 秒內完成狀況掌握。

```
班級儀表板
├── 今日摘要卡（最頂部）
│   ├── 今日出缺席：出席 32 / 缺席 3
│   ├── 待審假單：2 件
│   ├── 未簽聯絡簿：8 人
│   └── 本週活動提醒
├── 快速動作列
│   ├── [點名] [假單] [聯絡簿] [廣播]
└── 本週班級行事曆
    └── 考試/活動/重要日期一覽
```

**FeatureItem 路由：** `homeroom/dashboard`

---

### 4-2 出缺席管理（深化）

**現有問題：** 只能手動輸入，沒有異常預警，沒有跨日統計。

**深化內容：**

#### A. 請假流程管理

```
LeaveRequestScreen
├── 待審核（PENDING）badge 數
├── 請假單列表
│   ├── 學生姓名 + 假別 + 日期 + 節數
│   ├── 核准 / 不核准 按鈕
│   └── 備注輸入（附診斷書提醒）
└── 新增請假（導師代為登錄）
```

#### B. 出缺席異常預警

觸發條件（可自訂）：

| 規則 | 預設門檻 | 通報動作 |
|------|---------|---------|
| 單日曠課 | ≥ 3 節 | 推送警示 + 提醒聯繫家長 |
| 連續曠課 | ≥ 3 天 | 推送 URGENT + 建議轉介輔導 |
| 月遲到次數 | ≥ 5 次 | 推送 WARNING |
| 學期出席率 | ≤ 80% | 推送 WARNING + 影響操行 |

#### C. 出缺席統計儀表板

```
AttendanceStatsDashboard
├── 班級出席率趨勢（月折線圖）
├── 各假別人次統計（長條圖）
├── 個人出缺席排行（高缺席學生清單）
└── 匯出：學期出缺席彙整表（CSV）
```

**FeatureItem 路由：**
- `homeroom/attendance` — 快速點名
- `homeroom/leave` — 假單管理
- `homeroom/attendance/alerts` — 異常通報
- `homeroom/attendance/stats` — 統計

---

### 4-3 獎懲管理

**台灣高中操行計分制度：**

| 獎懲類型 | 分數 | 備注 |
|---------|------|------|
| 大功 | +9 | 功過相抵：1 大功抵 1 大過 |
| 小功 | +3 | 1 大功 = 3 小功 = 9 嘉獎 |
| 嘉獎 | +1 | |
| 警告 | -1 | 1 大過 = 3 小過 = 9 警告 |
| 小過 | -3 | |
| 大過 | -9 | |
| 操行基準分 | 85 | 各校略有不同 |

**功能細項：**

```
DisciplineScreen
├── Tab 1：新增獎懲
│   ├── 選學生（搜尋或從名冊選）
│   ├── 選獎懲類型（六種 + 獎狀/申誡）
│   ├── 填寫事由
│   └── 標記「是否已送學務處」
├── Tab 2：獎懲紀錄
│   ├── 全班列表（依學生分組）
│   ├── 點擊展開個人獎懲歷史
│   └── 單學期功過總計
└── Tab 3：操行成績
    ├── 全班操行分數一覽表
    ├── 自動計算（基準分 + 功過加減）
    ├── 手動微調並加注說明
    └── 匯出操行成績表
```

**FeatureItem 路由：** `homeroom/discipline`

---

### 4-4 學生個人檔案（深化）

**現況：** `Student` entity 只有基本學籍，缺乏健康、輔導整合。

**深化內容：**

```
StudentProfileScreen（個人頁面整合）
├── 基本資料（學籍）
│   ├── 照片、姓名、學號、座號
│   └── 緊急聯絡人（可快速撥號）
├── 健康資訊（StudentHealthInfo）
│   ├── 血型、過敏原、慢性病、用藥
│   ├── IEP 標記
│   └── 已轉介輔導標記
├── 本學期摘要
│   ├── 出缺席統計
│   ├── 功過統計 + 操行分數預測
│   ├── 各科成績摘要（低標科目紅字）
│   └── 近期行為觀察（最新 3 則）
├── 親師溝通紀錄
│   └── `ParentContactLog` 最近 5 則
└── 操作按鈕
    ├── 撥打家長電話
    ├── 新增觀察記錄
    ├── 新增獎懲
    └── 轉介輔導
```

**FeatureItem 路由：** `homeroom/students`

---

### 4-5 學期評語與操行成績

**台灣特有需求：** 學期末需要為全班每位學生填寫評語，
輸出至校務系統。約 35 人 × 200 字 = 高耗時工作。

**功能細項：**

#### A. 評語輔助工具

- **快速套用模板**：預設 30 種評語句型（可自訂）
  ```
  正向類：「學習態度積極，課堂參與熱忱，為同學樹立良好榜樣。」
  需加強：「有待提升自律能力，建議多利用課餘時間複習。」
  資優生：「思維敏捷，具備獨立思考能力，可進一步培養研究素養。」
  ```
- **AI 輔助生成**（選填）：根據學生的行為觀察記錄和獎懲紀錄，
  建議評語草稿，教師確認後送出。
- **進度追蹤**：已填 N / 全班 M 人，顯示完成百分比。

#### B. 操行成績確認

```
SemesterRecordScreen
├── 學生列表（可依操行分數排序）
├── 點擊學生 → 操行細項
│   ├── 自動計算值（基準分 + 功過）
│   ├── 手動微調欄位
│   └── 評語填寫區
├── 全班操行統計（平均/最高/最低）
└── 批次確認送出（→ 標記 isFinalized）
```

**FeatureItem 路由：** `homeroom/semester`

---

### 4-6 班費管理

**台灣導師特有業務：** 每學期收班費、管理班費支出、向家長公開帳目。

```
ClassFundScreen
├── 帳目總覽
│   ├── 目前結餘：NT$ X,XXX
│   ├── 本學期收入 / 支出 統計
│   └── 月收支折線圖
├── 收支記錄列表
│   ├── 依時間排序，顯示類別/金額/說明
│   └── 長按 → 刪除
├── 新增收支
│   ├── 類型（收入/支出）
│   ├── 類別（班費/文具/活動/其他）
│   ├── 金額 + 說明
│   └── 相機拍攝收據
└── 帳目公告
    └── 一鍵產生帳目摘要 → 貼入聯絡簿公告
```

**FeatureItem 路由：** `homeroom/fund`

---

### 4-7 親師溝通（深化）

**現況：** `ParentContactLog` 只是記錄，缺乏追蹤和座談功能。

**深化內容：**

```
ParentCommunicationScreen
├── Tab 1：聯絡記錄
│   ├── 全班聯絡紀錄時間軸
│   ├── 依學生篩選
│   ├── 新增聯絡記錄
│   │   ├── 聯絡管道：電話/Line/親晤/書面
│   │   ├── 主題/事由/內容
│   │   └── 後續追蹤事項
│   └── 未聯絡超過 60 天的學生提醒
├── Tab 2：親師座談
│   ├── 座談紀錄列表
│   ├── 建立座談紀錄（StudentTeacherConference）
│   │   ├── 出席者/日期
│   │   ├── 學業/行為/家長疑問/後續追蹤
│   │   └── 後續追蹤提醒設定
│   └── 未完成追蹤事項 badge
└── Tab 3：班級廣播
    ├── 發送全班通知（記錄備存）
    └── 歷史廣播查閱
```

**FeatureItem 路由：** `homeroom/communication`

---

### 4-8 數位聯絡簿（深化）

**現況：** `ContactBookEntry` 功能較陽春，缺乏家長簽閱追蹤和附件。

**深化內容：**

```
ContactBookScreen（深化版）
├── 今日聯絡簿
│   ├── 作業欄（多科目作業列表）
│   ├── 注意事項（導師留言）
│   ├── 重要公告（連結學校官網公告）
│   └── 附件（照片/PDF）
├── 簽閱追蹤
│   ├── 已簽 N / M 人
│   ├── 未簽名單（一鍵提醒）
│   └── 連續 3 天未簽 → 警示
├── 歷史查閱
│   └── 月曆視圖，有聯絡簿的日期標點
└── 模板管理
    └── 常用公告快速套用
```

**FeatureItem 路由：** `homeroom/contact_book`

---

## 五、完整 `homeroomFeatures` 代碼

```kotlin
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
```

---

## 六、DAO 新增方法

### 獎懲相關

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertDisciplineRecord(r: DisciplineRecord): Long

@Query("SELECT * FROM discipline_records WHERE classId = :classId ORDER BY recordDate DESC")
fun getDisciplineByClass(classId: String): Flow<List<DisciplineRecord>>

@Query("SELECT * FROM discipline_records WHERE studentId = :studentId ORDER BY recordDate DESC")
fun getDisciplineByStudent(studentId: String): Flow<List<DisciplineRecord>>

@Query("""SELECT SUM(
    CASE type
        WHEN 'MAJOR_MERIT'   THEN 9
        WHEN 'MINOR_MERIT'   THEN 3
        WHEN 'COMMENDATION'  THEN 1
        WHEN 'WARNING'       THEN -1
        WHEN 'MINOR_DEMERIT' THEN -3
        WHEN 'MAJOR_DEMERIT' THEN -9
        WHEN 'ADMONITION'    THEN -1
        ELSE 0
    END) FROM discipline_records
    WHERE studentId = :studentId AND academicYear = :year AND semester = :sem""")
fun getDisciplineScore(studentId: String, year: Int, sem: Int): Flow<Int?>
```

### 請假相關

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertLeaveRequest(r: LeaveRequest)

@Query("SELECT * FROM leave_requests WHERE classId = :classId AND status = 'PENDING' ORDER BY createdAt DESC")
fun getPendingLeaves(classId: String): Flow<List<LeaveRequest>>

@Query("SELECT COUNT(*) FROM leave_requests WHERE classId = :classId AND status = 'PENDING'")
fun getPendingLeaveCount(classId: String): Flow<Int>

@Query("UPDATE leave_requests SET status = :status, reviewedAt = :time, reviewNote = :note WHERE id = :id")
suspend fun reviewLeave(id: Int, status: String, time: Long, note: String)
```

### 健康資訊

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertStudentHealth(h: StudentHealthInfo)

@Query("SELECT * FROM student_health_info WHERE studentId = :studentId")
suspend fun getStudentHealth(studentId: String): StudentHealthInfo?

@Query("SELECT * FROM student_health_info WHERE iepStudent = 1")
fun getIepStudents(): Flow<List<StudentHealthInfo>>
```

### 操行與評語

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertSemesterRecord(r: SemesterRecord)

@Query("""SELECT * FROM semester_records
    WHERE classId = :classId AND academicYear = :year AND semester = :sem
    ORDER BY studentName""")
fun getSemesterRecordsByClass(classId: String, year: Int, sem: Int): Flow<List<SemesterRecord>>

@Query("SELECT COUNT(*) FROM semester_records WHERE classId = :classId AND isFinalized = 0")
fun getUnfinalizedCount(classId: String): Flow<Int>
```

### 班費

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertFundTransaction(t: ClassFundTransaction): Long

@Query("SELECT * FROM class_fund_transactions WHERE classId = :classId ORDER BY transactionDate DESC")
fun getFundByClass(classId: String): Flow<List<ClassFundTransaction>>

@Query("SELECT SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END) FROM class_fund_transactions WHERE classId = :classId")
fun getFundBalance(classId: String): Flow<Int?>
```

---

## 七、AppDatabase 更新

```kotlin
// version 15 → 16
entities = [
    ...existing...,
    DisciplineRecord::class,
    LeaveRequest::class,
    AttendanceAlert::class,
    StudentHealthInfo::class,
    SemesterRecord::class,
    ClassFundTransaction::class,
    ParentTeacherConference::class,
]
version = 16
```

---

## 八、Converters 新增

```kotlin
@TypeConverter fun fromDisciplineType(v: DisciplineType): String = v.name
@TypeConverter fun toDisciplineType(v: String): DisciplineType = DisciplineType.valueOf(v)

@TypeConverter fun fromLeaveType(v: LeaveType): String = v.name
@TypeConverter fun toLeaveType(v: String): LeaveType = LeaveType.valueOf(v)

@TypeConverter fun fromLeaveStatus(v: LeaveStatus): String = v.name
@TypeConverter fun toLeaveStatus(v: String): LeaveStatus = LeaveStatus.valueOf(v)

@TypeConverter fun fromAlertRuleType(v: AlertRuleType): String = v.name
@TypeConverter fun toAlertRuleType(v: String): AlertRuleType = AlertRuleType.valueOf(v)

@TypeConverter fun fromFundTransactionType(v: FundTransactionType): String = v.name
@TypeConverter fun toFundTransactionType(v: String): FundTransactionType = FundTransactionType.valueOf(v)
```

---

## 九、三大整合設計

### 9-1 與輔導教師模組整合

```
導師操作                          輔導模組
學生健康 iepStudent = true  →  自動出現在輔導追蹤名單
出缺席異常觸發               →  可一鍵「轉介輔導」→ 建立 RiskAlert
學生操行大過                 →  可同步通知輔導教師
```

### 9-2 與科任教師模組整合

```
科任教師功能                      導師模組
成績低標預警（< 60）        →  出現在導師「低標學生清單」
作業未繳通知                →  可從導師端查詢「哪幾科未繳」
補考名單                    →  導師可見，協助提醒學生
```

### 9-3 操行成績自動計算流程

```
DisciplineRecord 新增/刪除
    ↓
ViewModel 重新計算：85 + Σ scores
    ↓
SemesterRecord.conductScore 更新
    ↓
UI 即時反映（StateFlow）
```

---

## 十、實作優先順序

### Phase 1（學期開學前必備）

1. **`homeroomFeatures` 補齊** — FeatureData.kt 更新
2. **`LeaveRequestScreen`** — 假單審核（家長最常用功能）
3. **`DisciplineScreen`** — 獎懲管理（每週都需要）
4. **`StudentHealthInfo`** — 健康資訊 + 緊急聯絡人整合

### Phase 2（學期中持續使用）

5. **出缺席異常預警** — `AttendanceAlert` + 觸發邏輯
6. **班費管理** — `ClassFundScreen`
7. **親師溝通深化** — `ParentTeacherConference` + 追蹤

### Phase 3（學期末集中使用）

8. **學期評語輔助** — `SemesterRecord` + 模板庫
9. **操行成績計算** — 自動加減分 + 匯出
10. **整合儀表板** — 班級儀表板彙整所有 badge 資訊

---

## 十一、相關檔案索引

| 需新增/修改 | 路徑 | 說明 |
|------------|------|------|
| `HomeroomTeacherEntities.kt` | `entity/` | 7 個新 Entity |
| `CounselorDao.kt` | `dao/` | 新增上述 DAO 方法 |
| `AppDatabase.kt` | `data/local/` | version 15 → 16 |
| `Converters.kt` | `data/local/` | 新增 5 個 enum TypeConverter |
| `DisciplineScreen.kt` | `ui/screens/` | 新建 |
| `LeaveRequestScreen.kt` | `ui/screens/` | 新建 |
| `SemesterRecordScreen.kt` | `ui/screens/` | 新建 |
| `ClassFundScreen.kt` | `ui/screens/` | 新建 |
| `StudentHealthScreen.kt` | `ui/screens/` | 新建 |
| `ParentConferenceScreen.kt` | `ui/screens/` | 新建 |
| `HomeroomDashboardScreen.kt` | `ui/screens/` | 新建 |
| `HomeroomManagementScreen.kt` | `ui/screens/` | 擴充現有 6 項 → 更多 |
| `FeatureData.kt` | `ui/data/` | 更新 `homeroomFeatures` |
| `MainActivity.kt` | — | 新增所有路由 |
