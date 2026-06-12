# 開發歷程與更新日誌 (Project Development History)

本文件紀錄了專案從開發環境修復、功能實作到 CI/CD 建置的完整過程。

## 2026-06-12：基礎修復與核心模組實作

### 🔧 基礎建設與環境修復
- **KSP 編譯錯誤修復**: 解決了 `java.lang.IllegalArgumentException: 25.0.3` 衝突。
  - 將 Kotlin 升級至 `2.0.20`
  - 將 KSP 升級至 `2.0.20-1.0.24`
- **Compose 語法修正**: 修復了 `DashboardScreen.kt` 中因缺少 Import 導致的 `Unresolved reference: mutableStateOf` 錯誤。
- **CI/CD 自動化建置**: 
  - 新增 `android-ci.yml`: 實現每次 Push/PR 的自動編譯檢查。
  - 優化 `release.yml`: 支援使用 GitHub Secrets 進行自動化正式版 APK 簽署與發布。

### 🎓 輔導教師模組 (Counselor Module Sprints)

#### Sprint 3: 危機事件通報
- **功能**: 支援記錄自傷、霸凌、家暴等重大事件。
- **邏輯**: 「緊急」事件通報後，自動將該學生風險等級提升至 **High**。
- **UI**: 實作 `CrisisReportDialog` 通報表單與學生詳情頁的紅色危機歷史列表。

#### Sprint 4: 導師協作機制
- **功能**: 輔導老師可發送「去識別化」備忘錄給班級導師（如：請多關心、避免點名）。
- **UI**: 實作導師通知對話框與歷史備忘追蹤。

#### Sprint 5: 校外資源庫
- **資料**: 資料庫首次啟動時自動預填「安心專線 1925」、「生命線 1995」、「113」等緊急資源。
- **UI**: 實作 `ExternalResourceScreen`，支援一鍵撥打功能。

#### Sprint 6: 安全稽核系統 (Audit Log)
- **機制**: 實作全自動後台稽核日誌。
- **涵蓋範圍**: 包含個案檢視、紀錄建立、狀態更新、CSV 匯入等所有敏感操作。

### 🏫 學校資料動態整合
- **教育部資料對接**: 整合 Ktor Client 從教育部官網抓取最新全台高中職 CSV 資料。
- **智慧 Fallback 機制**: 自動計算學年度（每年 8/1 切換），若最新學年度資料尚未上架，自動回退至前一年度，確保服務不中斷。
- **學校挑選器**: 在設定對話框中加入搜尋與選取功能，取代手動輸入校名。

### 🎨 UI/UX 優化
- **首頁重構**: 將 `CounselingDashboard` 修改為全 `LazyColumn` 結構，解決長列表無法捲動的問題。
- **吸頂搜尋列**: 實作 `stickyHeader` 讓搜尋框在捲動時能固定在頂部。
- **學年自動化**: 移除冗餘的「全體升級」按鈕，改由系統自動判定。
