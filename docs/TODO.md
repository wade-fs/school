# 科任教師平台 — 功能深化規劃文件

> 專案：中學教師助手 App（`com.wade.school`）
> 更新日期：2026-06-17
> 狀態：規劃中，Entity 已建立，Screen 待實作
> 基礎版本：school3.zip（DB version 15）

---

## 一、現況盤點

### 已有的基礎（school3）

| 檔案 | 內容 | 完整度 |
|------|------|--------|
| `SubjectTeacherViewModel.kt` | 課表、教案、作業、成績匯入 | ★★★☆☆ |
| `LessonPlanScreen.kt` | 108 課綱教案庫 CRUD | ★★★☆☆ |
| `AssignmentManagementScreen.kt` | 作業管理 + 繳交追蹤 | ★★★☆☆ |
| `GradeAnalysisScreen.kt` | 班級成績統計圖 | ★★☆☆☆ |
| `ClassroomTaggingScreen.kt` | 課堂表現標記 | ★★☆☆☆ |
| `AttendanceScreen.kt` | 出缺席點名 | ★★☆☆☆ |
| `Assignment.kt` | 作業 entity | 骨架 |
| `Submission.kt` | 繳交/成績 entity | 骨架 |
| `LessonPlan.kt` | 教案 entity | 骨架 |
| `ClassroomPerformance.kt` | 課堂表現 entity | 骨架 |

### `subjectFeatures`（FeatureData.kt 現況）

```kotlin
// 目前只有一項佔位符
private val subjectFeatures = listOf(
    FeatureGroup("教學管理", listOf(
        FeatureItem("教案模板", "108課綱核心素養", "topic", null, BadgeType.NONE, "subject/template")
    ))
)
```

---

## 二、台灣高中科任教師真實需求分析

### 典型工作情境

- 同時教 **6～8 個班**，每班 35 人左右
- 每週備課、出題、批改作業循環
- 學期末需計算**加權總成績**（平時 30% + 期中 30% + 期末 40%）
- 段考後需**分析成績分布**、PR 值、追蹤低標學生
- 缺考學生需安排**補考**，不及格需**重補修**
- 教學省思為教師評鑑必備記錄
- 課堂互動（隨機抽人、搶答）需有工具輔助

---

## 三、新增 Entity 規劃（`SubjectTeacherEntities.kt`）

### 3-1 成績權重設定 `GradeWeight`

```kotlin
@Entity(tableName = "grade_weights",
    indices = [Index(value = ["classId", "subjectName", "semester"], unique = true)])
data class GradeWeight(
    val id: Int = 0,
    val classId: String,
    val subjectName: String,
    val academicYear: Int,
    val semester: Int,
    val dailyWeight: Float = 0.3f,       // 平時成績佔比
    val midtermWeight: Float = 0.3f,     // 期中考佔比
    val finalWeight: Float = 0.4f,       // 期末考佔比
    // 平時成績子項目
    val homeworkWeight: Float = 0.4f,
    val participationWeight: Float = 0.3f,
    val quizWeight: Float = 0.3f
)
```

### 3-2 考試記錄 `ExamRecord` + `ExamScore`

```kotlin
enum class ExamType { QUIZ, MIDTERM, FINAL, MAKEUP, PRACTICE }

@Entity(tableName = "exam_records")
data class ExamRecord(
    val classId: String,
    val subjectName: String,
    val examName: String,        // 「第一次段考」「第三章小考」
    val examType: ExamType,
    val examDate: Long,
    val totalScore: Int = 100,
    val academicYear: Int,
    val semester: Int
)

@Entity(tableName = "exam_scores",
    indices = [Index(value = ["examId", "studentId"], unique = true)])
data class ExamScore(
    val examId: Int,
    val studentId: String,
    val score: Float,            // 允許小數
    val isAbsent: Boolean = false,
    val isMakeupDone: Boolean = false,
    val makeupScore: Float? = null
)
```

### 3-3 補考管理 `MakeupExam`

```kotlin
enum class MakeupExamStatus { PENDING, SCHEDULED, DONE, WAIVED }

@Entity(tableName = "makeup_exams")
data class MakeupExam(
    val studentId: String,
    val studentName: String,
    val classId: String,
    val subjectName: String,
    val originalExamId: Int,
    val reason: String,          // 病假/事假/缺考
    val scheduledDate: Long? = null,
    val status: MakeupExamStatus = MakeupExamStatus.PENDING,
    val makeupScore: Float? = null
)
```

### 3-4 課堂互動紀錄 `ClassroomInteraction`

```kotlin
enum class InteractionType {
    RANDOM_PICK, VOLUNTEER, QUESTION,
    ANSWER_CORRECT, ANSWER_WRONG, PARTICIPATION
}

@Entity(tableName = "classroom_interactions")
data class ClassroomInteraction(
    val studentId: String,
    val classId: String,
    val subjectName: String,
    val interactionType: InteractionType,
    val score: Int = 0,          // 加/扣分（可負值）
    val note: String? = null,
    val recordedAt: Long
)
```

### 3-5 教學省思 `TeachingReflection`

```kotlin
@Entity(tableName = "teaching_reflections")
data class TeachingReflection(
    val lessonPlanId: Int? = null,   // 可選關聯教案
    val classId: String,
    val subjectName: String,
    val teachingDate: Long,
    val topic: String,
    val whatWentWell: String = "",
    val whatToImprove: String = "",
    val studentResponse: String = "",
    val nextSteps: String = ""
)
```

### 3-6 108 課綱素養對應 `CompetencyMapping`

```kotlin
@Entity(tableName = "competency_mappings")
data class CompetencyMapping(
    val lessonPlanId: Int,
    val competencyCode: String,    // "A1" "B2" "C3"
    val competencyLabel: String,   // "身心素質與自我精進"
    val description: String = ""   // 本節如何融入
)
```

### 3-7 科任出缺席 `SubjectAttendance`

```kotlin
enum class AttendanceStatus {
    PRESENT, LATE, ABSENT, SICK, PERSONAL, OFFICIAL, EARLY_LEAVE
}

@Entity(tableName = "subject_attendance",
    indices = [Index(value = ["studentId", "classId", "date", "period"], unique = true)])
data class SubjectAttendance(
    val studentId: String,
    val studentName: String,
    val classId: String,
    val subjectName: String,
    val date: Long,
    val period: Int,
    val status: AttendanceStatus = AttendanceStatus.PRESENT
)
```

---

## 四、六大功能模組詳細規劃

---

### 4-1 我的課表（主頁核心）

**現況：** `currentLesson` 已有即時偵測邏輯，但 UI 只是佔位符。

**深化內容：**

```
課表主頁
├── 今日課程卡（高亮目前/下一節）
│   ├── 班級名稱 + 教室
│   ├── 時間倒數（距下課/上課 XX 分鐘）
│   └── 快速入口：點名 / 課堂互動 / 教案
├── 本週課表格（5×N grid）
│   ├── 顯示科目/班級/教室
│   └── 點擊節次→該班詳情
└── 班級總覽卡片列
    ├── 所有授課班級
    ├── 各班未批改作業數（badge）
    └── 各班近期考試提醒
```

**FeatureItem 路由：** `subject/timetable`

---

### 4-2 成績管理系統（最核心）

**台灣高中成績計算邏輯：**

```
學期總成績 = 平時成績 × 30% + 期中考 × 30% + 期末考 × 40%
平時成績   = 作業 × 40% + 課堂表現 × 30% + 小考平均 × 30%
```

**功能細項：**

| 功能 | 說明 |
|------|------|
| 成績權重設定 | 每班可自訂各項佔比，支援預設值 |
| 輸入考試成績 | 批次輸入/CSV 匯入，滿分可自訂 |
| 自動計算加權 | 即時顯示每位學生的學期總成績 |
| 成績分布圖 | 長條圖（分數段人數）、常態曲線 |
| PR 值計算 | 班級排名與全年級 PR 值 |
| 低標追蹤 | 自動標出低於 60 分（可自訂門檻）的學生 |
| 成績單匯出 | 班級成績表 CSV，可貼入 Excel/校務系統 |

**FeatureItem 路由：** `subject/grades`

**Screen 結構：**

```
GradeManagementScreen
├── Tab 1：成績輸入
│   ├── 選班級 + 選考試（ExamRecord）
│   ├── 學生列表 + 逐一輸入成績
│   └── 批次 CSV 匯入
├── Tab 2：加權計算
│   ├── 成績權重設定卡片
│   ├── 學生學期成績預覽表
│   └── 個別學生成績細項展開
└── Tab 3：成績分析
    ├── 分布圖（Canvas 繪製）
    ├── 統計摘要（平均/最高/最低/標準差）
    ├── PR 值排名列表
    └── 低標學生清單（可一鍵推輔導警示）
```

---

### 4-3 作業與評量管理（深化現有）

**現有問題：** `Assignment` 缺少「類型權重」，`Submission` 缺少「遲交」狀態。

**深化內容：**

```
AssignmentManagementScreen（深化版）
├── 作業清單
│   ├── 類型標籤：作業 / 小考 / 專題 / 實驗報告
│   ├── 繳交狀態（已繳N/M，逾期M人）
│   └── 點擊→進入批改頁面
├── 批改頁面
│   ├── 學生逐一評分 + 文字回饋
│   ├── 快速標記：優秀 / 可 / 需重做
│   └── 掃描上傳（紙本作業拍照）
└── 統計
    ├── 平均分 / 未繳人數
    ├── 與前次比較趨勢
    └── 未繳名單 → 一鍵通知導師
```

**新增 Submission 狀態：**

```kotlin
// 建議擴充 Submission.status
"待繳" / "已繳" / "已批改" / "遲交" / "免繳" / "需重做"
```

**FeatureItem 路由：** `subject/assignments`

---

### 4-4 補考管理

**台灣高中補考流程：**
1. 段考缺考 → 自動產生補考需求
2. 教師排定補考日期通知學生
3. 補考後輸入成績
4. 學期末統計不及格 → 標記重補修

**功能細項：**

| 功能 | 說明 |
|------|------|
| 待補考名單 | 依班級/科目篩選，顯示缺考原因 |
| 排定補考 | 設定日期時間，推播通知學生/導師 |
| 輸入補考成績 | 補考成績可設上限（如最高 59 分） |
| 重補修預警 | 學期末自動標出學期總成績 < 60 的學生 |
| 匯出清單 | 補考/重補修名冊 CSV |

**FeatureItem 路由：** `subject/makeup`

**Screen 結構：**

```
MakeupExamScreen
├── 待補考（PENDING badge 數）
├── 已排定（SCHEDULED）
└── 已完成（DONE）
```

---

### 4-5 課堂互動工具

**設計概念：** 上課時快速操作，一手可用，螢幕大字顯示。

**功能細項：**

| 工具 | 說明 |
|------|------|
| 隨機抽人 | 班級名單隨機選出，大字顯示姓名（轉盤動畫） |
| 課堂加分板 | 快速記錄學生加/扣分，積分排行榜 |
| 分組工具 | N 人隨機分成 M 組，可鎖定/排除特定學生 |
| 課堂計時器 | 討論/測驗倒數計時，全螢幕顯示 |
| 問答搶答 | 學生按 App 搶答，教師端顯示搶到的人 |

**FeatureItem 路由：** `subject/interaction`

**Screen 結構：**

```
ClassroomInteractionScreen
├── 頂部：選擇班級（今日授課班快速切換）
├── 隨機抽人按鈕（佔大半畫面）
│   └── 動畫轉動 → 顯示姓名
├── 積分快速記錄
│   ├── 學生列表 + +1 / -1 按鈕
│   └── 本學期積分累計（→影響課堂表現分數）
└── 其他工具入口（分組/計時/搶答）
```

**與成績系統整合：**
課堂互動積分 → 自動匯入 `ClassroomInteraction` → 影響平時成績「課堂表現」欄位

---

### 4-6 教學省思日誌

**用途：** 教師評鑑、教學檔案、108 課綱實踐記錄。

**功能細項：**

| 功能 | 說明 |
|------|------|
| 快速記錄 | 課後 3 分鐘填完（四個欄位） |
| 關聯教案 | 可選擇對應的 LessonPlan |
| 月曆視圖 | 查看哪天有省思記錄 |
| 搜尋篩選 | 依班級/科目/日期範圍搜尋 |
| 匯出 | 整學期省思日誌 PDF/文字檔（用於評鑑） |

**四個填寫欄位（簡單設計）：**

```
本節主題：_______________
✅ 做得好的地方：
_______________
🔧 可以改進：
_______________
👥 學生反應：
_______________
➡️ 下次調整：
_______________
```

**FeatureItem 路由：** `subject/reflection`

---

## 五、108 課綱核心素養對應表

教案建立時可快速勾選，供省思與評鑑使用：

| 代碼 | 面向 | 素養項目 |
|------|------|---------|
| A1 | 自主行動 | 身心素質與自我精進 |
| A2 | 自主行動 | 系統思考與解決問題 |
| A3 | 自主行動 | 規劃執行與創新應變 |
| B1 | 溝通互動 | 符號運用與溝通表達 |
| B2 | 溝通互動 | 科技資訊與媒體素養 |
| B3 | 溝通互動 | 藝術涵養與美感素養 |
| C1 | 社會參與 | 道德實踐與公民意識 |
| C2 | 社會參與 | 人際關係與團隊合作 |
| C3 | 社會參與 | 多元文化與國際理解 |

---

## 六、完整 `subjectFeatures` 代碼

```kotlin
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
```

---

## 七、DAO 新增方法（`CounselorDao.kt`）

```kotlin
// ── 成績權重 ──────────────────────────────────────────────
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertGradeWeight(w: GradeWeight)

@Query("SELECT * FROM grade_weights WHERE classId = :classId AND subjectName = :subject AND semester = :sem")
suspend fun getGradeWeight(classId: String, subject: String, sem: Int): GradeWeight?

// ── 考試記錄 ──────────────────────────────────────────────
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertExamRecord(r: ExamRecord): Long

@Query("SELECT * FROM exam_records WHERE classId = :classId ORDER BY examDate DESC")
fun getExamsByClass(classId: String): Flow<List<ExamRecord>>

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertExamScore(s: ExamScore)

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertExamScores(list: List<ExamScore>)

@Query("SELECT * FROM exam_scores WHERE examId = :examId ORDER BY studentName")
fun getScoresByExam(examId: Int): Flow<List<ExamScore>>

@Query("SELECT * FROM exam_scores WHERE studentId = :studentId")
fun getScoresByStudent(studentId: String): Flow<List<ExamScore>>

@Query("SELECT AVG(score) FROM exam_scores WHERE examId = :examId AND isAbsent = 0")
fun getExamAverage(examId: Int): Flow<Double?>

// ── 補考管理 ──────────────────────────────────────────────
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertMakeupExam(m: MakeupExam)

@Query("SELECT * FROM makeup_exams WHERE status = 'PENDING' ORDER BY createdAt DESC")
fun getPendingMakeups(): Flow<List<MakeupExam>>

@Query("SELECT * FROM makeup_exams WHERE classId = :classId ORDER BY createdAt DESC")
fun getMakeupsByClass(classId: String): Flow<List<MakeupExam>>

@Query("SELECT COUNT(*) FROM makeup_exams WHERE status = 'PENDING'")
fun getPendingMakeupCount(): Flow<Int>

// ── 課堂互動 ──────────────────────────────────────────────
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertInteraction(i: ClassroomInteraction)

@Query("""SELECT * FROM classroom_interactions
    WHERE classId = :classId AND subjectName = :subject
    ORDER BY recordedAt DESC""")
fun getInteractionsByClass(classId: String, subject: String): Flow<List<ClassroomInteraction>>

@Query("""SELECT studentId, SUM(score) as totalScore
    FROM classroom_interactions
    WHERE classId = :classId
    GROUP BY studentId ORDER BY totalScore DESC""")
fun getInteractionScoreSummary(classId: String): Flow<List<StudentScoreSummary>>

// ── 教學省思 ──────────────────────────────────────────────
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertReflection(r: TeachingReflection): Long

@Query("SELECT * FROM teaching_reflections WHERE classId = :classId ORDER BY teachingDate DESC")
fun getReflectionsByClass(classId: String): Flow<List<TeachingReflection>>

@Query("SELECT * FROM teaching_reflections ORDER BY teachingDate DESC")
fun getAllReflections(): Flow<List<TeachingReflection>>

// ── 科任出缺席 ────────────────────────────────────────────
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertSubjectAttendance(a: SubjectAttendance)

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsertSubjectAttendances(list: List<SubjectAttendance>)

@Query("""SELECT * FROM subject_attendance
    WHERE classId = :classId AND date = :date AND period = :period""")
fun getSubjectAttendance(classId: String, date: Long, period: Int): Flow<List<SubjectAttendance>>
```

---

## 八、AppDatabase 更新

```kotlin
// 新增 entity，version 15 → 16
entities = [
    ...existing...,
    GradeWeight::class,
    ExamRecord::class,
    ExamScore::class,
    MakeupExam::class,
    ClassroomInteraction::class,
    TeachingReflection::class,
    CompetencyMapping::class,
    SubjectAttendance::class,
]
version = 16
```

---

## 九、Converters 新增

```kotlin
// 加入 Converters.kt
@TypeConverter fun fromExamType(v: ExamType): String = v.name
@TypeConverter fun toExamType(v: String): ExamType = ExamType.valueOf(v)

@TypeConverter fun fromMakeupExamStatus(v: MakeupExamStatus): String = v.name
@TypeConverter fun toMakeupExamStatus(v: String): MakeupExamStatus = MakeupExamStatus.valueOf(v)

@TypeConverter fun fromInteractionType(v: InteractionType): String = v.name
@TypeConverter fun toInteractionType(v: String): InteractionType = InteractionType.valueOf(v)

@TypeConverter fun fromAttendanceStatus(v: AttendanceStatus): String = v.name
@TypeConverter fun toAttendanceStatus(v: String): AttendanceStatus = AttendanceStatus.valueOf(v)
```

---

## 十、實作優先順序

### Phase 1（最高優先，日常必用）

1. **`subjectFeatures` 補齊** — FeatureData.kt 更新，讓 UI 先跑起來
2. **成績管理 Screen** — `GradeManagementScreen`（含輸入+加權計算+分析三 Tab）
3. **補考管理 Screen** — `MakeupExamScreen`（缺考自動產生→排程→輸入結果）

### Phase 2（次優先，提升效率）

4. **課堂互動工具** — `ClassroomInteractionScreen`（隨機抽人、加分板）
5. **科任點名深化** — 整合 `SubjectAttendance`，支援跨班切換
6. **作業深化** — 新增遲交狀態、未繳通知導師功能

### Phase 3（教學品質）

7. **教學省思日誌** — `TeachingReflectionScreen`（四格快速填寫）
8. **108 課綱素養對應** — 教案建立時勾選 `CompetencyMapping`
9. **低標學生推輔導警示** — 成績 < 60 → 觸發 `RiskAlert`（與輔導模組整合）

---

## 十一、相關檔案索引

| 需新增/修改 | 路徑 | 說明 |
|------------|------|------|
| `SubjectTeacherEntities.kt` | `entity/` | 所有新 Entity（已建立）|
| `CounselorDao.kt` | `dao/` | 新增上述 DAO 方法 |
| `AppDatabase.kt` | `data/local/` | version 15 → 16，加入新 entity |
| `Converters.kt` | `data/local/` | 新增 4 個 enum TypeConverter |
| `SubjectTeacherViewModel.kt` | `ui/screens/` | 加入成績/補考/互動方法 |
| `GradeManagementScreen.kt` | `ui/screens/` | 新建（含三 Tab）|
| `MakeupExamScreen.kt` | `ui/screens/` | 新建 |
| `ClassroomInteractionScreen.kt` | `ui/screens/` | 新建 |
| `TeachingReflectionScreen.kt` | `ui/screens/` | 新建 |
| `FeatureData.kt` | `ui/data/` | 更新 `subjectFeatures` |
| `MainActivity.kt` | — | 新增所有路由 |
