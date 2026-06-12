package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_plans")
data class LessonPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectName: String,
    val topic: String,
    val grade: Int,
    val competencies: String, // Comma separated tags for simplicity in prototype
    val content: String,
    val prepNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "learning_materials")
data class LearningMaterial(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonPlanId: Int,
    val title: String,
    val type: String,          // "PDF" / "Video" / "Link" / "Quiz"
    val url: String,
    val description: String? = null
)
