package com.wade.school.data.local.entity

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
    // Emotion Quadrant
    val selectedEmotion: String? = null, // e.g., "興奮", "焦慮"
    val emotionQuadrant: String? = null, // "YELLOW", "RED", "GREEN", "BLUE"
    // BSRS-5 (0-4 points each)
    val bsrsSleep: Int? = null,
    val bsrsTense: Int? = null,
    val bsrsAnger: Int? = null,
    val bsrsDepression: Int? = null,
    val bsrsInferiority: Int? = null,
    val bsrsSuicide: Int? = null,
    
    val score: Int = 0, // Keep for backward compatibility or total BSRS score
    val note: String? = null  // 學生自願填寫的說明
)
