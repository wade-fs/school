# 台灣高中教師 App 框架說明 (Taiwan High School Teacher App Framework)

這是一個基於 **Jetpack Compose (Kotlin)** 開發的 Android 應用程式原型，旨在呈現「以角色為核心」的設計理念。

## 已實作功能亮點

1.  **多角色入口 (Role Selector)**:
    - 啟動後可選擇：導師、科任教師、行政、輔導、科主任。
    - 每個角色都有專屬的圖示與功能描述。
2.  **動態工作台 (Dynamic Dashboard)**:
    - 根據選擇的角色，呈現不同的功能模組。
    - **導師**: 側重「快速點名」與「家長聯繫」。
    - **輔導**: 強調「個案紀錄加密」與「心情預警」。
    - **科任**: 整合「108 課綱教材」與「AI 批改」。
3.  **Material 3 現代化介面**:
    - 使用 Google 最新設計規範。
    - 專為台灣教育場景設計的配色（專業藍/成長綠）。
4.  **客製化校徽圖標 (Custom App Icon)**:
    - 採用「新北市立清水高級中學」校徽進行設計。
    - 支援 Android 8.0+ 的自定義圖標 (Adaptive Icon)，包含前景與背景圖層。

## 如何運行此專案

1.  **環境要求**:
    - 安裝最新版 **Android Studio (Hedgehog 或更高版本)**。
    - **JDK 17** 或更高。
2.  **載入專案**:
    - 直接在 Android Studio 中開啟此目錄（`school`）。
    - Gradle 會自動同步依賴項。
3.  **運行**:
    - 點擊 **Run** 按鈕部署至 Android 模擬器或實體手機。

## 後續開發建議 (Roadmap)

- [ ] **串接 SchoolBO/校務系統**: 實作 API 層以同步學生名單與課表。
- [ ] **離線點名機制**: 整合 Room Database 支援網路不穩時的資料暫存。
- [ ] **AI 助理整合**: 加入 Gemini API 輔助教師撰寫教案或分析作業。
- [ ] **生物辨識登入**: 輔導個案紀錄區塊應強制指紋或臉部辨識才能進入。

## 專案結構圖

```
app/src/main/java/com/taiwan/teacherapp/
├── MainActivity.kt         # 導航核心邏輯
├── ui/
│   ├── theme/              # 配色、字體與主題定義
│   └── screens/
│       ├── RoleSelector.kt # 角色選擇介面
│       └── Dashboard.kt    # 各角色動態工作台
```
