package com.wade.school.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wade.school.data.local.entity.AssessmentQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<AssessmentQuestion>)

    @Query("SELECT * FROM assessment_questions WHERE templateId = :templateId ORDER BY `order` ASC")
    fun getQuestionsForTemplate(templateId: String): Flow<List<AssessmentQuestion>>
}
