# 輔導教師功能開發完整規劃

> 基於現有程式碼（`DashboardScreen.kt`、`StudentDetailScreen.kt`、`FeatureUI.kt`、  
> `CounselorViewModel.kt`、全套 Entity / DAO）的逐項盤點與開發計畫

---

## 一、現況盤點（已完成）

| 項目 | 狀態 | 所在位置 |
|------|------|---------|
| 學生名冊 CSV 匯入 | ✅ 完整 | `CounselorViewModel.importCsv()` |
| 學生清單搜尋與快速篩選 | ✅ 完整 | `CounselingDashboard` |
| `StudentWithProfile` 關聯查詢 | ✅ 完整 | `CounselorViewModel.studentsWithProfiles` |
| 個案晤談紀錄（加密儲存） | ✅ 完整 | `CounselorViewModel.saveCaseLog()` + `CaseLogCrypto` |
| 歷史紀錄解密顯示 | ✅ 完整 | `StudentDetailScreen.LogItem` |
| 語音辨識輸入（zh-TW） | ✅ 完整 | `StudentDetailScreen.startListening()` |
| 預約時間紀錄（欄位） | ✅ 有欄位，顯示硬編碼 | `CounselingProfile.nextAppointment` |
| 全體升學年 | ✅ 完整 | `CounselorViewModel.promoteAllStudents()` |
| 高風險 / 重點追蹤標記 | ✅ 完整 | `toggleKeyTracking()` / `setStudentStatus()` |
| Room Entity 定義 | ✅ 完整 | `Appointment`、`CrisisEvent`、`MoodCheck*`、`AuditLog`、`CounselorTeacherNote`、`ExternalResource` |
| Room DAO 定義 | ✅ 完整 | `CounselorDao` |

**結論**：資料層（Entity + DAO + ViewModel）非常完整。缺的幾乎全在 UI 層與 ViewModel 的部分方法。

---

## 二、已知問題（需先修）

### P0-1：預約時間顯示硬編碼

**位置**：`DashboardScreen.kt` 第 228 行
```kotlin
// 現況 — 假資料
supportingContent = { Text("預約時間: 2026-06-12 14:30") }
trailingContent = { Text("明日", color = ...) }

// 應改為
val ts = entry.profile?.nextAppointment ?: return@forEach
val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
val daysLeft = ((ts - System.currentTimeMillis()) / 86400000).toInt()
val trailingStr = when {
    daysLeft < 0  -> "已過期"
    daysLeft == 0 -> "今日"
    daysLeft == 1 -> "明日"
    else          -> "${daysLeft}天後"
}
supportingContent = { Text("預約時間: $dateStr") }
trailingContent = { Text(trailingStr, color = if (daysLeft <= 1) errorColor else normalColor) }
```

### P0-2：`CounselingDashboard` 學生清單沒有 LazyColumn

**位置**：`CounselingDashboard` 底部用 `forEach` 直接堆疊 Card，100 個學生就 render 100 個。

```kotlin
// 現況 — 效能殺手
filteredEntries.forEach { entry -> DashboardActionCard(...) }

// 應改為
LazyColumn {
    items(filteredEntries, key = { it.student.studentId }) { entry ->
        StudentListItem(entry, onNavigateToStudent)
    }
}
```

### P0-3：「分享本次文字稿」按鈕不應存在

`StudentDetailScreen` 有一個「分享本次文字稿」按鈕，會把個案內容用系統分享送出（Line、Gmail...）。這違反個案保密原則，**應直接移除**，只保留「儲存」。

---

## 三、開發規劃（按優先順序）

---

### Sprint 1 — 補齊既有功能的 UI（約 1 週）

#### 1-A：學生詳細資料編輯頁

**現況**：`StudentDetailScreen` 只能看狀態，無法修改輔導相關欄位。

**需新增的 UI 區塊**（加在 `StudentDetailScreen` 的 Card 裡）：

```kotlin
// 狀態下拉選單
var showStatusPicker by remember { mutableStateOf(false) }
val statusOptions = listOf("Active", "休學", "轉學", "結案", "外部轉介")

// 風險等級 Radio
val priorityOptions = listOf("Normal", "Low", "Medium", "High")

// 儲存鈕 → viewModel.setStudentStatus(studentId, status, legalStatus, priority)
```

UI 元件：`ExposedDropdownMenuBox`（狀態）、`RadioButton`（風險等級）、`OutlinedTextField`（法律狀態說明）

---

#### 1-B：預約排程功能

**現況**：`scheduleAppointment()` 方法存在，DAO 的 `upsertAppointment()` 也有，但 UI 只有示範按鈕。

**需新增**：

```
StudentDetailScreen
  └── 「新增預約」按鈕
        └── DatePickerDialog（選日期）
              └── TimePickerDialog（選時間）
                    └── 類型下拉（初談/後續/電訪/家訪）
                          └── viewModel.scheduleAppointment()
```

**ViewModel 需補充**：
```kotlin
// 從 Appointment 表讀取，不再只靠 CounselingProfile.nextAppointment
fun getAppointmentsForStudent(studentId: String): Flow<List<Appointment>> =
    dao.getUpcomingAppointments(startOfDay = 0L)
        .map { list -> list.filter { it.studentId == studentId } }

fun updateAppointmentStatus(id: Int, status: String) {
    viewModelScope.launch(Dispatchers.IO) {
        // 需在 DAO 補充 update query
    }
}
```

---

#### 1-C：今日晤談 Dashboard 動態化

**現況**：「即將到來的預約」只取第一筆，且顯示硬編碼時間。

**應改為**：

```kotlin
// 今日午夜的 timestamp
val startOfToday = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
}.timeInMillis

val todayAppointments by viewModel.getTodayAppointments(startOfToday).collectAsState(emptyList())

// 顯示今日全部預約，而非只取第一筆
// 每筆顯示：學生姓名、時間、類型、「標記完成」/ 「未赴約」按鈕
```

---

### Sprint 2 — 心情溫度計（約 1 週）

Entity 和 DAO 都已就緒（`MoodCheckSession`、`MoodCheckResponse`、相關 DAO 方法），需要建 UI 和 ViewModel 方法。

#### 2-A：ViewModel 新增方法

```kotlin
// 發起一次全班週測
fun startMoodCheckSession(classId: String, counselorId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val sessionId = dao.insertMoodCheckSession(
            MoodCheckSession(
                classId = classId,
                conductedAt = System.currentTimeMillis(),
                conductedBy = counselorId
            )
        )
        // TODO: 未來透過推播通知學生填寫
        _activeSessionId.value = sessionId.toInt()
    }
}

// 輔導老師手動代填（面談當下直接輸入）
fun recordMoodResponse(sessionId: Int, studentId: String, score: Int, note: String?) {
    viewModelScope.launch(Dispatchers.IO) {
        dao.insertMoodCheckResponse(
            MoodCheckResponse(sessionId = sessionId, studentId = studentId,
                              score = score, note = note)
        )
    }
}

// 班級預警：本週平均 < 4 或下降超過 2 分
fun getClassMoodAlert(classId: String): Flow<List<String>> // 回傳需關注學生 ID 清單
```

#### 2-B：UI 規劃

**入口**：`CounselingDashboard` 新增「心情溫度計」卡片，顯示「上次施測：X 天前，2 人需關注」

**施測頁 `MoodCheckScreen`**：
```
班級選擇（下拉）
  ↓
學生清單（每人一列）
  ├── 姓名 + 學號
  ├── 滑桿（1-10 分）或數字輸入
  └── 備註欄（可選填）
  ↓
「完成施測」按鈕 → 寫入 Room → 計算預警名單
```

**結果頁**：
- 班級分數長條圖（橫向，依分數高低排序）
- 紅色標示：分數 ≤ 3
- 黃色標示：比上次下降 ≥ 2 分
- 點擊學生 → 跳轉 `StudentDetailScreen`

---

### Sprint 3 — 危機事件記錄（約 3～5 天）

Entity（`CrisisEvent`）和 DAO 已就緒。

#### 3-A：ViewModel 新增方法

```kotlin
fun reportCrisisEvent(
    studentId: String,
    eventType: String,
    severity: String,
    actionTaken: String,
    occurredAt: Long = System.currentTimeMillis(),
    reportedBy: String,
    notifiedParent: Boolean = false,
    notifiedPrincipal: Boolean = false,
    referralUnit: String? = null
) {
    viewModelScope.launch(Dispatchers.IO) {
        dao.insertCrisisEvent(
            CrisisEvent(
                studentId = studentId,
                eventType = eventType,
                occurredAt = occurredAt,
                reportedBy = reportedBy,
                severity = severity,
                actionTaken = actionTaken,
                notifiedParent = notifiedParent,
                notifiedPrincipal = notifiedPrincipal,
                externalReferral = referralUnit != null,
                referralUnit = referralUnit
            )
        )
        // 高嚴重性事件 → 自動將學生風險等級設為 High
        if (severity == "緊急") setStudentStatus(studentId, "Active", null, "High")
    }
}

fun getCrisisEventsForStudent(studentId: String): Flow<List<CrisisEvent>> =
    dao.getCrisisEventsForStudent(studentId)
```

#### 3-B：UI 規劃

**入口**：`StudentDetailScreen` 頂部 Card 新增「⚠ 通報危機事件」按鈕（紅色）

**表單 `CrisisReportSheet`**（BottomSheet 或獨立頁）：
```
事件類型（Chip 多選）：
  自傷、自殺意念、霸凌（施）、霸凌（受）、家暴通報、其他

嚴重程度（RadioButton）：
  緊急、嚴重、一般

事件描述（OutlinedTextField，多行）

已採取行動（OutlinedTextField）

已通知家長 ☐ / 已通知校長 ☐

外部轉介（Toggle）→ 展開填寫機構名稱

[送出通報]
```

**歷史事件顯示**：`StudentDetailScreen` 歷史紀錄區塊下方新增「危機事件記錄」子區塊，依時間倒序列出，顯示事件類型 + 嚴重程度標籤 + 是否已轉介。

---

### Sprint 4 — 導師協作備忘（約 3 天）

Entity（`CounselorTeacherNote`）已就緒，DAO 中**尚未定義**相關 query，需補充。

#### 4-A：補充 DAO 方法

```kotlin
// 需加入 CounselorDao
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertCounselorNote(note: CounselorTeacherNote)

@Query("SELECT * FROM counselor_teacher_notes WHERE toTeacherId = :teacherId AND isRead = 0")
fun getUnreadNotesForTeacher(teacherId: String): Flow<List<CounselorTeacherNote>>

@Query("SELECT * FROM counselor_teacher_notes WHERE studentId = :studentId ORDER BY createdAt DESC")
fun getNotesForStudent(studentId: String): Flow<List<CounselorTeacherNote>>

@Query("UPDATE counselor_teacher_notes SET isRead = 1 WHERE id = :id")
suspend fun markNoteAsRead(id: Int)
```

#### 4-B：ViewModel 新增方法

```kotlin
fun sendNoteToTeacher(
    studentId: String,
    fromCounselorId: String,
    toTeacherId: String,
    summary: String,        // 去識別化，不含晤談細節
    requestType: String     // "請多關心" / "注意課堂行為" / "避免點名" / "其他"
) {
    viewModelScope.launch(Dispatchers.IO) {
        dao.insertCounselorNote(
            CounselorTeacherNote(
                studentId = studentId,
                fromCounselorId = fromCounselorId,
                toTeacherId = toTeacherId,
                summary = summary,
                requestType = requestType
            )
        )
    }
}
```

#### 4-C：UI 規劃

**入口**：`StudentDetailScreen` 新增「✉ 通知導師」按鈕

**表單**（Dialog）：
```
請求類型（Chip 單選）：
  請多關心、注意課堂行為、避免點名、其他

備忘摘要（OutlinedTextField）
  ⚠ 提示：「此欄位將傳送給導師，請勿填寫個案細節」

[送出]
```

---

### Sprint 5 — 校外資源庫（約 2 天）

Entity（`ExternalResource`）已就緒，需預填內建資料並建 UI。

#### 5-A：預填內建資料

```kotlin
// 在 AppDatabase.getDatabase() 加入 callback
.addCallback(object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // 用 coroutine 預填
        CoroutineScope(Dispatchers.IO).launch {
            instance.counselorDao().apply {
                insertExternalResource(ExternalResource(name="安心專線", phone="1925", type="24小時專線", city=null, isEmergency=true))
                insertExternalResource(ExternalResource(name="生命線", phone="1995", type="24小時專線", city=null, isEmergency=true))
                insertExternalResource(ExternalResource(name="兒童保護專線", phone="113", type="24小時專線", city=null, isEmergency=true))
                insertExternalResource(ExternalResource(name="少年專線", phone="0800-001769", type="全國專線", city=null, isEmergency=false))
                insertExternalResource(ExternalResource(name="張老師專線", phone="1980", type="24小時專線", city=null, isEmergency=false))
            }
        }
    }
})
```

#### 5-B：UI 規劃

**入口**：`CounselingDashboard` 底部「校外資源」固定區塊  
或從 `StudentDetailScreen` 的 TopBar action 進入

**資源清單頁 `ExternalResourceScreen`**：
```
緊急求助（紅色區塊）
  安心專線 1925  [撥打]
  生命線   1995  [撥打]
  兒少保護  113  [撥打]

其他資源（依縣市篩選）
  [縣市下拉]
  ...列表...
```

點擊「撥打」直接呼叫 `Intent(Intent.ACTION_DIAL, Uri.parse("tel:1925"))`，不需網路。

---

### Sprint 6 — 稽核日誌（約 2 天）

Entity（`AuditLog`）已就緒，DAO 目前**沒有** AuditLog 相關方法，需補充。

#### 6-A：補充 DAO 方法

```kotlin
// 需加入 CounselorDao
@Insert
suspend fun insertAuditLog(log: AuditLog)

@Query("SELECT * FROM audit_logs ORDER BY performedAt DESC LIMIT 200")
fun getRecentAuditLogs(): Flow<List<AuditLog>>
```

#### 6-B：自動寫入時機

在 `CounselorViewModel` 的各個操作方法中加入稽核記錄：

```kotlin
// 範例：每次查看學生個案自動記錄
fun logViewEvent(targetType: String, targetId: String, performedBy: String) {
    viewModelScope.launch(Dispatchers.IO) {
        dao.insertAuditLog(
            AuditLog(
                action = "VIEW",
                targetType = targetType,
                targetId = targetId,
                performedBy = performedBy
            )
        )
    }
}

// 加入的位置：
// - saveCaseLog()         → action="CREATE", targetType="CaseLog"
// - decryptLogContent()   → action="VIEW",   targetType="CaseLog"
// - reportCrisisEvent()   → action="CREATE", targetType="CrisisEvent"
// - setStudentStatus()    → action="UPDATE", targetType="CounselingProfile"
```

#### 6-C：UI

管理員入口（未來實作），目前只需確保資料有正確寫入。

---

## 四、`CounselingDashboard` 最終目標樣貌

完成所有 Sprint 後，Dashboard 應動態顯示：

```
┌─────────────────────────────────────────────┐
│  輔導個案管理              [匯入] [清空]      │
├─────────────────────────────────────────────┤
│  今日晤談  3 場（13:30、14:30、15:30）        │  ← Sprint 1-C
│  [陳小明 13:30 初談] [完成] [未赴約]          │
├─────────────────────────────────────────────┤
│  ⚠ 需關注  林志強、王大為（週測分數低落）     │  ← Sprint 2
├─────────────────────────────────────────────┤
│  🚨 待追蹤危機事件  2 件（未結案）            │  ← Sprint 3
├─────────────────────────────────────────────┤
│  💬 導師來訊  1 則未讀                        │  ← Sprint 4（導師端視角）
├─────────────────────────────────────────────┤
│  搜尋列 + 快速篩選                            │
│  全部 / 高風險 / 休學 / 法院                   │
├─────────────────────────────────────────────┤
│  學生清單（LazyColumn）                       │  ← Sprint 1 P0-2
│  ...                                         │
└─────────────────────────────────────────────┘
```

---

## 五、Navigation 架構規劃

目前 `MainActivity.kt` 的導航只有兩層（role_selector → dashboard），需擴充：

```
role_selector
  └── dashboard/counseling
        ├── student_detail/{studentId}/{studentName}   ← 已有
        ├── mood_check/{classId}                       ← Sprint 2 新增
        ├── crisis_report/{studentId}                  ← Sprint 3 新增
        └── external_resources                         ← Sprint 5 新增
```

**傳遞參數方式**（Compose Navigation）：
```kotlin
// MainActivity.kt 的 NavHost 新增路由
composable("mood_check/{classId}") { backStackEntry ->
    val classId = backStackEntry.arguments?.getString("classId") ?: return@composable
    MoodCheckScreen(classId = classId, onBack = { navController.popBackStack() })
}
```

---

## 六、開發順序總表

| Sprint | 功能 | 前置條件 | 預估工時 |
|--------|------|---------|---------|
| 先決 | 移除「分享文字稿」按鈕 | 無 | 10 分鐘 |
| 先決 | 預約時間顯示動態化 | 無 | 1 小時 |
| 先決 | 學生清單改 LazyColumn | 無 | 30 分鐘 |
| Sprint 1 | 學生狀態編輯 UI | 無 | 2 天 |
| Sprint 1 | 排程預約 UI + 今日晤談動態化 | 1-A | 3 天 |
| Sprint 2 | 心情溫度計施測 + 結果頁 | Sprint 1 | 5 天 |
| Sprint 3 | 危機事件通報表單 + 歷史顯示 | Sprint 1 | 3 天 |
| Sprint 4 | 補充 DAO + 導師協作備忘 UI | Sprint 3 | 3 天 |
| Sprint 5 | 校外資源庫 + 預填資料 | 無 | 2 天 |
| Sprint 6 | 稽核日誌寫入點 | Sprint 1~4 | 2 天 |

**總計預估**：約 3～4 週（單人開發，含測試）

---

*基於 `school.zip` 現有程式碼分析，2026-06-12*
