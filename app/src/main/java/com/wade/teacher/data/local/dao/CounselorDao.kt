package com.wade.teacher.data.local.dao

import androidx.room.*
import com.wade.teacher.data.local.entity.Student
import com.wade.teacher.data.local.entity.CaseLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CounselorDao {
    // Student operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Query("SELECT * FROM students ORDER BY currentClass, seatNo")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE studentId = :id")
    suspend fun getStudentById(id: String): Student?

    @Query("SELECT * FROM students WHERE name LIKE '%' || :query || '%' OR studentId LIKE '%' || :query || '%'")
    fun searchStudents(query: String): Flow<List<Student>>

    @Update
    suspend fun updateStudent(student: Student)

    // Case Log operations
    @Insert
    suspend fun insertCaseLog(log: CaseLog)

    @Query("SELECT * FROM case_logs WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getLogsForStudent(studentId: String): Flow<List<CaseLog>>

    @Query("DELETE FROM case_logs WHERE id = :id")
    suspend fun deleteLog(id: Int)
}
