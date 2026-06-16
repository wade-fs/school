# 輔導教師平台 — 量表與調查功能完整規劃

> 專案：中學教師助手 App（`com.wade.school`）
> 更新日期：2026-06-16
> 狀態：規劃中，尚未實作
> 參考現有 entity：`MoodCheckSession`、`MoodCheckResponse`、`CounselingProfile`、`CrisisEvent`、`ExternalResource`

---

## 一、整體架構概念

```
輔導教師功能台
├── 量表施測中心          ← 本文件主軸
│   ├── 測驗模板庫        （選用 & 建立模板）
│   ├── 施測管理          （發布 / 收卷 / 班級統計）
│   └── 個人作答紀錄      （學生歷次結果）
├── 心理健康評估          ← 含高風險偵測
├── 高風險名單即時推送
└── 資源轉介連結庫
```

**設計原則：**
- 所有學生作答資料視同輔導個案資料，加密儲存（延用現有 `CaseLogCrypto`）
- 量表結果不直接顯示給學生，由輔導教師判讀後回饋
- 高風險觸發條件可在模板設定，自動推入 `CrisisEvent`
- 支援「教師選用現成模板」或「自訂題目」兩種模式

---

## 二、Room Entity 設計

### 2-1 量表模板 `AssessmentTemplate`

```kotlin
@Entity(tableName = "assessment_templates")
data class AssessmentTemplate(
    @PrimaryKey val templateId: String,          // UUID 或固定代號如 "MOOD_WEEKLY"
    val name: String,                             // 顯示名稱
    val category: TemplateCategory,              // 見下方 enum
    val description: String,                     // 量表說明
    val isBuiltIn: Boolean = true,               // 內建 vs. 教師自訂
    val isActive: Boolean = true,
    val estimatedMinutes: Int = 5,               // 預估作答時間
    val frequencyHint: String? = null,           // "每週一次" / "學期初/末"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TemplateCategory(val label: String) {
    MOOD("情緒監測"),
    MENTAL_HEALTH("心理健康評估"),
    CAREER("生涯與職涯"),
    GENDER_EQUALITY("性別平等"),
    INTERPERSONAL("人際與愛情"),
    CAMPUS_SAFETY("校園安全"),
    CUSTOM("自訂量表")
}
```

### 2-2 量表題目 `AssessmentQuestion`

```kotlin
@Entity(tableName = "assessment_questions")
data class AssessmentQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateId: String,                      // FK → AssessmentTemplate
    val order: Int,                              // 題序
    val text: String,                            // 題目文字
    val type: QuestionType,
    val options: String? = null,                 // JSON: ["從不","偶爾","經常","總是"]
    val reverseScore: Boolean = false,           // 反向計分
    val minVal: Int = 1,                         // 量尺最小值（Likert / 溫度計）
    val maxVal: Int = 5,                         // 量尺最大值
    val minLabel: String? = null,               // 例如「完全不同意」
    val maxLabel: String? = null,               // 例如「非常同意」
    val riskTrigger: Boolean = false,           // 此題答到特定值時觸發高風險警示
    val riskThreshold: Int? = null,             // 觸發門檻值
    val riskCondition: String = "GTE"           // GTE=大於等於 / LTE=小於等於
)

enum class QuestionType {
    LIKERT,       // 1-5 / 1-4 Likert 量尺
    SCALE,        // 連續數字量尺（心情溫度計 1-10）
    SINGLE,       // 單選
    MULTI,        // 多選
    TEXT,         // 開放文字
    YES_NO        // 是 / 否
}
```

### 2-3 施測場次 `AssessmentSession`

```kotlin
// 延伸現有 MoodCheckSession，統一所有量表場次
@Entity(tableName = "assessment_sessions")
data class AssessmentSession(
    @PrimaryKey val sessionId: String,           // UUID
    val templateId: String,
    val targetClass: String,                     // 班級，"全年級" 或單班
    val conductedBy: String,                     // 輔導教師
    val scheduledAt: Long,                       // 預定施測時間
    val closedAt: Long? = null,                  // 截止時間（null=尚未截止）
    val status: SessionStatus = SessionStatus.OPEN,
    val note: String? = null
)

enum class SessionStatus { DRAFT, OPEN, CLOSED, ARCHIVED }
```

### 2-4 學生作答 `AssessmentResponse`

```kotlin
// 延伸現有 MoodCheckResponse
@Entity(tableName = "assessment_responses",
    indices = [Index(value = ["sessionId", "studentId"], unique = true)])
data class AssessmentResponse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val studentId: String,
    val answersJson: String,                     // JSON: {"q1":3,"q2":4,...}（加密儲存）
    val totalScore: Int? = null,                 // 計算後總分（部分量表適用）
    val riskFlagged: Boolean = false,            // 是否觸發高風險
    val completedAt: Long = System.currentTimeMillis(),
    val counselorNote: String? = null           // 輔導老師批注
)
```

### 2-5 高風險推送紀錄 `RiskAlert`

```kotlin
@Entity(tableName = "risk_alerts")
data class RiskAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val sourceType: String,            // "ASSESSMENT" / "MANUAL" / "CRISIS"
    val sourceId: String,              // sessionId 或 crisisEventId
    val triggeredAt: Long = System.currentTimeMillis(),
    val severity: AlertSeverity,
    val reason: String,                // 觸發原因描述
    val isRead: Boolean = false,
    val handledAt: Long? = null,
    val handledBy: String? = null,
    val handledNote: String? = null
)

enum class AlertSeverity { WATCH, WARNING, URGENT }
```

---

## 三、內建量表模板詳細規劃

---

### 3-1 學生心情溫度計（每日 / 每週）

**Template ID：** `MOOD_DAILY` / `MOOD_WEEKLY`
**分類：** `MOOD`
**預估時間：** 1-2 分鐘
**施測頻率：** 每日（導師課前）/ 每週一次

| # | 題目 | 類型 | 量尺 | 高風險觸發 |
|---|------|------|------|-----------|
| 1 | 現在的心情分數是幾分？ | SCALE | 1-10 | ≤ 3 → URGENT |
| 2 | 今天的精神狀態如何？ | LIKERT | 1-5 | — |
| 3 | 今天有沒有發生讓你特別開心或難過的事？ | TEXT | — | — |
| 4 | 有沒有任何事情讓你擔心或害怕？ | YES_NO | — | 是 → WATCH |
| 5 | 如果有任何事想跟老師說，可以寫在這裡。（選填） | TEXT | — | — |

**計分方式：**
- 第 1 題為主指標
- 1-3 分：立即推送 URGENT 警示給輔導教師
- 4-5 分：推送 WARNING（連續兩週）
- 6-10 分：正常，可視班級平均趨勢分析

**班級統計視圖：**
- 熱力圖（週次 × 學生，顏色=平均分）
- 折線圖（班級平均趨勢，可比較不同班）
- 即時名單：本週分數 ≤ 4 的學生清單

---

### 3-2 生涯規劃與職涯興趣量表

**Template ID：** `CAREER_INTEREST`
**分類：** `CAREER`
**預估時間：** 15-20 分鐘
**施測頻率：** 學期初、高一新生、申請志願前

#### 模組 A：Holland 職業興趣類型（簡版 30 題）

參考 Holland 六邊形（RIASEC），每型 5 題，Likert 1-5：

| Holland 類型 | 中文 | 代表職業方向 | 範例題目 |
|-------------|------|------------|---------|
| R（實用型） | Realistic | 工程、農業、技職 | 「我喜歡動手操作機械或工具」 |
| I（研究型） | Investigative | 科學、研究、醫學 | 「我喜歡探索事物的原理和規律」 |
| A（藝術型） | Artistic | 設計、音樂、文學 | 「我喜歡透過藝術表達自己的想法」 |
| S（社會型） | Social | 教育、社工、醫護 | 「我喜歡幫助別人解決問題」 |
| E（企業型） | Enterprising | 商業、管理、法律 | 「我喜歡說服或影響他人」 |
| C（傳統型） | Conventional | 行政、財務、資料 | 「我喜歡依照程序有條理地完成工作」 |

**計分輸出：**
- 六邊形雷達圖（學生可看）
- 前兩高類型 → 推薦科系方向列表
- 教師可備注「與學業成就/家庭期望的落差」

#### 模組 B：學習風格自評（8 題）

| # | 題目 | 類型 |
|---|------|------|
| 1 | 我比較喜歡透過閱讀理解新知識 | LIKERT |
| 2 | 我比較喜歡聽老師講解再思考 | LIKERT |
| 3 | 我需要實際操作才能真正學會 | LIKERT |
| 4 | 我喜歡在安靜環境下一個人讀書 | LIKERT |
| 5 | 我在小組討論中學得比較好 | LIKERT |
| 6 | 我喜歡有明確目標和時間表的學習方式 | LIKERT |
| 7 | 我在壓力下反而表現得更好 | LIKERT |
| 8 | 我容易因外在刺激分心 | LIKERT |

#### 模組 C：生涯成熟度（6 題，是/否）

| # | 題目 |
|---|------|
| 1 | 我對自己未來想從事的工作有初步方向 |
| 2 | 我了解自己有興趣的大學科系需要哪些能力 |
| 3 | 我曾主動蒐集過某個職業的相關資訊 |
| 4 | 我有認識的大人從事我有興趣的職業 |
| 5 | 我了解台灣大學入學的管道（繁星/申請/分發） |
| 6 | 我知道自己目前的成績大概能申請什麼學校 |

**輸出報告欄位（教師側）：**
- 主要 Holland 類型 + 圖表
- 學習風格傾向（視覺/聽覺/動覺）
- 生涯成熟度分數（0-6）
- 建議輔導介入方向（自動產生文字）

---

### 3-3 性別平等調查

**Template ID：** `GENDER_EQUALITY`
**分類：** `GENDER_EQUALITY`
**預估時間：** 10 分鐘
**施測頻率：** 每學期一次，全校或年級

#### 模組 A：性別刻板印象認知（10 題，Likert 1-5）

> 題目反映「同意程度」，反向計分題以 * 標示

| # | 題目 | 反向 |
|---|------|------|
| 1 | 男生理科比女生強是天生的 | ✕ |
| 2 | 家事應該由男女雙方平等分擔 | * |
| 3 | 女生比較不適合擔任領導者 | ✕ |
| 4 | 男生哭泣是懦弱的表現 | ✕ |
| 5 | 任何人都有權利選擇自己的穿著，不應受到批評 | * |
| 6 | 男生在感情中主動追求才正常 | ✕ |
| 7 | 女生說「不要」有時候其實是「要」 | ✕（高風險偵測題）|
| 8 | 不同性別的人應有平等的工作機會 | * |
| 9 | LGBTQ+ 是一種需要被「矯正」的狀態 | ✕ |
| 10 | 我覺得學校對所有性別的學生都公平對待 | * |

#### 模組 B：校園性別友善環境感受（8 題，Likert）

| # | 題目 |
|---|------|
| 1 | 我在校園裡感到安全 |
| 2 | 學校廁所 / 更衣室讓我感到隱私受保護 |
| 3 | 我曾目睹或聽說過同學間有性騷擾言語 |
| 4 | 老師對待不同性別學生的方式是公平的 |
| 5 | 如果我遇到性別相關困擾，我知道可以去哪裡求助 |
| 6 | 我覺得可以在學校公開表達自己的性別認同 |
| 7 | 學校的性平課程對我有實質幫助 |
| 8 | 我曾因性別原因在學校受到不公平對待 |

#### 模組 C：個人經驗（6 題，敏感，匿名處理）

| # | 題目 | 類型 |
|---|------|------|
| 1 | 我曾在校園內受到帶有性別意味的嘲弄或取笑 | YES_NO |
| 2 | 我曾在網路上收到讓我不舒服的性別相關訊息 | YES_NO |
| 3 | 如果第 1 或 2 題回答「是」，請簡單描述（選填） | TEXT |
| 4 | 我是否需要老師協助或諮詢？ | YES_NO |
| 5 | 我希望學校加強哪方面的性平教育？（可複選） | MULTI（6 選項）|

**高風險偵測：**
- 第 A 組第 7 題得 4-5 分 → 加入觀察名單（潛在加害者認知）
- 模組 C 第 1/2 題回答「是」且第 4 題「是」→ 立即推送 WARNING 給輔導教師

---

### 3-4 校園人際關係 / 愛情調查

**Template ID：** `INTERPERSONAL` / `ROMANCE`
**分類：** `INTERPERSONAL`
**預估時間：** 10-15 分鐘
**施測頻率：** 學期初 + 期末

#### 模組 A：人際關係品質（10 題，Likert 1-5）

| # | 題目 |
|---|------|
| 1 | 在班上，我有至少一位真正了解我的朋友 |
| 2 | 我覺得自己是班上的一份子 |
| 3 | 當我遇到困難，班上有同學願意幫助我 |
| 4 | 我在班上容易與同學發生衝突 |
| 5 | 我曾被同學排擠或孤立 |
| 6 | 我害怕在課堂上表達意見，擔心被同學嘲笑 |
| 7 | 我在網路上（通訊軟體/社群媒體）與同學的互動是正向的 |
| 8 | 我知道如何與意見不同的人好好溝通 |
| 9 | 我在需要幫助時能主動向師長求助 |
| 10 | 整體來說，我對目前的人際關係感到滿意 |

**計分解讀：**

| 總分 | 解讀 | 建議行動 |
|------|------|---------|
| 40-50 | 人際支持良好 | 維持，可作為同儕支持員培訓候選 |
| 28-39 | 一般，有部分孤立感 | 追蹤第 1、2、10 題 |
| 15-27 | 人際支持薄弱 | 推送 WARNING，安排個別晤談 |
| < 15 | 嚴重孤立或霸凌風險 | 推送 URGENT，啟動危機處理 |

#### 模組 B：霸凌偵測（8 題）

| # | 題目 | 類型 | 高風險觸發 |
|---|------|------|-----------|
| 1 | 我曾在學校被同學持續嘲笑、羞辱或取綽號 | YES_NO | 是 → WATCH |
| 2 | 我曾被同學故意推打、踢踹或拿走我的東西 | YES_NO | 是 → WARNING |
| 3 | 我曾被同學在網路上公開嘲笑或散佈我的隱私 | YES_NO | 是 → WARNING |
| 4 | 上述情況發生時，大概多頻繁？ | SINGLE（4 選項）| 每天/每週 → URGENT |
| 5 | 我曾告訴師長或家長這件事嗎？ | YES_NO | — |
| 6 | 告訴大人之後，情況有改善嗎？ | SINGLE（3 選項）| — |
| 7 | 我是否也曾對其他同學做過類似的事？ | YES_NO | 是 → WATCH（加害者）|
| 8 | 我希望老師幫我處理這件事嗎？ | YES_NO | — |

#### 模組 C：愛情關係健康度（10 題，僅高中部施測）

> 適用對象：高一至高三，含感情經驗與健康交往觀念

| # | 題目 | 反向 |
|---|------|------|
| 1 | 健康的感情關係中，兩人應互相尊重彼此的邊界 | * |
| 2 | 如果對方不回訊息，我有權利一直傳訊息要對方解釋 | ✕ |
| 3 | 感情中的嫉妒是愛的表現，是正常的 | ✕ |
| 4 | 如果我不喜歡交往，對方應該要接受我的拒絕 | * |
| 5 | 我知道什麼是「情感操控（PUA）」 | * |
| 6 | 交往中若感到不舒服，我有勇氣說出來 | * |
| 7 | 如果我喜歡一個人，對方「不要」可能只是害羞 | ✕（高風險）|
| 8 | 分手後若持續騷擾對方，這是正確的行為 | ✕（高風險）|
| 9 | 我目前的感情狀態讓我感到快樂和安全 | * |
| 10 | 如果感情遇到困擾，我願意尋求輔導老師的協助 | * |

---

### 3-5 心理健康評估工具

**Template ID：** `PHQ9_TW` / `GAD7_TW` / `DASS21_TW` / `STRESS_SCHOOL`
**分類：** `MENTAL_HEALTH`
**預估時間：** 5-15 分鐘
**施測頻率：** 學期初/末 + 個別評估需求

#### 工具 A：PHQ-9（憂鬱症篩檢，台灣版 9 題）

> 過去兩週內，下列問題讓你感到困擾的頻率（0=從未 / 1=幾天 / 2=超過一半 / 3=幾乎每天）

| # | 題目 | 高風險偵測 |
|---|------|-----------|
| 1 | 對事情提不起勁或沒有樂趣 | — |
| 2 | 感到情緒低落、沮喪或絕望 | — |
| 3 | 入睡困難、睡眠不穩或睡太多 | — |
| 4 | 感到疲倦或沒有活力 | — |
| 5 | 食慾不振或吃太多 | — |
| 6 | 對自己感到不好，覺得自己是個失敗者或讓家人失望 | — |
| 7 | 難以專注在事情上，例如看報紙或看電視 | — |
| 8 | 動作或說話速度變慢，或者坐立難安、動來動去 | — |
| 9 | **有不如死掉或傷害自己的念頭** | ≥ 1 → 立即 URGENT |

**計分解讀：**

| 分數 | 憂鬱程度 | App 行動 |
|------|---------|---------|
| 0-4 | 無憂鬱 | 記錄存檔 |
| 5-9 | 輕度 | 推送 WATCH，建議追蹤 |
| 10-14 | 中度 | 推送 WARNING，排個別晤談 |
| 15-19 | 中重度 | 推送 URGENT，啟動危機程序 |
| 20-27 | 重度 | 推送 URGENT + 通知家長建議 |
| 第 9 題 > 0 | 任何分數 | 立即 URGENT（不依總分）|

#### 工具 B：GAD-7（焦慮篩檢，7 題）

> 同 PHQ-9 計分方式（0-3），評估過去兩週

| # | 題目 |
|---|------|
| 1 | 感覺緊張、焦慮或者急躁 |
| 2 | 沒有辦法停止或控制擔憂 |
| 3 | 對各種事情過分擔憂 |
| 4 | 難以放鬆 |
| 5 | 坐立難安，很難靜下來 |
| 6 | 容易心煩或易怒 |
| 7 | 感覺有什麼可怕的事情要發生 |

**計分：** 5-9 輕度 / 10-14 中度 / ≥ 15 重度

#### 工具 C：學業壓力量表（校本自編，12 題）

| # | 題目 |
|---|------|
| 1 | 我擔心考試成績不夠好 |
| 2 | 我感覺功課的量超過我能負荷的範圍 |
| 3 | 我害怕讓父母或老師失望 |
| 4 | 我因為課業壓力而影響睡眠 |
| 5 | 我會為了讀書放棄休閒或社交活動 |
| 6 | 我覺得自己的成績決定了我的未來 |
| 7 | 我擔心升學選擇（科系/學校）的問題 |
| 8 | 當考試表現不佳，我會有很長一段時間情緒低落 |
| 9 | 我覺得學校的進度對我來說太快 |
| 10 | 我在意排名，總是和同學比較 |
| 11 | 我曾為了成績而有作弊的念頭 |
| 12 | 整體來說，課業壓力讓我感到喘不過氣 |

#### 工具 D：PHQ-9 + GAD-7 合併施測（建議學期初全校篩檢）

- 兩份量表合在一個 Session 施測（16 題，約 8 分鐘）
- 結果自動分類為四象限：
  - 情緒穩定（低憂鬱 + 低焦慮）
  - 焦慮傾向（低憂鬱 + 高焦慮）
  - 憂鬱傾向（高憂鬱 + 低焦慮）
  - 雙重高風險（高憂鬱 + 高焦慮）→ 優先個別晤談

---

## 四、施測管理流程

```
1. 教師選擇模板
      ↓
2. 設定施測對象（全班 / 多班 / 個別學生）
      ↓
3. 設定開放時間（立即 / 排程）
      ↓
4. 學生 App 收到通知 → 作答（僅學生視角）
      ↓
5. 即時收卷統計（已答 N/M 人）
      ↓
6. 高風險自動偵測 → 推送 RiskAlert 給輔導教師
      ↓
7. 教師批次查看結果 → 標注個別學生 → 安排晤談
      ↓
8. Session 結案歸檔 → 進入 CounselingProfile 歷史
```

---

## 五、高風險名單即時推送規格

### 推送來源整合

| 來源 | 觸發條件 | 嚴重等級 |
|------|---------|---------|
| 心情溫度計 | 分數 ≤ 3（當日）| URGENT |
| 心情溫度計 | 連續兩週平均 ≤ 5 | WARNING |
| PHQ-9 第 9 題 | 任何大於 0 的作答 | URGENT |
| PHQ-9 總分 | ≥ 15 分 | URGENT |
| 人際量表 | 總分 < 15 | URGENT |
| 霸凌偵測 | 每天/每週受害 | URGENT |
| 愛情量表 | 第 7/8 題高分 | WARNING（觀念偏差）|
| 性平調查 | 模組 C 第 1/2 題「是」且求助 | WARNING |
| 教師手動 | 任何時候 | 可選 |

### 推送內容設計（App 通知格式）

```
【高風險提醒】
學生：[姓名縮寫]（[班級]）
來源：PHQ-9 第 9 題作答
時間：2026-06-16 10:32
等級：🔴 緊急
━━━━━━━━━━━━━━━━━━
請儘快聯繫該生確認安全狀況。
[查看詳情]  [記錄已處理]
```

### `RiskAlert` DAO 方法

```kotlin
// 建議加入 CounselorDao
@Query("SELECT * FROM risk_alerts WHERE isRead = 0 ORDER BY triggeredAt DESC")
fun getUnreadAlerts(): Flow<List<RiskAlert>>

@Query("SELECT * FROM risk_alerts WHERE studentId = :studentId ORDER BY triggeredAt DESC")
fun getAlertsByStudent(studentId: String): Flow<List<RiskAlert>>

@Query("UPDATE risk_alerts SET isRead = 1 WHERE id = :alertId")
suspend fun markAsRead(alertId: Int)

@Query("UPDATE risk_alerts SET handledAt = :time, handledBy = :by, handledNote = :note WHERE id = :alertId")
suspend fun markAsHandled(alertId: Int, time: Long, by: String, note: String)

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertAlert(alert: RiskAlert)
```

---

## 六、資源轉介連結庫

延伸現有 `ExternalResource` entity，補充下列類別和預設資料：

### 6-1 分類擴充

```kotlin
// 建議把現有 ExternalResource.type 改為 enum
enum class ResourceType(val label: String) {
    HOTLINE_24H("24小時危機專線"),
    COUNSELING("諮商/心理治療"),
    PSYCHIATRY("精神科醫療"),
    SOCIAL_WORK("社福與家庭支持"),
    LEGAL("法律資源"),
    GENDER("性別/LGBTQ+ 友善"),
    CAREER("生涯輔導資源"),
    SUBSTANCE("成癮/物質濫用"),
    DOMESTIC_VIOLENCE("家暴/性侵害"),
    BULLYING("霸凌通報")
}
```

### 6-2 預設全國資源

| 名稱 | 電話 | 類別 | 24H |
|------|------|------|-----|
| 安心專線 | **1925** | HOTLINE_24H | ✓ |
| 自殺防治專線 | **1925** | HOTLINE_24H | ✓ |
| 兒童保護專線 | **113** | HOTLINE_24H | ✓ |
| 性侵/家暴通報 | **113** | DOMESTIC_VIOLENCE | ✓ |
| 少年專線 | **0800-001-769** | HOTLINE_24H | ✓ |
| 張老師專線 | **1980** | COUNSELING | ✓ |
| iWIN 網路內容防護 | **0800-001-070** | BULLYING | — |
| 生命線 | **1995** | HOTLINE_24H | ✓ |
| 法律扶助基金會 | **(02)412-8518** | LEGAL | — |
| 同志諮詢熱線 | **(02)2392-1970** | GENDER | — |

### 6-3 轉介連結庫 UI 功能規劃

- **搜尋欄**：依名稱或類別即時過濾
- **分頁 Tab**：緊急 / 諮商 / 醫療 / 法律 / 社福
- **一鍵撥號**：點擊電話號碼直接 `Intent.ACTION_DIAL`
- **轉介紀錄**：記錄「哪位學生被轉介到哪個單位、日期、負責教師」
  - 對應建議新增 entity `ReferralRecord`
- **教師自訂**：可新增學校所在縣市的在地資源
- **QR Code 分享**：將資源清單生成 QR 供在輔導公告欄張貼

### 6-4 `ReferralRecord` Entity

```kotlin
@Entity(tableName = "referral_records")
data class ReferralRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val resourceId: Int,              // FK → ExternalResource
    val resourceName: String,         // 冗餘存一份，防資源被刪
    val referredAt: Long = System.currentTimeMillis(),
    val referredBy: String,           // 輔導教師
    val reason: String,               // 轉介原因
    val followUpDate: Long? = null,
    val outcome: String? = null       // 後續結果記錄
)
```

---

## 七、`counselingFeatures` FeatureData 更新

```kotlin
private val counselingFeatures = listOf(
    FeatureGroup("量表施測中心", listOf(
        FeatureItem("測驗模板庫",   "選用或建立量表模板",        "library_books",  null,     BadgeType.NONE,    "counseling/templates"),
        FeatureItem("發起施測",     "對班級發布量表",            "send",           null,     BadgeType.NONE,    "counseling/sessions/new"),
        FeatureItem("進行中施測",   "查看作答進度與即時結果",    "analytics",      "2",      BadgeType.INFO,    "counseling/sessions/active"),
        FeatureItem("歷史結果",     "查閱各場次施測報告",        "history",        null,     BadgeType.NONE,    "counseling/sessions/history")
    )),
    FeatureGroup("高風險監控", listOf(
        FeatureItem("風險警示",     "高風險學生即時通知",        "notification_important", "3", BadgeType.URGENT, "counseling/alerts"),
        FeatureItem("心情溫度計",   "全班情緒週測與趨勢圖",     "thermostat",     "週測進行中", BadgeType.INFO, "counseling/mood"),
        FeatureItem("高風險名單",   "目前需追蹤學生清單",        "warning",        "5",      BadgeType.WARNING,  "counseling/watchlist")
    )),
    FeatureGroup("個案管理", listOf(
        FeatureItem("加密個案紀錄", "需生物辨識解鎖",            "security",       "1",      BadgeType.WARNING,  "counseling/cases"),
        FeatureItem("晤談紀錄",     "新增與查閱個別晤談",        "record_voice_over", null,  BadgeType.NONE,    "counseling/sessions/talk"),
        FeatureItem("危機事件",     "通報與後續追蹤",            "crisis_alert",   null,     BadgeType.NONE,    "counseling/crisis")
    )),
    FeatureGroup("資源轉介", listOf(
        FeatureItem("轉介連結庫",   "全國 & 在地輔導資源",       "hub",            null,     BadgeType.NONE,    "counseling/resources"),
        FeatureItem("轉介紀錄",     "學生轉介歷程追蹤",          "assignment_turned_in", null, BadgeType.NONE, "counseling/referrals"),
        FeatureItem("緊急聯絡",     "一鍵撥打危機專線",          "emergency",      null,     BadgeType.URGENT,  "counseling/emergency")
    ))
)
```

---

## 八、實作優先順序

### Phase A（最高優先，直接影響學生安全）

1. **`RiskAlert` entity + DAO + 推送邏輯** — 高風險偵測是整個量表系統的核心
2. **`MOOD_WEEKLY` 心情溫度計** — 現有 `MoodCheckScreen` 已有基礎，擴充高風險判斷即可
3. **PHQ-9 施測** — 單一量表，14 題，影響最大

### Phase B（次要優先，學期初使用）

4. **AssessmentTemplate + Question + Session + Response entity** — 通用框架建好，後面所有量表都能套用
5. **`CAREER_INTEREST` 生涯量表** — 高一新生最常用
6. **資源轉介連結庫 + 撥號** — 現有 `ExternalResourceScreen` 擴充

### Phase C（學期中補充）

7. `GENDER_EQUALITY` 性平調查
8. `INTERPERSONAL` 人際量表 + 霸凌偵測
9. `ROMANCE` 愛情關係量表（高中部限定）
10. `GAD7` + `STRESS_SCHOOL` 焦慮 / 學業壓力量表

---

## 九、相關檔案索引

| 需新增/修改 | 路徑 | 說明 |
|------------|------|------|
| `AssessmentTemplate.kt` | `entity/` | 量表模板 entity |
| `AssessmentQuestion.kt` | `entity/` | 題目 entity |
| `AssessmentSession.kt` | `entity/` | 施測場次（取代 MoodCheckSession）|
| `AssessmentResponse.kt` | `entity/` | 作答記錄（取代 MoodCheckResponse）|
| `RiskAlert.kt` | `entity/` | 高風險警示 |
| `ReferralRecord.kt` | `entity/` | 轉介紀錄 |
| `CounselorDao.kt` | `dao/` | 新增所有量表相關 DAO |
| `AppDatabase.kt` | `data/local/` | 加入新 entity，version +1 |
| `AssessmentViewModel.kt` | `ui/screens/` | 量表施測 ViewModel |
| `AssessmentScreen.kt` | `ui/screens/` | 施測主畫面 |
| `RiskAlertScreen.kt` | `ui/screens/` | 高風險警示畫面 |
| `FeatureData.kt` | `ui/data/` | 更新 `counselingFeatures` |
| `MoodCheckScreen.kt` | `ui/screens/` | 整合進 AssessmentScreen |
