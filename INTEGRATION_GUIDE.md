# 整合說明

## 新增的兩個檔案

### FeatureData.kt
- 定義 `FeatureItem`、`FeatureGroup`、`BadgeType` 資料結構
- 包含 5 個角色的完整功能樹（共 ~60 個功能項）
- 提供 `getFeaturesForRole(role: String)` 取得對應清單

### FeatureUI.kt
- `RoleFeatureContent(role)` — 取代原本 DashboardScreen 中的空殼 Composable
- `FeatureRow(feature)` — 單一功能列，含 badge 徽章顯示

## DashboardScreen.kt 修改方式

將原本的 `when (role)` 區塊：
```kotlin
when (role) {
    "homeroom" -> HomeroomDashboard()
    "subject"  -> SubjectDashboard()
    ...
}
```
替換為：
```kotlin
RoleFeatureContent(role = role)
```

原本的 HomeroomDashboard()、SubjectDashboard() 等空殼 Composable 可以刪除。

## 未來擴充：點擊路由
FeatureRow 中的 onClick 目前為空，接 NavController 後：
```kotlin
onClick = { navController.navigate(feature.route) }
```
各 route 字串已預先定義在 FeatureItem 中。

## 功能數量彙整
| 角色     | 功能群組 | 功能項數 |
|----------|----------|----------|
| 導師     | 4 群組   | 14 項    |
| 科任教師 | 4 群組   | 14 項    |
| 行政教師 | 4 群組   | 13 項    |
| 輔導教師 | 4 群組   | 14 項    |
| 科主任   | 3 群組   | 12 項    |
| **合計** | **19**   | **67 項** |
