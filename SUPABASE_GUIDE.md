# 零預算後端方案：Supabase 實作指南 (正式版)

## 第一步：註冊與建立專案 (100% 免費)
1.  前往 [Supabase 官網 (https://supabase.com/)](https://supabase.com/) 並點擊 **"Start your project"**。
2.  使用 GitHub 帳號登入 (最快)。
3.  點擊 **"New Project"**：
    - **Name**: `TaiwanSchoolApp` (或其他您喜歡的名稱)
    - **Database Password**: 設定一個密碼並記下來。
    - **Region**: 建議選 **Tokyo (ap-northeast-1)** 或 **Singapore**，這對台灣的連線速度最快。
    - **Plan**: 選擇 **Free (免費)**。

## 第二步：建立資料結構 (SQL)
1.  進入 Supabase 後台，點擊左側選單的 **"SQL Editor"**。
2.  點擊 **"+ New query"**。
3.  貼上以下腳本並點擊 **"Run"**：

```sql
-- 1. 使用者擴充資料 (Linked to Supabase Auth)
CREATE TABLE profiles (
  id UUID REFERENCES auth.users ON DELETE CASCADE PRIMARY KEY,
  name TEXT NOT NULL,
  role TEXT CHECK (role IN ('teacher', 'student', 'parent')),
  school_id TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. 班級資料
CREATE TABLE classes (
  id SERIAL PRIMARY KEY,
  class_name TEXT NOT NULL,
  grade INTEGER NOT NULL,
  homeroom_teacher_id UUID REFERENCES profiles(id)
);

-- 3. 學生與家長關聯
CREATE TABLE student_parent_relation (
  student_id UUID REFERENCES profiles(id),
  parent_id UUID REFERENCES profiles(id),
  PRIMARY KEY (student_id, parent_id)
);

-- 4. 出缺席紀錄
CREATE TABLE attendance (
  id SERIAL PRIMARY KEY,
  student_id UUID REFERENCES profiles(id),
  class_id INTEGER REFERENCES classes(id),
  status TEXT NOT NULL,
  recorded_by UUID REFERENCES profiles(id),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. 數位聯絡簿 (Interaction)
CREATE TABLE contact_logs (
  id SERIAL PRIMARY KEY,
  class_id INTEGER REFERENCES classes(id),
  content TEXT NOT NULL,
  is_signed_by_parents BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 開啟即時更新 (Real-time)
ALTER PUBLICATION supabase_realtime ADD TABLE attendance;
ALTER PUBLICATION supabase_realtime ADD TABLE contact_logs;
```

## 第三步：取得 API Key 並填入 App
1.  點擊左下角的 **"Project Settings" (齒輪圖示)**。
2.  選擇 **"API"**。
3.  您會看到：
    - **Project URL**: 例如 `https://xyz.supabase.co`
    - **API Key (anon public)**: 一長串以 `ey...` 開頭的字元。
4.  將這兩項填入 App 中的初始化代碼即可。

## 第四步：Android 端開發狀態
我已經將 Supabase 的 SDK 加入到 `app/build.gradle.kts` 中。當您完成上述註冊後，即可在 `MainActivity.kt` 進行連線實作。
