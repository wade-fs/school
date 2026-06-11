# 輔導教師模組：應興應革完整分析

> 基於現有程式碼（`CaseLog.kt`、`Student.kt`、`CounselorDao.kt`、  
> `CounselorViewModel.kt`、`StudentDetailScreen.kt`）的逐項檢討與建議

---

## 一、現有程式碼盤點：做得好的部分

在提出改進前，先肯定已有的良好設計：

| 已有功能 | 所在檔案 | 評價 |
|---------|---------|------|
| 語音轉文字輸入晤談紀錄 | `StudentDetailScreen.kt` | ✅ 符合輔導老師手邊忙、打字慢的實際場景 |
| 學生狀態分類（在學、休學、轉介…） | `Student.kt` | ✅ 考慮到台灣輔導實務中的多種狀態 |
| `legalStatus` 欄位 | `Student.kt` | ✅ 有考慮司法少年個案，少見但重要 |
| `isKeyTracking` 重點追蹤旗標 | `Student.kt` | ✅ 快速篩出高關注學生 |
| `promoteAllStudents()` 升學年 | `CounselorViewModel.kt` | ✅ 學年繼承邏輯已有雛形 |
| CSV 匯入學生名冊 | `CounselorViewModel.kt` | ✅ 對接學校現有資料的務實方案 |
| `tags` 欄位（逗號分隔字串） | `CaseLog.kt` | ✅ 有標籤概念，但實作方式需改進（見下） |

---

## 二、應革：現有問題清單

### 問題 1：個案內容完全未加密 ❗最高優先

**現況**
```kotlin
// CaseLog.kt
val content: String,   // 明文儲存，Room SQLite 無加密
```

**問題**
- 手機遺失或被他人取得，所有個案內容一覽無遺
- 違反《個人資料保護法》對敏感個資的保護義務
- SQLite 資料庫檔案可以用任何 DB Browser 直接打開讀取

**應改為**
```kotlin
// CaseLog.kt — 應用層加密版
val contentEncrypted: ByteArray,   // AES-256-GCM 加密後的位元組
val contentIv: ByteArray,          // 每筆記錄獨立的 IV（初始向量）
// 解密 key 存放在 Android Keystore，不在 APK 內
```

**加密實作方向**
```kotlin
object CaseLogCrypto {
    private const val KEY_ALIAS = "counselor_case_log_key"

    fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                .init(
                    KeyGenParameterSpec.Builder(KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setUserAuthenticationRequired(false) // 可設為 true 要求生物辨識
                        .build()
                ).generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encrypt(plainText: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return Pair(cipher.doFinal(plainText.toByteArray()), cipher.iv)
    }

    fun decrypt(encrypted: ByteArray, iv: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        return String(cipher.doFinal(encrypted))
    }
}
```

---

### 問題 2：`tags` 用逗號分隔字串儲存，無法有效查詢

**現況**
```kotlin
val tags: String? = null // 如: "家庭問題, 學習適應"
```

**問題**
- 無法用 SQL 查詢「所有標記為家庭問題的紀錄」
- 標籤名稱不一致（手打，可能出現「家庭問題」vs「家庭」vs「家庭議題」）
- 排序、統計都很困難

**應改為獨立的 Tag 表 + 關聯表**
```kotlin
@Entity(tableName = "case_tags")
data class CaseTag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,       // 例如 "家庭問題"
    val color: String       // 顯示顏色，例如 "#FF6B6B"
)

@Entity(tableName = "case_log_tags",
    primaryKeys = ["caseLogId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = CaseLog::class, parentColumns = ["id"], childColumns = ["caseLogId"]),
        ForeignKey(entity = CaseTag::class, parentColumns = ["id"], childColumns = ["tagId"])
    ])
data class CaseLogTag(
    val caseLogId: Int,
    val tagId: Int
)
```

**預設標籤清單**（依台灣輔導實務常見類別）
- 家庭問題、學習適應、人際關係、情緒困擾
- 生涯規劃、霸凌事件、自傷風險、網路成癮
- 法律事件、性別議題、藥物問題、轉介追蹤

---

### 問題 3：`StudentDetailScreen` 狀態硬編碼，且未接 ViewModel

**現況**
```kotlin
// StudentDetailScreen.kt — 硬編碼的假資料
Text("當前狀態: 休學中 (定期電訪追蹤)", fontWeight = FontWeight.Bold)
Text("上次晤談: 2026-05-10")
```

**問題**
- 顯示的是假資料，不是從 ViewModel 讀取的真實學生資料
- 儲存按鈕（「打包寄給自己」）只是呼叫系統分享，**沒有真正存入 Room**
- 語音辨識結果存在 `recognizedText` 本地狀態，App 關掉就消失

**應改為**
```kotlin
@Composable
fun StudentDetailScreen(
    studentId: String,
    viewModel: CounselorViewModel = viewModel(),
    onBack: () -> Unit
) {
    val student by viewModel.getStudent(studentId).collectAsState(initial = null)
    val caseLogs by viewModel.getLogsForStudent(studentId).collectAsState(initial = emptyList())
    // ... 從 Room 讀取真實資料，儲存時也寫回 Room
}
```

---

### 問題 4：ViewModel 資料只存在記憶體，App 重啟就消失

**現況**
```kotlin
// CounselorViewModel.kt
private val _students = MutableStateFlow<List<Student>>(emptyList())
// 學生資料只在記憶體中，未接 Room Database
```

**問題**
- `AppDatabase` 已建好，`CounselorDao` 也有，但 ViewModel **完全沒有使用它們**
- CSV 匯入後的資料只在記憶體，App 關掉就清空，下次重開要重新匯入

**應改為**
```kotlin
class CounselorViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.counselorDao()

    // 直接從 Room 觀察，資料永久保存
    val students: StateFlow<List<Student>> = dao.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = context.contentResolver.openInputStream(uri)?.use {
                CsvParser.parseStudentCsv(it)
            } ?: return@launch
            dao.insertStudents(list)  // 寫入 Room，自動持久化
        }
    }
}
```

---

### 問題 5：`Student` 表承載太多不同性質的資料

**現況**  
`Student.kt` 同時包含：學籍資料、輔導狀態、聯絡資訊、預約時間——全擠在同一張表。

**問題**
- 學籍資料（姓名、班級）應由教務系統管理，輔導教師不應能修改
- 輔導狀態（`priority`、`isKeyTracking`）是輔導專屬資料
- 混在一起讓職責不清，未來接後端 API 時會很亂

**建議拆分為**

```kotlin
// 學籍資料（唯讀，從教務匯入）
@Entity data class Student(
    @PrimaryKey val studentId: String,
    val name: String,
    val gender: String,
    val currentClass: String,
    val seatNo: Int,
    val entryYear: Int,
    val phone: String? = null,
    val guardianName: String? = null,
    val guardianPhone: String? = null
)

// 輔導專屬資料（輔導教師可讀寫）
@Entity(foreignKeys = [ForeignKey(entity = Student::class,
    parentColumns = ["studentId"], childColumns = ["studentId"])])
data class CounselingProfile(
    @PrimaryKey val studentId: String,
    val priority: String = "Normal",       // High / Medium / Low / Normal
    val isKeyTracking: Boolean = false,
    val status: String = "Active",         // Active / 休學 / 轉學 / 結案 / 外部轉介
    val statusNote: String? = null,
    val legalStatus: String? = null,
    val nextAppointment: Long? = null,
    val assignedCounselorId: String? = null  // 負責的輔導老師
)
```

---

### 問題 6：語音錄音只做辨識，沒有保留原始音檔

**現況**
```kotlin
val audioPath: String? = null // 本地音檔路徑（CaseLog.kt 有欄位，但未實作錄音儲存）
```

`StudentDetailScreen` 中只做語音轉文字，原始音檔沒有被儲存，`audioPath` 欄位閒置。

**建議**  
語音輸入分兩種模式讓輔導老師選擇：

1. **轉文字模式**（現有）：即時辨識，只存文字，適合正式記錄
2. **錄音備存模式**（待實作）：同時儲存原始 m4a 檔案到 App 私有目錄，`audioPath` 填入路徑，適合事後補記

注意：錄音備存模式需要特別說明隱私政策，且備份時不應上傳至雲端。

---

## 三、應興：現有程式碼缺少的重要功能

### 新增 1：晤談排程管理

目前 `Student.nextAppointment` 只存一個時間戳記，無法管理多筆排程。

```kotlin
@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val scheduledAt: Long,          // 預約時間
    val duration: Int = 50,         // 分鐘
    val type: String,               // "初談" / "後續晤談" / "電訪" / "家訪"
    val location: String? = null,   // "輔導室" / "教室" / "電話"
    val status: String = "scheduled", // scheduled / completed / no_show / cancelled
    val reminderSent: Boolean = false
)
```

**UI 需求**
- 輔導老師的日曆視圖（今日、本週的晤談時程）
- 「今日晤談」清單，點擊直接進入該學生的紀錄頁
- 若學生未出現，一鍵標記「未赴約」並自動記錄

---

### 新增 2：心情溫度計（全班週測）

這是台灣輔導實務中最常用的早期預警工具，目前完全沒有。

```kotlin
@Entity(tableName = "mood_check_sessions")
data class MoodCheckSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val conductedAt: Long,
    val conductedBy: String   // 輔導老師 ID
)

@Entity(tableName = "mood_check_responses")
data class MoodCheckResponse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val studentId: String,
    val score: Int,           // 1-10 分
    val note: String? = null  // 學生自願填寫的說明
)
```

**Dashboard 顯示邏輯**
- 週平均低於 4 分 → 橘色警示
- 本週分數比上週下降 2 分以上 → 黃色提醒
- 連續兩週低於 3 分 → 紅色，推送給輔導老師

---

### 新增 3：危機事件記錄

高風險事件（自傷、霸凌、家暴通報）需要比一般晤談紀錄更結構化的表單。

```kotlin
@Entity(tableName = "crisis_events")
data class CrisisEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val eventType: String,      // "自傷" / "自殺意念" / "霸凌" / "家暴通報" / "其他"
    val occurredAt: Long,
    val reportedAt: Long = System.currentTimeMillis(),
    val reportedBy: String,     // 誰通報（可能是導師，不一定是輔導老師）
    val severity: String,       // "緊急" / "嚴重" / "一般"
    val actionTaken: String,    // 採取的處理行動
    val followUpDate: Long? = null,
    val notifiedParent: Boolean = false,
    val notifiedPrincipal: Boolean = false,
    val externalReferral: Boolean = false,
    val referralUnit: String? = null   // 轉介單位名稱
)
```

**為什麼重要**  
台灣《學生輔導法》規定高關懷學生需有書面記錄，危機事件處理流程也需留存佐證。這張表就是法規要求的「書面紀錄」的數位版。

---

### 新增 4：稽核日誌（Audit Log）

輔導個案資料的存取本身也需要被記錄，供未來查驗。

```kotlin
@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,        // "VIEW" / "CREATE" / "UPDATE" / "DELETE"
    val targetType: String,    // "CaseLog" / "CrisisEvent" / "CounselingProfile"
    val targetId: String,
    val performedBy: String,   // 輔導老師帳號
    val performedAt: Long = System.currentTimeMillis(),
    val ipAddress: String? = null  // 未來接後端時記錄
)
```

---

### 新增 5：導師跨角色協作（去識別化分享）

目前沒有機制讓輔導老師把關鍵資訊通知導師，而不暴露個案細節。

```kotlin
@Entity(tableName = "counselor_teacher_notes")
data class CounselorTeacherNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val fromCounselorId: String,
    val toTeacherId: String,       // 導師
    val summary: String,           // 去識別化摘要，不含晤談細節
    val requestType: String,       // "請多關心" / "注意課堂行為" / "避免點名" / "其他"
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
```

---

### 新增 6：校外資源連結庫

目前缺少一個快速查詢的資源清單。

```kotlin
@Entity(tableName = "external_resources")
data class ExternalResource(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,           // 例如 "安心專線"
    val phone: String,          // "1925"
    val type: String,           // "24小時專線" / "諮商機構" / "社福單位" / "醫療"
    val city: String?,          // NULL 表示全國性
    val notes: String? = null,
    val isEmergency: Boolean = false  // 是否為緊急求助專線
)
```

**預設資料**（內建，不需連網）

| 名稱 | 電話 | 類型 |
|------|------|------|
| 安心專線 | 1925 | 24小時、全國 |
| 生命線 | 1995 | 24小時、全國 |
| 兒童保護專線 | 113 | 24小時、全國 |
| 少年專線 | 0800-001769 | 全國 |
| 各縣市家暴性侵專線 | 113 | 全國 |

---

## 四、應革：DashboardScreen 輔導教師入口

現有 `DashboardScreen.kt` 中輔導教師的入口只是靜態的功能卡，缺少即時狀態。

**應改為動態 Dashboard，顯示：**

```
┌─────────────────────────────────┐
│  今日晤談  3 場（下午 1:30 起）  │  ← 從 Appointment 表讀取
├─────────────────────────────────┤
│  ⚠ 需關注  小明、小華（週測低分）│  ← 從 MoodCheckResponse 讀取
├─────────────────────────────────┤
│  📋 待處理  危機事件後續追蹤 2 件 │  ← 從 CrisisEvent 讀取
├─────────────────────────────────┤
│  💬 導師來訊  3 則未讀            │  ← 從 CounselorTeacherNote 讀取
└─────────────────────────────────┘
```

---

## 五、優先順序建議

依重要性與開發難度排列：

| 優先 | 項目 | 理由 |
|------|------|------|
| P0 | **個案內容加密** | 法規要求，上線前必須完成 |
| P0 | **ViewModel 接通 Room** | 現在資料重啟就消失，基本功能不完整 |
| P1 | **`StudentDetailScreen` 接真實資料** | 目前顯示假資料，無法實際使用 |
| P1 | **拆分 `Student` 與 `CounselingProfile`** | 架構清晰，避免未來債務 |
| P1 | **Tags 改為獨立表** | 影響後續統計分析 |
| P2 | **晤談排程管理** | 輔導老師最常用的日常功能 |
| P2 | **危機事件記錄** | 法規要求的書面紀錄數位化 |
| P3 | **心情溫度計** | 預警系統，需要全班配合使用 |
| P3 | **導師協作備忘** | 需要多端（學生端、教師端）配合 |
| P4 | **稽核日誌** | 重要但可先做簡易版 |
| P4 | **校外資源庫** | 靜態資料，較易實作 |

---

## 六、資料表最終結構總覽

```
Student（學籍，唯讀）
  └── CounselingProfile（輔導狀態，1對1）
  └── CaseLog（晤談紀錄，1對多）
        └── CaseLogTag（標籤關聯，多對多）
  └── Appointment（排程，1對多）
  └── CrisisEvent（危機事件，1對多）
  └── MoodCheckResponse（週測回應，1對多）

CaseTag（標籤定義）
MoodCheckSession（週測場次）
CounselorTeacherNote（跨角色協作備忘）
ExternalResource（校外資源庫，靜態）
AuditLog（稽核日誌）
```

---

*本文件為輔導教師模組的完整應興應革分析，建議按 P0 → P4 順序開發*
