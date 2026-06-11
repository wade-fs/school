# 校園 App 資料架構設計筆記

> 主題：學生、班級、家長、教師、輔導教師之間的關聯設計與建立流程

---

## 一、核心設計思想

在動手建表之前，先釐清幾個根本問題：

### 「帳號」與「身份」要分開

系統裡有兩種不同的概念容易混淆：

- **帳號（USER）**：登入用，代表一個真實的人，有手機號、密碼
- **身份（STUDENT / TEACHER 角色）**：這個人在學校裡扮演的角色

分開的理由：
- 同一個學生畢業後成為校友，帳號不應消失，但學籍身份已結束
- 教師與學生都是人，共用同一張 USER 表，不需要兩套登入系統
- 未來若學生跨校選修，同一個 `user_id` 可以對應兩所學校的 STUDENT 記錄

### 關聯關係都是「多對多」

幾乎所有關係都不是單純一對一：

- 一個學生可以有多位家長（父、母、監護人）
- 一位家長可以有多個孩子（兄弟姊妹都在同校）
- 一個班級有多位科任教師；一位科任教師教多個班
- 一位輔導教師輔導多個學生；一個學生可能有多位輔導教師協同

這些都需要中介關聯表（junction table）來處理，不能用單一外鍵欄位了事。

### 歷史資料永不刪除

學年結束後，不刪舊資料，而是透過 `academic_year_id` 區隔。  
原因：
- 出缺席、成績、輔導紀錄有法規保存義務
- 家長、學生日後可能需要查詢歷年紀錄
- 升學年只需更新 `student.class_id`，舊資料自動留存

---

## 二、資料表設計

### 頂層：學校與學年

```sql
SCHOOL
  id            UUID  PK
  name          TEXT            -- 學校全名
  short_name    TEXT            -- 簡稱（顯示用）
  city          TEXT
  created_at    TIMESTAMP

ACADEMIC_YEAR
  id            UUID  PK
  school_id     UUID  FK → SCHOOL
  label         TEXT            -- 例如 "113學年度"
  start_date    DATE
  end_date      DATE
  is_current    BOOLEAN         -- 只有一筆為 true
```

`is_current` 用來讓 App 知道現在是哪個學年，不用每次都查日期範圍。

---

### 班級

```sql
CLASS
  id                    UUID  PK
  academic_year_id      UUID  FK → ACADEMIC_YEAR
  grade                 INTEGER        -- 1、2、3（年級）
  name                  TEXT           -- 例如 "忠班"、"甲"
  homeroom_teacher_id   UUID  FK → USER  NULL  -- 可晚點指派
  created_at            TIMESTAMP
```

班級在每個學年都是新建的，不是跨學年共用。  
`homeroom_teacher_id` 允許 NULL，因為可能開學前幾天才確定導師。

---

### 使用者帳號（共用表）

```sql
USER
  id              UUID  PK
  school_id       UUID  FK → SCHOOL
  name            TEXT
  phone           TEXT  UNIQUE        -- 登入帳號
  hashed_password TEXT
  role            ENUM  (teacher, student, parent, admin)
  is_active       BOOLEAN  DEFAULT true
  created_at      TIMESTAMP
  last_login_at   TIMESTAMP
```

`role` 決定登入後看到哪個端的介面，也決定 JWT token 的內容。  
`is_active` 用來停用帳號，不刪資料。

---

### 學生學籍

```sql
STUDENT
  id            UUID  PK
  user_id       UUID  FK → USER       -- 對應的登入帳號
  class_id      UUID  FK → CLASS      -- 目前所在班級
  student_no    TEXT                  -- 學號（學校內唯一）
  seat_no       INTEGER               -- 座號
  enrolled_at   DATE                  -- 入學日
  graduated_at  DATE  NULL            -- 畢業日（NULL 表示在學中）
```

升學年時，只更新 `class_id` 到新班級，`student_no` 不變，歷史出缺席、成績都保留。

---

### 家長與學生的關聯（多對多）

```sql
PARENT_STUDENT
  id              UUID  PK
  parent_user_id  UUID  FK → USER       -- 家長帳號
  student_id      UUID  FK → STUDENT
  relation        ENUM  (father, mother, guardian, other)
  is_primary      BOOLEAN  DEFAULT false  -- 主要聯絡人（緊急時優先通知）
  bound_at        TIMESTAMP              -- 綁定時間（供稽核）
  bound_method    ENUM  (invite_link, verification_code, admin_manual)
```

`is_primary` 很重要：當有緊急通知時，系統只推給主要聯絡人，避免同一件事父母各收一則重複通知，但也可以設定都收。

---

### 科任教師與班級的關聯（多對多）

```sql
TEACHER_SUBJECT_CLASS
  id                UUID  PK
  teacher_user_id   UUID  FK → USER
  class_id          UUID  FK → CLASS
  subject           TEXT              -- 例如 "數學"、"物理"
  academic_year_id  UUID  FK → ACADEMIC_YEAR
```

同一位教師同一學年可以教多個班多個科目，都是獨立的記錄。

---

### 輔導教師與學生的關聯

```sql
COUNSELOR_STUDENT
  id                  UUID  PK
  counselor_user_id   UUID  FK → USER
  student_id          UUID  FK → STUDENT
  assigned_date       DATE
  priority            ENUM  (high, medium, low)   -- 風險等級
  assigned_by         UUID  FK → USER             -- 誰指派的
  note                TEXT  NULL                  -- 指派原因摘要（加密存）
  is_active           BOOLEAN  DEFAULT true       -- 結案後設為 false
```

`priority` 讓輔導老師的學生清單按風險排序而非字母順序。  
`is_active` 區分「目前輔導中」與「已結案」，歷史記錄都保留。  
這張表的讀取權限僅限 `role=counselor` 與學校管理員，導師、科任教師、家長、學生均不可見。

---

## 三、關聯圖總覽

```
SCHOOL
  └── ACADEMIC_YEAR
        └── CLASS ──────────────── USER（導師）
              └── STUDENT
                    ├── USER（學生帳號，1對1）
                    ├── PARENT_STUDENT ── USER（家長帳號，多對多）
                    └── COUNSELOR_STUDENT ── USER（輔導教師，多對多）

CLASS ── TEACHER_SUBJECT_CLASS ── USER（科任教師，多對多）
```

---

## 四、資料建立的時序

所有資料都有依賴關係，必須按照順序建立：

### Phase 0：系統初始化（部署時一次性）

由系統管理員（非學校教師）在後台操作：

1. 建立 `SCHOOL` 記錄
2. 建立 `ACADEMIC_YEAR`（目前學年）
3. 建立學校管理員帳號（`role=admin`）

---

### Phase 1：開學前，教務組作業（每學年）

由教務組管理員登入後台批次操作：

1. **建立班級**（CSV 匯入或手動）
   - 此時 `homeroom_teacher_id` 可以先留 NULL
   
2. **建立教師帳號**（CSV 匯入）
   - 欄位：姓名、手機號碼
   - 系統自動產生初始密碼，以簡訊寄送
   - 教師首次登入須強制修改密碼

3. **指派導師**
   - 教務組操作：選班級 → 選教師 → 儲存
   - 更新 `class.homeroom_teacher_id`

4. **指派科任教師**
   - 教務組操作：選教師 → 選班級 → 選科目
   - 寫入 `TEACHER_SUBJECT_CLASS`
   - 可以 CSV 批次匯入（學校通常有課表 Excel）

---

### Phase 2：學生資料建立（開學時）

由教務組操作：

5. **匯入學生名冊**（CSV 或從 SchoolBO 同步）
   - 每筆學生記錄自動建立對應的 `USER` 帳號
   - 初始帳號 = 學號，初始密碼 = 身分證後六碼（或學校自訂規則）
   - `student.class_id` 指向正確的班級

6. **指派輔導教師**（輔導組操作）
   - 輔導組登入後，在 `COUNSELOR_STUDENT` 建立關聯
   - 設定初始風險等級（通常全員預設 `low`，有特殊狀況再調整）

---

### Phase 3：家長綁定（最複雜，開學後持續進行）

家長綁定有兩種方案，建議並存：

#### 方案 A：邀請連結（安全性較高，建議作為主流程）

```
學校匯入家長手機號碼（來自紙本資料或 SchoolBO）
    ↓
系統產生一次性 token（有效期 7 天）
    ↓
簡訊發送：「請點擊連結完成綁定：https://...?token=xxx」
    ↓
家長點擊 → 設定密碼 → parent_student 自動建立
```

優點：學校掌控誰能綁定，不可能被外人搶先  
缺點：需要學校事先收集家長手機號碼

#### 方案 B：學號 + 驗證碼（補綁用，例如轉學生）

```
家長自行下載 App → 選擇「綁定孩子」
    ↓
輸入孩子學號 + 學校發的驗證碼（紙本通知單上的6位數字）
    ↓
驗證通過 → 建立帳號 → parent_student 建立
    ↓
導師收到通知：「xxx 的家長已完成綁定」（供確認）
```

優點：家長可以自助操作  
缺點：驗證碼若外洩有被冒綁風險，需要加上導師確認機制

#### 特殊情況處理

- **一個孩子有兩位家長**：各自綁定，都建立 `PARENT_STUDENT`，其中一位設 `is_primary=true`
- **一位家長有多個孩子在同校**：同一個家長帳號，綁定多筆 `PARENT_STUDENT`，App 介面切換孩子
- **家長離婚 / 監護權異動**：只能由學校管理員操作，不開放家長自行修改（避免糾紛）

---

### Phase 4：學年結束，資料繼承

7. **建立新 `ACADEMIC_YEAR`**
8. **建立新班級**（高一新生 + 原高一升高二 + 原高二升高三）
9. **更新 `student.class_id`**（批次操作，原有資料不動）
10. **舊學年 `ACADEMIC_YEAR.is_current` 設為 false，新學年設為 true**
11. **重新指派導師與科任教師**（每年可能異動）
12. **家長綁定不需重做**（`PARENT_STUDENT` 是跨學年有效的）
13. **畢業生** `student.graduated_at` 填入日期，帳號設 `is_active=false`

---

## 五、各角色「能看到誰的資料」總整理

| 角色 | 可看學生範圍 | 備註 |
|------|-------------|------|
| 學生 | 只有自己 | API 層強制 `WHERE student.user_id = token.sub` |
| 家長 | 只有自己綁定的孩子 | API 層強制 `WHERE parent_student.parent_user_id = token.sub` |
| 導師 | 自己班的全部學生 | `WHERE class.homeroom_teacher_id = token.sub` |
| 科任教師 | 自己任課班的學生 | `JOIN teacher_subject_class WHERE teacher_user_id = token.sub` |
| 輔導教師 | 自己負責的學生（跨班） | `JOIN counselor_student WHERE counselor_user_id = token.sub` |
| 教務組管理員 | 全校所有學生 | 需有 `role=admin` 才能操作 |

> ⚠️ 重要：以上過濾**必須在後端 API 層強制執行**，前端隱藏介面只是 UX，不是安全防線。

---

## 六、敏感資料的特殊處理

### 輔導個案資料

`COUNSELOR_STUDENT` 表本身只是關聯，真正的個案內容會在另一張表：

```sql
COUNSELING_RECORD
  id                  UUID  PK
  counselor_student_id UUID FK → COUNSELOR_STUDENT
  session_date        DATE
  content_encrypted   BYTEA    -- AES-256 加密，key 存 Android Keystore
  created_by          UUID  FK → USER
  created_at          TIMESTAMP
```

- 內容欄位在應用層加密後再存入資料庫（不只靠資料庫層加密）
- 讀取時只有本人（被指派的輔導教師）可以解密
- 即使資料庫被入侵，個案內容也無法直接讀取
- 所有讀取操作都要留存取日誌（供稽核）

### 家長聯絡資訊

家長的手機號碼是個資，應：
- 傳輸時走 HTTPS
- 儲存時雜湊（查詢用）+ 加密（顯示用）
- 不在 App 前端 log 中出現

---

## 七、與 SchoolBO 的整合考量

台灣高中普遍使用 SchoolBO 等校務行政系統，已有大量學生資料。  
不應要求學校重新輸入，應提供：

1. **CSV 匯入格式**：定義標準欄位，讓學校從 SchoolBO 匯出後直接上傳
2. **API 串接**（長期目標）：若 SchoolBO 有提供 API，可自動同步學生異動
3. **異動通知機制**：轉學生入學、退學等異動，需有人工確認步驟，不完全自動

---

## 八、尚待決策的問題

在開始寫程式之前，以下問題需要和學校端確認：

- [ ] 家長手機號碼從哪裡來？學校有沒有現成資料？
- [ ] 學生初始密碼規則？（身分證後六碼有隱私疑慮）
- [ ] 輔導個案資料的保存年限？（法規要求？）
- [ ] 是否需要支援家長解除綁定？（離婚、監護權移轉等）
- [ ] 科任教師需要看到其他班同一科的成績比較嗎？
- [ ] 學校管理員是教務主任還是資訊組？誰來操作後台？
- [ ] 伺服器要落地台灣哪裡？自架還是雲端（AWS 台灣、GCP 台灣）？

---

*整理自系統設計討論，實作前請先確認以上待決事項*
