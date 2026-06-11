# 台灣高中校園 App 架構設計筆記

> 涵蓋範圍：角色設計、功能規劃、三端架構、資料互動、Android 實作方向

---

## 一、教師角色分類與功能需求

### 核心設計原則

台灣高中教師最大的痛點是「角色疊加」：同一個人可能同時是導師、科任教師、還身兼行政職。App 應以**角色為軸心**，讓每位教師只看到跟自己職責相關的功能。

---

### 導師功能樹

**出缺席管理**
- 快速點名（QR 掃碼 / 人臉辨識）
- 請假審核流程
- 出缺席統計圖表（月趨勢）
- 家長自動推播規則設定

**家校聯繫**
- 聯絡簿訊息（家長回覆）
- 班級廣播通知
- 家長會排程與出席簽到
- 已讀回執追蹤

**班級事務**
- 學生名冊快查（緊急聯絡人、健康狀況）
- 週記 / 日記批閱（拍照批示）
- 榮譽 / 警告紀錄
- 班費收支管理
- 座位表管理（拖曳式）

**學習歷程輔助（108 課綱）**
- 學習歷程上傳截止提醒
- 班級上傳進度追蹤

---

### 科任教師功能樹

**教材管理**
- 教案模板庫（依 108 課綱核心素養分類）
- 教材版本控制、跨班複用
- 素養題型參考庫
- 備課筆記

**作業與評量**
- 線上派發作業（附截止時間、附件）
- 繳交狀況與未繳名單
- AI 輔助批改（初稿評分、關鍵字標記）
- 多元評量紀錄（口語、實作、同儕互評）
- 題庫出卷（隨機出題、難易度配比）

**成績分析**
- 班級成績分佈（雷達圖、箱型圖）
- 個別學生跨學期學習曲線
- AI 弱點診斷報告
- 成績匯出（Excel / PDF）

**課堂互動**
- 即時搶答 / 票選（投影幕同步）
- 隨機點名抽籤
- 課堂表現快速標記

---

### 行政教師功能樹

**公文與簽核**
- 待簽公文（急件優先）
- 公文流程追蹤
- 表單一站申請（請假、差旅、採購）
- 電子簽名與授權代理設定

**會議與行事曆**
- 校務行事曆同步（含個人行程整合）
- 會議召集與議程管理
- 會議紀錄與決議追蹤

**場地與設備**
- 場地借用申請（體育館、視聽教室）
- 時段衝突自動偵測
- 設備盤點與維修申報

**統計報表**
- 出缺席彙整（全校 / 年級 / 班級）
- 活動參與率統計
- 自訂報表匯出（Word / Excel / PDF）

---

### 輔導教師功能樹

**個案管理（加密環境）**
- 個案清單（依高中低風險分類）
- 加密晤談紀錄（SOAP 格式輔助）
- 跨師協作備忘（去識別化分享）
- 轉介流程與外部資源追蹤

**心理健康篩檢**
- 心情溫度計全班週測
- 標準化量表線上施測（PHQ-9、GAD-7、BAI）
- 高風險預警名單推送
- 危機處理 SOP 快查

**晤談排程**
- 個人晤談時段管理
- 學生自助預約入口
- 不赴約記錄與自動提醒
- 家長同意書電子版

**資源庫**
- 校外諮商資源（各縣市）與 24 小時專線
- 心理健康班會教材下載

---

### 科主任功能樹

**課程規劃**
- 108 課綱學分結構合規檢核
- 選修課程開課管理（含停開警示）
- 課程地圖視覺化
- 跨科協作空間

**教師督導與專業成長**
- 教學觀察記錄（課室觀察表）
- 同儕共備追蹤
- 新進教師輔導里程碑
- 教師研習時數統計與達標提醒

**成效分析**
- 學測 / 會考歷年趨勢
- 跨班教學成效對比
- 課程滿意度匿名調查
- 外部評鑑自評資料整合

---

## 二、三端角色架構（教師 / 學生 / 家長）

### APK 打包策略建議

| 方案 | 優點 | 缺點 |
|------|------|------|
| 單一 APK | 只維護一份 code | 介面複雜、Play Store 定位模糊 |
| 三個獨立 APK | UX 乾淨、功能精準 | 三份打包流程 |
| **Gradle Product Flavor（建議）** | 同一 code base、共用元件、三個打包目標 | 需要規劃好 module 邊界 |

**結論**：使用 Android **Product Flavor** 最合理。共享資料層（Repository、API、DB）與 UI 元件（Design System），只有功能模組依角色不同。

---

### 各端功能邊界

#### 教師端
- 讀寫全班資料
- 成績登錄、作業批改、發送通知
- 教師內部角色再細分（導師、科任、行政…）

#### 學生端
- 只讀自己的資料（成績、出缺席、通知）
- 可寫：作業繳交、補點名申請、簽到
- 不能看到其他同學資料

#### 家長端
- 唯讀綁定孩子的資料
- 限制：`parent_id` 綁定 `student_id`，API 層強制過濾
- 可寫：回覆教師訊息、確認通知已讀

---

## 三、資料互動技術架構

### JWT Token 設計

後端登入後回傳 JWT，token 內帶齊角色與權限：

```json
{
  "sub": "user_id_abc",
  "role": "teacher",
  "school_id": "school_123",
  "class_ids": ["cls_1a", "cls_2b"],
  "teacher_roles": ["homeroom", "subject"],
  "child_id": null
}
```

```json
{
  "sub": "user_id_xyz",
  "role": "parent",
  "school_id": "school_123",
  "child_id": "student_456"
}
```

> ⚠️ 重要：角色過濾**必須在後端 API 層做**，前端不能只靠 UI 隱藏，任何人都可以直接呼叫 API。

---

### API 層強制角色過濾（範例）

```kotlin
// 後端邏輯（任何語言皆同）
fun getAttendance(token: JwtClaims, targetClassId: String): List<AttendanceRecord> {
    return when (token.role) {
        "teacher" -> db.queryWholeClass(targetClassId)      // 全班
        "student" -> db.queryByStudent(token.sub)           // 只有自己
        "parent"  -> db.queryByStudent(token.childId!!)     // 只有孩子
        else      -> throw UnauthorizedException()
    }
}
```

---

### 即時資料推播

建議搭配 **WebSocket（即時雙向）** 或 **Server-Sent Events（即時單向）**，不要讓學生、家長端輪詢 API：

```
教師點名 → POST /attendance → 寫入 DB
                             → 觸發 FCM 推播
                                ├── topic: school_123_class_1a_parents  → 通知家長
                                └── topic: school_123_class_1a_students → 通知學生
```

**常見互動場景**

| 動作 | 寫入方 | 接收通知方 |
|------|--------|-----------|
| 教師點名 | 教師 | 學生（自己狀態）、家長（孩子出缺席） |
| 作業批改完成 | 教師 | 學生（成績出來了） |
| 發送班級公告 | 教師 | 學生、家長 |
| 學生繳交作業 | 學生 | 教師（有新繳交） |
| 家長回覆訊息 | 家長 | 教師（有回覆） |
| 輔導老師新增預警 | 輔導教師 | 導師（需關注） |

---

## 四、Android 端 Kotlin 實作方向

### 登入流程調整

目前 `MainActivity.kt` 是進 App 後讓使用者選角色，應改為登入後由伺服器決定：

```kotlin
// 原本（使用者自選）
NavHost(startDestination = "role_selector")

// 改為（token 決定）
val role = authViewModel.currentUser.role
NavHost(startDestination = "dashboard/$role")
```

---

### User 資料模型

```kotlin
enum class UserRole { TEACHER, STUDENT, PARENT }

data class AppUser(
    val id: String,
    val name: String,
    val role: UserRole,
    // 教師內部角色（可多選）
    val teacherRoles: List<String>? = null,  // "homeroom", "subject", "admin"...
    // 家長綁定的孩子
    val childStudentId: String? = null,
    // 教師負責的班級
    val classIds: List<String> = emptyList()
)
```

---

### 功能資料模型（FeatureData.kt）

```kotlin
data class FeatureItem(
    val title: String,
    val subtitle: String,
    val badge: String? = null,
    val badgeType: BadgeType = BadgeType.NONE,
    val route: String
)

enum class BadgeType { NONE, URGENT, INFO, WARNING, SUCCESS }

data class FeatureGroup(
    val groupTitle: String,
    val items: List<FeatureItem>
)

fun getFeaturesForRole(role: String): List<FeatureGroup> = when (role) {
    "homeroom"   -> homeroomFeatures
    "subject"    -> subjectFeatures
    "admin"      -> adminFeatures
    "counseling" -> counselingFeatures
    "dept_head"  -> deptHeadFeatures
    else         -> emptyList()
}
```

---

### Navigation 架構

```
role_selector（首次使用）
    ↓
login（每次啟動）
    ↓ JWT 回傳 role
dashboard/{role}
    ├── teacher/{teacherRole}   → homeroom / subject / admin / counseling / dept_head
    ├── student
    └── parent
         └── child/{childId}
```

---

### Repository 層建議

```kotlin
interface AttendanceRepository {
    // 教師用：全班資料
    suspend fun getClassAttendance(classId: String, date: LocalDate): List<AttendanceRecord>
    suspend fun submitAttendance(records: List<AttendanceRecord>)

    // 學生用：只有自己
    suspend fun getMyAttendance(studentId: String): List<AttendanceRecord>

    // 家長用：孩子資料（後端過濾，前端只需傳 token）
    suspend fun getChildAttendance(): List<AttendanceRecord>
}
```

---

## 五、重要設計考量

### 安全性
- 輔導個案資料必須**加密儲存**（Android Keystore + AES-256）
- 所有含個人資料的 API 都要 HTTPS
- Token refresh 機制，避免長期有效的 token 外洩

### 台灣法規
- 學生個資受《個人資料保護法》保護
- 伺服器建議落地台灣境內（學校採購決策考量）
- 輔導資料的存取紀錄需可稽核

### 離線支援
- 出缺席點名支援離線記錄，上線後同步（Room DB + WorkManager）
- 部分山區學校網路不穩，核心功能不能依賴網路

### 通知策略
- 教師通常已加入數十個 Line 群組，通知疲勞是真實問題
- 建議分級：急件（立即推播）、一般（摘要推播）、低優先（只進通知中心）

### 與現有系統整合
- 台灣高中普遍使用 **SchoolBO** 或各縣市校務行政平台
- 應提供 API 串接，避免教師重複輸入資料

---

## 六、功能數量彙整

| 角色 | 功能群組數 | 功能項數 |
|------|-----------|---------|
| 導師 | 4 | 14 |
| 科任教師 | 4 | 14 |
| 行政教師 | 4 | 13 |
| 輔導教師 | 4 | 14 |
| 科主任 | 3 | 12 |
| 學生端 | 3 | ~10 |
| 家長端 | 2 | ~8 |
| **合計** | **24** | **~85** |

---

*最後更新：依與 Claude 討論內容整理*
