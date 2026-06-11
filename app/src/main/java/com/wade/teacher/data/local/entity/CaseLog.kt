package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "case_logs")
data class CaseLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val academicYear: Int,
    val semester: Int,
    val classAtTime: String,
    val content: String,
    val audioPath: String? = null, // 本地音檔路徑
    val tags: String? = null // 如: "家庭問題, 學習適應"
)
