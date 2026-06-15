package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_plans")
data class LessonPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectName: String,
    val topic: String,
    val grade: Int,
    val competencies: String, // Comma separated tags
    val content: String,
    val prepNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
