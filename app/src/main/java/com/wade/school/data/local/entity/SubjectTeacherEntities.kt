package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ── 成績權重設定 ──────────────────────────────────────────────

@Entity(
    tableName = "grade_weights",
    indices = [Index(value = ["classId", "subjectName", "academicYear", "semester"], unique = true)]
)
data class GradeWeight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val subjectName: String,
    val academicYear: Int,
    val semester: Int,
    val dailyWeight: Float = 0.3f,       // 平時成績佔比
    val midtermWeight: Float = 0.3f,     // 期中考佔比
    val finalWeight: Float = 0.4f,       // 期末考佔比
    // 平時成績子項目
    val homeworkWeight: Float = 0.4f,
    val participationWeight: Float = 0.3f,
    val quizWeight: Float = 0.3f
)

// ── 考試記錄 ──────────────────────────────────────────────

enum class ExamType { QUIZ, MIDTERM, FINAL, MAKEUP, PRACTICE }

@Entity(tableName = "exam_records")
data class ExamRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val subjectName: String,
    val examName: String,        // 「第一次段考」「第三章小考」
    val examType: ExamType,
    val examDate: Long,
    val totalScore: Int = 100,
    val academicYear: Int,
    val semester: Int
)

@Entity(
    tableName = "exam_scores",
    indices = [Index(value = ["examId", "studentId"], unique = true)]
)
data class ExamScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examId: Int,
    val studentId: String,
    val studentName: String = "", // Added for easier display
    val score: Float,            // 允許小數
    val isAbsent: Boolean = false,
    val isMakeupDone: Boolean = false,
    val makeupScore: Float? = null
)

// ── 補考管理 ──────────────────────────────────────────────

enum class MakeupExamStatus { PENDING, SCHEDULED, DONE, WAIVED }

@Entity(tableName = "makeup_exams")
data class MakeupExam(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val subjectName: String,
    val originalExamId: Int,
    val reason: String,          // 病假/事假/缺考
    val scheduledDate: Long? = null,
    val status: MakeupExamStatus = MakeupExamStatus.PENDING,
    val makeupScore: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ── 課堂互動紀錄 ──────────────────────────────────────────────

enum class InteractionType {
    RANDOM_PICK, VOLUNTEER, QUESTION,
    ANSWER_CORRECT, ANSWER_WRONG, PARTICIPATION
}

@Entity(tableName = "classroom_interactions")
data class ClassroomInteraction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val classId: String,
    val subjectName: String,
    val interactionType: InteractionType,
    val score: Int = 0,          // 加/扣分（可負值）
    val note: String? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

data class StudentScoreSummary(
    val studentId: String,
    val totalScore: Int
)

// ── 教學省思 ──────────────────────────────────────────────

@Entity(tableName = "teaching_reflections")
data class TeachingReflection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonPlanId: Int? = null,   // 可選關聯教案
    val classId: String,
    val subjectName: String,
    val teachingDate: Long,
    val topic: String,
    val whatWentWell: String = "",
    val whatToImprove: String = "",
    val studentResponse: String = "",
    val nextSteps: String = ""
)

// ── 108 課綱素養對應 ──────────────────────────────────────────────

@Entity(tableName = "competency_mappings")
data class CompetencyMapping(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonPlanId: Int,
    val competencyCode: String,    // "A1" "B2" "C3"
    val competencyLabel: String,   // "身心素質與自我精進"
    val description: String = ""   // 本節如何融入
)

// ── 科任出缺席 ────────────────────────────────────────────

enum class AttendanceStatus {
    PRESENT, LATE, ABSENT, SICK, PERSONAL, OFFICIAL, EARLY_LEAVE
}

@Entity(
    tableName = "subject_attendance",
    indices = [Index(value = ["studentId", "classId", "date", "period"], unique = true)]
)
data class SubjectAttendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val classId: String,
    val subjectName: String,
    val date: Long,
    val period: Int,
    val status: AttendanceStatus = AttendanceStatus.PRESENT
)
