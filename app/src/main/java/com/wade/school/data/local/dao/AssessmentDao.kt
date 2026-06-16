package com.wade.school.data.local.dao

import androidx.room.*
import com.wade.school.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<AssessmentQuestion>)

    @Query("SELECT * FROM assessment_questions WHERE templateId = :templateId ORDER BY `order` ASC")
    fun getQuestionsForTemplate(templateId: String): Flow<List<AssessmentQuestion>>

    @Query("SELECT * FROM assessment_sessions ORDER BY scheduledAt DESC")
    fun getAllSessions(): Flow<List<AssessmentSession>>

    @Query("SELECT COUNT(*) FROM assessment_responses WHERE sessionId = :sessionId")
    fun getCompletedCount(sessionId: String): Flow<Int>

    @Query("SELECT s.* FROM students s WHERE s.currentClass = (SELECT targetClass FROM assessment_sessions WHERE sessionId = :sessionId) AND s.studentId NOT IN (SELECT studentId FROM assessment_responses WHERE sessionId = :sessionId)")
    fun getPendingStudents(sessionId: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponse(response: AssessmentResponse)

    @Query("SELECT * FROM assessment_responses WHERE sessionId = :sessionId AND studentId = :studentId")
    fun getResponse(sessionId: String, studentId: String): Flow<AssessmentResponse?>
}
