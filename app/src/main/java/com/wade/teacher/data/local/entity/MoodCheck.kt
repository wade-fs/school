package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
