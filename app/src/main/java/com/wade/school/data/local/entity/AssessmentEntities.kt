package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

// 1. 量表模板
enum class TemplateCategory { MOOD, MENTAL_HEALTH, CAREER, GENDER_EQUALITY, INTERPERSONAL, CAMPUS_SAFETY, CUSTOM }

@Entity(tableName = "assessment_templates")
data class AssessmentTemplate(
    @PrimaryKey val templateId: String,
    val name: String,
    val category: TemplateCategory,
    val description: String,
    val isBuiltIn: Boolean = true
)

// 2. 量表題目
enum class QuestionType { LIKERT, SCALE, SINGLE, MULTI, TEXT, YES_NO }

@Entity(tableName = "assessment_questions")
data class AssessmentQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateId: String,
    val order: Int,
    val text: String,
    val type: QuestionType,
    val options: String? = null,
    val riskTrigger: Boolean = false,
    val riskThreshold: Int? = null,
    val riskCondition: String = "GTE"
)

// 3. 施測場次
enum class SessionStatus { DRAFT, OPEN, CLOSED, ARCHIVED }

@Entity(tableName = "assessment_sessions")
data class AssessmentSession(
    @PrimaryKey val sessionId: String,
    val templateId: String,
    val targetClass: String,
    val conductedBy: String,
    val scheduledAt: Long,
    val status: SessionStatus = SessionStatus.OPEN
)

// 4. 學生作答
@Entity(tableName = "assessment_responses",
    indices = [Index(value = ["sessionId", "studentId"], unique = true)])
data class AssessmentResponse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val studentId: String,
    val answersJson: String,
    val totalScore: Int? = null,
    val riskFlagged: Boolean = false,
    val completedAt: Long = System.currentTimeMillis()
)
