# 行政教師工作台 — 功能規劃文件

> 專案：中學教師助手 App（`com.wade.school`）
> 更新日期：2026-06-16
> 狀態：規劃中，尚未實作

---

## 背景

目前 `FeatureData.kt` 中的 `adminFeatures` 僅有一行佔位符：

```kotlin
private val adminFeatures = listOf(
    FeatureGroup("公文簽核", listOf(
        FeatureItem("待簽公文", "有 3 件急件", "draw", "3", BadgeType.URGENT, "admin/docs")
    ))
)
```

本文件規劃完整的六大功能模組，供後續逐步實作。

---

## 功能模組總覽

| # | 模組 | 核心業務 | 實作優先 |
|---|------|---------|---------|
| ① | 公文管理 | 簽核、追蹤、歸檔 | ★★★ 最高 |
| ② | 行事曆與會議 | 校務行程、會議紀錄 | ★★★ 最高 |
| ③ | 公告管理 | 發布、分類、推播 | ★★☆ 中高 |
| ④ | 人事與排課 | 教師資料、課務時數 | ★★☆ 中高 |
| ⑤ | 學生事務 | 獎懲、活動、表單 | ★☆☆ 中 |
| ⑥ | 設備與場地 | 借用、報修、盤點 | ★☆☆ 中 |

---

## ① 公文管理

台灣學校行政最核心的日常工作，優先實作。

### 功能細項

| 功能 | 說明 | Badge 類型 |
|------|------|-----------|
| 待簽公文佇列 | 依急件/一般分類，顯示件數 | `URGENT`（急件） |
| 電子簽章 | PIN 碼確認模擬簽章，記錄時間戳 | — |
| 公文進度追蹤 | 收文 → 起草 → 簽核 → 發文 → 歸檔 | `INFO`（待處理數） |
| 文件掃描上傳 | 相機拍攝紙本，存為 PDF 或圖片 | — |

### 公文狀態機

```
DRAFT（起草）
  └→ PENDING_SIGN（待簽核）
       └→ SIGNED（已簽核）
            ├→ SENT（已發文）
            └→ ARCHIVED（已歸檔）
  └→ REJECTED（退件）→ DRAFT
```

### Room Entity 草稿

```kotlin
@Entity(tableName = "official_documents")
data class OfficialDocument(
    @PrimaryKey val docId: String,           // 公文字號
    val title: String,                        // 主旨
    val category: DocCategory,               // 人事 / 教學 / 總務 / 訓育 / 輔導
    val status: DocStatus,                   // DRAFT / PENDING_SIGN / SIGNED / SENT / ARCHIVED / REJECTED
    val isUrgent: Boolean = false,           // 急件
    val receivedAt: Long? = null,            // 收文時間
    val deadline: Long? = null,              // 辦理期限
    val signedAt: Long? = null,              // 簽核時間
    val attachmentPath: String? = null,      // 掃描檔路徑
    val note: String? = null                 // 備註
)

enum class DocStatus { DRAFT, PENDING_SIGN, SIGNED, SENT, ARCHIVED, REJECTED }
enum class DocCategory { PERSONNEL, ACADEMIC, GENERAL_AFFAIRS, STUDENT_AFFAIRS, COUNSELING, OTHER }
```

### FeatureData 項目

```kotlin
FeatureGroup("公文簽核", listOf(
    FeatureItem("待簽公文", "急件優先處理", "draw", "3", BadgeType.URGENT, "admin/docs/pending"),
    FeatureItem("公文查詢", "依字號、日期、類別搜尋", "search", null, BadgeType.NONE, "admin/docs/search"),
    FeatureItem("文件掃描", "拍照上傳紙本公文", "camera_alt", null, BadgeType.NONE, "admin/docs/scan"),
    FeatureItem("已歸檔公文", "已結案公文歸檔查閱", "archive", null, BadgeType.NONE, "admin/docs/archive")
)),
```

---

## ② 行事曆與會議

### 功能細項

| 功能 | 說明 | Badge 類型 |
|------|------|-----------|
| 校務行事曆 | 全校共用，分「全校/業務組」兩層 | `INFO`（今日事項數） |
| 會議通知與簽到 | 接收會議通知，出席時在 app 內簽到 | `WARNING`（待出席） |
| 會議紀錄 | 結構化填寫（討論項目 + 決議 + 負責人 + 期限） | — |
| 代課/補課申請 | 送出申請、追蹤審核狀態 | `INFO`（待審核數） |

### Room Entity 草稿

```kotlin
@Entity(tableName = "school_events")
data class SchoolEvent(
    @PrimaryKey val eventId: String,
    val title: String,
    val eventType: EventType,              // MEETING / ACTIVITY / HOLIDAY / EXAM / OTHER
    val scope: EventScope,                 // ALL_SCHOOL / ADMIN / DEPARTMENT
    val startAt: Long,
    val endAt: Long,
    val location: String? = null,
    val note: String? = null
)

@Entity(tableName = "meeting_minutes")
data class MeetingMinutes(
    @PrimaryKey val minutesId: String,
    val eventId: String,                   // 對應 SchoolEvent
    val agenda: String,                    // 討論事項（換行分隔）
    val resolutions: String,               // 決議事項
    val actionItems: String,               // 負責人 + 期限（JSON 字串）
    val createdAt: Long
)
```

### FeatureData 項目

```kotlin
FeatureGroup("行事曆與會議", listOf(
    FeatureItem("校務行事曆", "全校行程一覽", "calendar_month", "今日 2 件", BadgeType.INFO, "admin/calendar"),
    FeatureItem("會議出席簽到", "待出席會議 1 件", "how_to_reg", "1", BadgeType.WARNING, "admin/meetings/checkin"),
    FeatureItem("會議紀錄", "撰寫與查閱歷次紀錄", "edit_note", null, BadgeType.NONE, "admin/meetings/minutes"),
    FeatureItem("代課申請", "申請與審核代補課", "swap_horiz", null, BadgeType.NONE, "admin/substitute")
)),
```

---

## ③ 公告管理

與「學校資訊」模組（`SchoolInfoScreen`）互補：  
- **學校資訊**：從學校官網爬取公告（唯讀，給家長/教師查看）  
- **公告管理**：行政教師在 app 內起草、審核、發布公告（寫入）

### 功能細項

| 功能 | 說明 |
|------|------|
| 起草公告 | 填寫標題、分類 tag、內文、有效期間 |
| 送簽發布 | 草稿 → 主任核閱 → 發布 |
| 推播通知 | 發布後觸發 FCM，選擇推播對象（教師/家長/學生） |
| 公告統計 | 各公告的已讀人數（若有 backend 支援） |
| 分類管理 | 人事、教學、總務、宣導、榮譽榜等 tag |

### FeatureData 項目

```kotlin
FeatureGroup("公告管理", listOf(
    FeatureItem("發布公告", "起草並推播學校通知", "campaign", null, BadgeType.NONE, "admin/announcements/new"),
    FeatureItem("公告草稿", "尚未發布的草稿", "drafts", "2", BadgeType.INFO, "admin/announcements/drafts"),
    FeatureItem("歷史公告", "查閱已發布公告", "history", null, BadgeType.NONE, "admin/announcements/history"),
    FeatureItem("分類標籤", "管理公告分類", "label", null, BadgeType.NONE, "admin/announcements/tags")
)),
```

---

## ④ 人事與排課輔助

### 功能細項

| 功能 | 說明 |
|------|------|
| 教師基本資料 | 姓名、科別、聯絡方式、擔任職務（唯讀查詢） |
| 課務時數統計 | 本學期各教師排課時數，標示超/少排 |
| 代課審核 | 審核教師送出的代課申請 |
| 教師請假管理 | 查看教師請假紀錄，觸發代課需求 |

### 與現有代碼的關聯

- 教師資料可以 `MoeSchool` 的學校 code 為 FK，將來擴充為「教師資料表」
- 代課申請直接與 ② 行事曆模組的 `SchoolEvent` 連動

### FeatureData 項目

```kotlin
FeatureGroup("人事與排課", listOf(
    FeatureItem("教師名冊", "科別、聯絡資料查詢", "groups", null, BadgeType.NONE, "admin/personnel/list"),
    FeatureItem("課務時數", "本學期排課統計", "bar_chart", null, BadgeType.NONE, "admin/personnel/hours"),
    FeatureItem("代課審核", "待審核申請 1 件", "swap_horiz", "1", BadgeType.WARNING, "admin/personnel/substitute"),
    FeatureItem("請假紀錄", "教師出勤狀況查詢", "event_busy", null, BadgeType.NONE, "admin/personnel/leave")
)),
```

---

## ⑤ 學生事務

### 功能細項

| 功能 | 說明 |
|------|------|
| 學生獎懲紀錄 | 新增/查詢/統計功勛獎懲（和輔導模組 studentId 共用） |
| 社團 / 活動管理 | 建立活動、開放報名、人數上限、截止日 |
| 表單審核 | 借用場地、校外教學同意書、競賽報名等 |
| 統計報表匯出 | 獎懲統計、活動參與率，匯出 CSV |

### 與現有代碼的關聯

- `studentId` 對應 `Student.studentId`（Room entity）
- 獎懲記錄可新增 `DisciplineRecord` entity，避免污染輔導用的 `CounselingProfile`

### FeatureData 項目

```kotlin
FeatureGroup("學生事務", listOf(
    FeatureItem("獎懲紀錄", "記錄功過與獎懲", "gavel", null, BadgeType.NONE, "admin/student/discipline"),
    FeatureItem("活動報名", "社團、競賽、校外教學", "event", null, BadgeType.NONE, "admin/student/activities"),
    FeatureItem("表單審核", "各類借用與申請", "assignment_turned_in", "3", BadgeType.INFO, "admin/student/forms"),
    FeatureItem("統計報表", "匯出 CSV 統計資料", "summarize", null, BadgeType.NONE, "admin/student/reports")
)),
```

---

## ⑥ 設備與場地

### 功能細項

| 功能 | 說明 |
|------|------|
| 場地借用 | 日曆視圖顯示衝突，線上申請審核 |
| 設備借出/歸還 | QR code 掃描登記（QR 貼在設備上） |
| 維修報修單 | 填故障說明 + 拍照 → 推給總務組 |
| 財產設備盤點 | 每年盤點作業，勾選清單 |

### Room Entity 草稿

```kotlin
@Entity(tableName = "venue_bookings")
data class VenueBooking(
    @PrimaryKey val bookingId: String,
    val venueName: String,                  // 體育館 / 視聽教室 / 操場…
    val applicant: String,                  // 申請者姓名
    val purpose: String,                    // 用途說明
    val startAt: Long,
    val endAt: Long,
    val status: BookingStatus              // PENDING / APPROVED / REJECTED / CANCELLED
)

@Entity(tableName = "equipment_loans")
data class EquipmentLoan(
    @PrimaryKey val loanId: String,
    val equipmentQrCode: String,           // 設備 QR code
    val equipmentName: String,
    val borrower: String,
    val borrowedAt: Long,
    val returnedAt: Long? = null
)
```

### FeatureData 項目

```kotlin
FeatureGroup("設備與場地", listOf(
    FeatureItem("場地借用", "查詢與申請場地", "meeting_room", null, BadgeType.NONE, "admin/facility/venue"),
    FeatureItem("設備借出", "QR 掃碼借還設備", "qr_code_scanner", null, BadgeType.NONE, "admin/facility/equipment"),
    FeatureItem("維修報修", "填寫並追蹤報修單", "build", "1", BadgeType.WARNING, "admin/facility/repair"),
    FeatureItem("財產盤點", "年度設備清點作業", "inventory", null, BadgeType.NONE, "admin/facility/inventory")
))
```

---

## 完整 adminFeatures 代碼

將以下內容替換 `FeatureData.kt` 中的 `adminFeatures`：

```kotlin
private val adminFeatures = listOf(
    FeatureGroup("公文簽核", listOf(
        FeatureItem("待簽公文", "急件優先處理", "draw", "3", BadgeType.URGENT, "admin/docs/pending"),
        FeatureItem("公文查詢", "依字號、日期、類別搜尋", "search", null, BadgeType.NONE, "admin/docs/search"),
        FeatureItem("文件掃描", "拍照上傳紙本公文", "camera_alt", null, BadgeType.NONE, "admin/docs/scan"),
        FeatureItem("已歸檔公文", "已結案公文查閱", "archive", null, BadgeType.NONE, "admin/docs/archive")
    )),
    FeatureGroup("行事曆與會議", listOf(
        FeatureItem("校務行事曆", "全校行程一覽", "calendar_month", "今日 2 件", BadgeType.INFO, "admin/calendar"),
        FeatureItem("會議出席簽到", "待出席會議", "how_to_reg", "1", BadgeType.WARNING, "admin/meetings/checkin"),
        FeatureItem("會議紀錄", "撰寫與查閱歷次紀錄", "edit_note", null, BadgeType.NONE, "admin/meetings/minutes"),
        FeatureItem("代課申請", "申請與審核代補課", "swap_horiz", null, BadgeType.NONE, "admin/substitute")
    )),
    FeatureGroup("公告管理", listOf(
        FeatureItem("發布公告", "起草並推播學校通知", "campaign", null, BadgeType.NONE, "admin/announcements/new"),
        FeatureItem("公告草稿", "尚未發布的草稿", "drafts", "2", BadgeType.INFO, "admin/announcements/drafts"),
        FeatureItem("歷史公告", "查閱已發布公告", "history", null, BadgeType.NONE, "admin/announcements/history"),
        FeatureItem("分類標籤", "管理公告分類", "label", null, BadgeType.NONE, "admin/announcements/tags")
    )),
    FeatureGroup("人事與排課", listOf(
        FeatureItem("教師名冊", "科別、聯絡資料查詢", "groups", null, BadgeType.NONE, "admin/personnel/list"),
        FeatureItem("課務時數", "本學期排課統計", "bar_chart", null, BadgeType.NONE, "admin/personnel/hours"),
        FeatureItem("代課審核", "待審核申請", "swap_horiz", "1", BadgeType.WARNING, "admin/personnel/substitute"),
        FeatureItem("請假紀錄", "教師出勤狀況查詢", "event_busy", null, BadgeType.NONE, "admin/personnel/leave")
    )),
    FeatureGroup("學生事務", listOf(
        FeatureItem("獎懲紀錄", "記錄功過與獎懲", "gavel", null, BadgeType.NONE, "admin/student/discipline"),
        FeatureItem("活動報名", "社團、競賽、校外教學", "event", null, BadgeType.NONE, "admin/student/activities"),
        FeatureItem("表單審核", "各類借用與申請", "assignment_turned_in", "3", BadgeType.INFO, "admin/student/forms"),
        FeatureItem("統計報表", "匯出 CSV 統計資料", "summarize", null, BadgeType.NONE, "admin/student/reports")
    )),
    FeatureGroup("設備與場地", listOf(
        FeatureItem("場地借用", "查詢與申請場地", "meeting_room", null, BadgeType.NONE, "admin/facility/venue"),
        FeatureItem("設備借出", "QR 掃碼借還設備", "qr_code_scanner", null, BadgeType.NONE, "admin/facility/equipment"),
        FeatureItem("維修報修", "填寫並追蹤報修單", "build", "1", BadgeType.WARNING, "admin/facility/repair"),
        FeatureItem("財產盤點", "年度設備清點作業", "inventory", null, BadgeType.NONE, "admin/facility/inventory")
    ))
)
```

---

## 實作路徑建議

```
Phase 1（核心）
  └── ① 公文管理 Screen + OfficialDocument Entity + DAO
  └── ② 行事曆 Screen + SchoolEvent Entity

Phase 2（溝通）
  └── ③ 公告管理 Screen（與 SchoolInfoScreen 共用 Announcement 資料模型）
  └── ④ 人事查詢 Screen（唯讀，不需新 Entity）

Phase 3（業務擴充）
  └── ⑤ 學生事務 + DisciplineRecord Entity
  └── ⑥ 設備場地 + VenueBooking + EquipmentLoan Entity
```

---

## 相關檔案索引

| 檔案 | 路徑 | 備註 |
|------|------|------|
| `FeatureData.kt` | `ui/data/FeatureData.kt` | 修改 `adminFeatures` |
| `FeatureUI.kt` | `ui/screens/FeatureUI.kt` | `RoleFeatureContent` 使用，無需修改 |
| `AppDatabase.kt` | `data/local/AppDatabase.kt` | 新 Entity 加入後需更新 `version` |
| `CounselorDao.kt` | `data/local/dao/CounselorDao.kt` | 新增 admin 相關 DAO 方法 |
| `MoeSchool.kt` | `data/local/entity/MoeSchool.kt` | 學校代碼可作為 FK |
| `Student.kt` | `data/local/entity/Student.kt` | `studentId` 供獎懲模組使用 |
