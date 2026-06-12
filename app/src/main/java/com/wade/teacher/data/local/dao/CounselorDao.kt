package com.wade.teacher.data.local.dao

import androidx.room.*
import com.wade.teacher.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CounselorDao {
    // Students (Academic)
    @Query("SELECT * FROM students ORDER BY studentId ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Query("SELECT * FROM students WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: String): Student?

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()

    // Counseling Profiles
    @Transaction
    @Query("SELECT * FROM students ORDER BY studentId ASC")
    fun getAllStudentsWithProfiles(): Flow<List<StudentWithProfile>>

    @Query("SELECT * FROM counseling_profiles WHERE studentId = :studentId")
    fun getProfileForStudent(studentId: String): Flow<CounselingProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: CounselingProfile)

    // Case Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CaseLog): Long

    @Query("SELECT * FROM case_logs WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getLogsForStudent(studentId: String): Flow<List<CaseLog>>

    @Query("DELETE FROM case_logs WHERE id = :id")
    suspend fun deleteLog(id: Int)

    // Tags
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: CaseTag): Long

    @Query("SELECT * FROM case_tags")
    fun getAllTags(): Flow<List<CaseTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogTag(link: CaseLogTag)

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM case_tags JOIN case_log_tags ON case_tags.id = case_log_tags.tagId WHERE caseLogId = :logId")
    fun getTagsForLog(logId: Int): Flow<List<CaseTag>>

    // Appointments
    @Query("SELECT * FROM appointments WHERE scheduledAt >= :startOfDay ORDER BY scheduledAt ASC")
    fun getUpcomingAppointments(startOfDay: Long): Flow<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppointment(appointment: Appointment)

    // Crisis Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrisisEvent(event: CrisisEvent)

    @Query("SELECT * FROM crisis_events WHERE studentId = :studentId ORDER BY occurredAt DESC")
    fun getCrisisEventsForStudent(studentId: String): Flow<List<CrisisEvent>>

    // Counselor-Teacher Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounselorNote(note: CounselorTeacherNote)

    @Query("SELECT * FROM counselor_teacher_notes WHERE toTeacherId = :teacherId AND isRead = 0")
    fun getUnreadNotesForTeacher(teacherId: String): Flow<List<CounselorTeacherNote>>

    @Query("SELECT * FROM counselor_teacher_notes WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun getNotesForStudent(studentId: String): Flow<List<CounselorTeacherNote>>

    @Query("UPDATE counselor_teacher_notes SET isRead = 1 WHERE id = :id")
    suspend fun markNoteAsRead(id: Int)

    // Mood Checks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodCheckSession(session: MoodCheckSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodCheckResponse(response: MoodCheckResponse)

    @Query("SELECT * FROM mood_check_responses WHERE sessionId = :sessionId")
    fun getResponsesForSession(sessionId: Int): Flow<List<MoodCheckResponse>>

    @Query("SELECT * FROM mood_check_sessions WHERE classId = :classId ORDER BY conductedAt DESC LIMIT :limit")
    fun getLatestSessions(classId: String, limit: Int): Flow<List<MoodCheckSession>>

    @Query("SELECT * FROM mood_check_sessions ORDER BY conductedAt DESC LIMIT 1")
    fun getLastSession(): Flow<MoodCheckSession?>

    // External Resources
    @Query("SELECT * FROM external_resources")
    fun getExternalResources(): Flow<List<ExternalResource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExternalResource(resource: ExternalResource)

    // Audit Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Query("SELECT * FROM audit_logs ORDER BY performedAt DESC LIMIT 200")
    fun getRecentAuditLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY performedAt DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    // Timetable
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableEntries(entries: List<TimetableEntry>)

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, period ASC")
    fun getFullTimetable(): Flow<List<TimetableEntry>>

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteFullTimetable()

}
