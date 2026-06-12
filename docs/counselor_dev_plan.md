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

### Sprint 2 — 心情溫度計（✅ 完整）

Entity 和 DAO 都已就緒，已完成 UI 和 ViewModel 方法。

#### 2-A：ViewModel 新增方法 (✅)

- `startMoodCheckSession`: 發起施測
- `recordMoodResponse`: 紀錄學生分數
- `getClassMoodAlerts`: 班級預警邏輯 (分數 <= 3 或 下降 >= 2)

#### 2-B：UI 規劃 (✅)

- `CounselingDashboard` 新增「心情溫度計」卡片與預警 Banner
- `MoodCheckScreen`: 班級選擇、學生滑桿輸入、完成送出

---

### Sprint 3 — 危機事件記錄 (✅ 完整)

Entity（`CrisisEvent`）和 DAO 已就緒。

#### 3-A：ViewModel 新增方法 (✅)

```kotlin
fun reportCrisisEvent(...)
fun getCrisisEventsForStudent(...)
```

#### 3-B：UI 規劃 (✅)

- `StudentDetailScreen` 新增「⚠ 通報危機事件」按鈕
- `CrisisReportDialog`: 事件類型、嚴重程度、描述、行動、通知與轉介
- 歷史事件顯示: `CrisisEventItem` 列出事件類型與詳情

---

### Sprint 4 — 導師協作備忘 (✅ 完整)

Entity（`CounselorTeacherNote`）已就緒。

#### 4-A：補充 DAO 方法 (✅)

```kotlin
suspend fun insertCounselorNote(...)
fun getUnreadNotesForTeacher(...)
fun getNotesForStudent(...)
suspend fun markNoteAsRead(...)
```

#### 4-B：ViewModel 新增方法 (✅)

```kotlin
fun sendNoteToTeacher(...)
fun getNotesForStudent(...)
fun markNoteAsRead(...)
```

#### 4-C：UI 規劃 (✅)

- `StudentDetailScreen` 新增「✉ 通知導師」按鈕
- `CounselorNoteDialog`: 請求類型 Chip、備忘摘要輸入
- 歷史顯示: `TeacherNoteItem` 顯示請求類型與摘要，包含導師已閱狀態

---

### Sprint 5 — 校外資源庫 (✅ 完整)

Entity（`ExternalResource`）已就緒。

#### 5-A：預填內建資料 (✅)

在 `AppDatabase` 的 `onCreate` callback 中實作了預填邏輯：
- 安心專線 (1925)
- 生命線 (1995)
- 兒童保護專線 (113)
- 少年專線
- 張老師專線

#### 5-B：UI 規劃 (✅)

- `ExternalResourceScreen`: 分為「緊急求助」與「常用機構」兩大區塊。
- 撥打功能: 點擊按鈕直接開啟系統撥號介面。
- 入口: `CounselingDashboard` 新增「校外資源庫」功能卡片。

---

### Sprint 6 — 稽核日誌 (✅ 完整)

Entity（`AuditLog`）已就緒。

#### 6-A：補充 DAO 方法 (✅)

```kotlin
suspend fun insertAuditLog(...)
fun getRecentAuditLogs(...)
```

#### 6-B：自動寫入時機 (✅)

在 `CounselorViewModel` 中實作了 `logAudit` 私有方法，並整合至以下操作：
- `importCsv` / `promoteAllStudents` / `clearAllStudents` (Bulk)
- `saveCaseLog` (CREATE) / `getLogsForStudent` (VIEW)
- `reportCrisisEvent` (CREATE) / `getCrisisEventsForStudent` (VIEW)
- `setStudentStatus` / `toggleKeyTracking` (UPDATE)
- `scheduleAppointment` (CREATE)
- `sendNoteToTeacher` (CREATE)

#### 6-C：UI (✅)

已確保資料正確寫入資料庫，可供未來管理員頁面查詢。

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
