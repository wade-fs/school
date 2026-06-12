# 導師功能開發完整規劃 (Homeroom Teacher Module)

> 本文件旨在規劃導師（班導師）專屬的功能模組，核心聚焦於「班級日常管理」、「親師溝通」與「學生生活輔導」。

---

## 一、 開發階段規劃

### Sprint 1 — 班級日常營運 (核心功能)
- **1-A: 班級概況導覽 (Class Overview)**:
    *   顯示班級基本資訊（班號、教室、學生人數）。
    *   實作「快速聯絡清單」，可一鍵撥打家長電話或發送 Email。(✅ 完成)
- **1-B: 數位點名系統 (Attendance)**:
    *   提供視覺化介面。
    *   支援異常回報（遲到、曠課、病假、公假）。(✅ 完成)
- **1-C: 導師公告欄 (Class Bulletins)**:
    *   發布班級重要事項。
    *   追蹤公告內容。(✅ 完成)

### Sprint 2 — 親師生溝通樞紐
- **2-A: 數位聯絡簿 (Digital Contact Book)**:
    *   每日生活紀錄與作業叮嚀。
    *   家長線上簽閱功能。
- **2-B: 請假審核流程**:
    *   線上接收家長請假申請。

### Sprint 3 — 生活輔導與數據追蹤
- **3-A: 班級心情溫度計 (Class Wellbeing)**:
    *   彙整全班每日心情狀態分布圖。(✅ 完成)
- **3-B: 常規表現加減分 (Conduct Points)**:
    *   整潔、秩序、早自習表現紀錄。

### Sprint 4 — 綜合行政與分析
- **4-A: 成績與出缺席預警分析**:
    *   結合科任老師的成績資料，分析班級整體學習狀況。

---

## 二、 資料模型設計 (Entity Expansion)

### 點名紀錄 (AttendanceRecord)
```kotlin
data class AttendanceRecord(
    val id: Int,
    val studentId: String,
    val classId: String,
    val date: Long,
    val status: String // "出席", "遲到", "曠課", "病假", "事假", "公假"
)
```

### 聯絡簿公告 (ClassBulletin)
```kotlin
data class ClassBulletin(
    val id: Int,
    val classId: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val readCount: Int
)
```

---

## 三、 進度追蹤

| Sprint | 功能 | 狀態 | 實作日期 |
|--------|------|------|---------|
| 1-A | 班級概況導覽 | ✅ 完成 | 2026-06-12 |
| 1-B | 數位點名系統 | ✅ 完成 | 2026-06-12 |
| 1-C | 導師公告欄 | ✅ 完成 | 2026-06-12 |
| 2-A | 數位聯絡簿 | ⏳ 待開發 | - |
| 2-B | 請假審核流程 | ⏳ 待開發 | - |
| 3-A | 班級心情溫度計 | ✅ 完成 | 2026-06-12 |
| 3-B | 常規表現紀錄 | ⏳ 待開發 | - |
| 4-A | 數據預警分析 | ⏳ 待開發 | - |
