package com.wade.school.data.local.dao

import androidx.room.*
import com.wade.school.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CounselorDao {

    // ── Students ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM students ORDER BY studentId ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE currentClass = :classId ORDER BY seatNo ASC")
    fun getStudentsByClass(classId: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Query("SELECT * FROM students WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: String): Student?

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()

    @Query("DELETE FROM students WHERE currentClass = :classId")
    suspend fun deleteStudentsByClass(classId: String)

    // ── Counseling Profiles ───────────────────────────────────────────────────
    @Transaction
    @Query("SELECT * FROM students ORDER BY studentId ASC")
    fun getAllStudentsWithProfiles(): Flow<List<StudentWithProfile>>

    @Transaction
    @Query("SELECT * FROM students WHERE studentId IN (SELECT studentId FROM counseling_profiles WHERE isKeyTracking = 1 OR status != 'Active')")
    fun getStudentsWithActiveProfiles(): Flow<List<StudentWithProfile>>

    @Query("SELECT * FROM counseling_profiles WHERE studentId = :studentId")
    fun getProfileForStudent(studentId: String): Flow<CounselingProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: CounselingProfile)

    // ── Case Logs ─────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CaseLog): Long

    @Query("SELECT * FROM case_logs WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getLogsForStudent(studentId: String): Flow<List<CaseLog>>

    @Query("DELETE FROM case_logs WHERE id = :id")
    suspend fun deleteLog(id: Int)

    // ── Tags ──────────────────────────────────────────────────────────────────
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

    // ── Appointments ──────────────────────────────────────────────────────────
    @Query("SELECT * FROM appointments WHERE scheduledAt >= :startOfDay ORDER BY scheduledAt ASC")
    fun getUpcomingAppointments(startOfDay: Long): Flow<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppointment(appointment: Appointment)

    // ── Crisis Events ─────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrisisEvent(event: CrisisEvent)

    @Query("SELECT * FROM crisis_events WHERE studentId = :studentId ORDER BY occurredAt DESC")
    fun getCrisisEventsForStudent(studentId: String): Flow<List<CrisisEvent>>

    // ── Mood Checks ───────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodCheckSession(session: MoodCheckSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodCheckResponse(response: MoodCheckResponse)

    @Query("SELECT * FROM mood_check_responses WHERE sessionId = :sessionId")
    fun getResponsesForSession(sessionId: Long): Flow<List<MoodCheckResponse>>

    @Query("SELECT * FROM mood_check_sessions WHERE classId = :classId ORDER BY conductedAt DESC LIMIT :limit")
    fun getLatestSessions(classId: String, limit: Int): Flow<List<MoodCheckSession>>

    @Query("SELECT * FROM mood_check_sessions ORDER BY conductedAt DESC LIMIT 1")
    fun getLastSession(): Flow<MoodCheckSession?>

    // ── External Resources ────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExternalResource(resource: ExternalResource)

    @Query("SELECT * FROM external_resources")
    fun getExternalResources(): Flow<List<ExternalResource>>

    // ── Audit Logs ────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    // ── PeriodTime ─────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeriodTimes(times: List<PeriodTime>)

    @Query("SELECT * FROM period_times ORDER BY period ASC")
    fun getPeriodTimes(): Flow<List<PeriodTime>>

    @Query("DELETE FROM period_times")
    suspend fun deleteAllPeriodTimes()

    // ── AuditLog ──────────────────────────────────────────────────────────────
    @Query("SELECT * FROM audit_logs ORDER BY performedAt DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    // ── Timetable ─────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableEntries(entries: List<TimetableEntry>)

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, period ASC")
    fun getFullTimetable(): Flow<List<TimetableEntry>>

    @Query("DELETE FROM timetable_entries")
    suspend fun deleteFullTimetable()

    // ── Lesson Plans ──────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessonPlan(plan: LessonPlan): Long

    @Query("SELECT * FROM lesson_plans ORDER BY createdAt DESC")
    fun getAllLessonPlans(): Flow<List<LessonPlan>>

    // ── Learning Materials ────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningMaterial(material: LearningMaterial)

    @Query("SELECT * FROM learning_materials WHERE lessonPlanId = :planId")
    fun getMaterialsForPlan(planId: Int): Flow<List<LearningMaterial>>

    // ── Assignments ───────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment): Long

    @Query("SELECT * FROM assignments WHERE classId = :classId ORDER BY dueDate ASC")
    fun getAssignmentsForClass(classId: String): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments ORDER BY dueDate ASC")
    fun getAllAssignments(): Flow<List<Assignment>>

    // ── Submissions ───────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmissions(submissions: List<Submission>)

    @Update
    suspend fun updateSubmission(submission: Submission)

    @Query("SELECT * FROM submissions WHERE assignmentId = :assignmentId ORDER BY studentId ASC")
    fun getSubmissionsForAssignment(assignmentId: Int): Flow<List<Submission>>

    @Query("""
        SELECT s.* FROM submissions s
        INNER JOIN assignments a ON s.assignmentId = a.id
        WHERE a.classId = :classId
    """)
    fun getAllSubmissionsByClass(classId: String): Flow<List<Submission>>

    // ── Classroom Performance ─────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassroomPerformance(performance: ClassroomPerformance)

    @Query("SELECT * FROM classroom_performances WHERE studentId = :studentId AND classId = :classId ORDER BY createdAt DESC")
    fun getPerformanceForStudent(studentId: String, classId: String): Flow<List<ClassroomPerformance>>

    // ── Counselor Teacher Notes ───────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounselorNote(note: CounselorTeacherNote)

    @Query("SELECT * FROM counselor_teacher_notes WHERE studentId = :studentId ORDER BY createdAt DESC")
    fun getNotesForStudent(studentId: String): Flow<List<CounselorTeacherNote>>

    // ── Attendance ────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(records: List<AttendanceRecord>)

    @Query("DELETE FROM attendance_records WHERE classId = :classId AND date >= :startOfDay AND date < :endOfDay AND periodName = :periodName")
    suspend fun deleteAttendanceForPeriod(classId: String, startOfDay: Long, endOfDay: Long, periodName: String)

    @Query("SELECT * FROM attendance_records WHERE classId = :classId AND date >= :startOfDay AND date < :endOfDay AND periodName = :periodName")
    fun getAttendanceForPeriod(classId: String, startOfDay: Long, endOfDay: Long, periodName: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE classId = :classId AND date >= :startOfDay AND date < :endOfDay")
    fun getAttendanceForDate(classId: String, startOfDay: Long, endOfDay: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE classId = :classId ORDER BY date DESC")
    fun getAllAttendanceForClass(classId: String): Flow<List<AttendanceRecord>>

    // ── Class Bulletins ───────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBulletin(bulletin: ClassBulletin)

    @Query("SELECT * FROM class_bulletins WHERE classId = :classId ORDER BY timestamp DESC")
    fun getBulletinsForClass(classId: String): Flow<List<ClassBulletin>>

    // ── Contact Book ──────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactBook(entry: ContactBookEntry)

    @Query("SELECT * FROM contact_book WHERE classId = :classId AND date = :date LIMIT 1")
    fun getContactBookForDate(classId: String, date: Long): Flow<ContactBookEntry?>

    // ── School Config ──────────────────────────────────────────────────────────
    @Query("SELECT * FROM school_config WHERE id = 0")
    fun getSchoolConfig(): Flow<SchoolConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchoolConfig(config: SchoolConfig)

    // ── Homeroom Checklist ──────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(item: HomeroomChecklist)

    @Update
    suspend fun updateChecklist(item: HomeroomChecklist)

    @Query("DELETE FROM homeroom_checklists WHERE id = :id")
    suspend fun deleteChecklist(id: Int)

    @Query("SELECT * FROM homeroom_checklists WHERE classId = :classId ORDER BY isDone ASC, date ASC")
    fun getChecklistByClass(classId: String): Flow<List<HomeroomChecklist>>

    // ── Parent Contact Log ────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactLog(log: ParentContactLog)

    @Query("SELECT * FROM parent_contact_logs WHERE studentId = :studentId ORDER BY contactDate DESC")
    fun getContactLogsForStudent(studentId: String): Flow<List<ParentContactLog>>

    @Query("SELECT * FROM parent_contact_logs ORDER BY contactDate DESC")
    fun getAllContactLogs(): Flow<List<ParentContactLog>>

    // ── Behavior Observations ────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: BehaviorObservations)

    @Query("SELECT * FROM behavior_observations WHERE studentId = :studentId ORDER BY date DESC")
    fun getObservationsForStudent(studentId: String): Flow<List<BehaviorObservations>>

    @Query("SELECT * FROM behavior_observations ORDER BY date DESC")
    fun getAllObservations(): Flow<List<BehaviorObservations>>

    // ── Class Cadres ──────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCadre(cadre: ClassCadre)

    @Query("DELETE FROM class_cadres WHERE id = :id")
    suspend fun deleteCadre(id: Int)

    @Query("SELECT * FROM class_cadres WHERE classId = :classId")
    fun getCadresByClass(classId: String): Flow<List<ClassCadre>>

    // ── Class Activities ──────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ClassActivity)

    @Query("SELECT * FROM class_activities WHERE classId = :classId ORDER BY startDate DESC")
    fun getActivitiesByClass(classId: String): Flow<List<ClassActivity>>

    @Query("DELETE FROM class_activities WHERE id = :id")
    suspend fun deleteActivity(id: Int)

    // ── Class Honors ──────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHonor(honor: ClassHonor)

    @Query("SELECT * FROM class_honors WHERE classId = :classId ORDER BY awardDate DESC")
    fun getHonorsByClass(classId: String): Flow<List<ClassHonor>>

    @Query("DELETE FROM class_honors WHERE id = :id")
    suspend fun deleteHonor(id: Int)

    // ── MOE Schools ──────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoeSchools(schools: List<MoeSchool>)

    @Query("SELECT * FROM moe_schools WHERE city = :city")
    fun getSchoolsByCity(city: String): Flow<List<MoeSchool>>

    @Query("SELECT * FROM moe_schools WHERE name LIKE '%' || :query || '%'")
    fun searchSchools(query: String): Flow<List<MoeSchool>>

    @Query("SELECT DISTINCT city FROM moe_schools")
    fun getAllCities(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM moe_schools")
    fun getMoeSchoolCount(): Flow<Int>
    // ── Official Documents (公文管理) ─────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDocument(doc: com.wade.school.data.local.entity.OfficialDocument)

    @Query("SELECT * FROM official_documents ORDER BY isUrgent DESC, createdAt DESC")
    fun getAllDocuments(): Flow<List<com.wade.school.data.local.entity.OfficialDocument>>

    @Query("""SELECT * FROM official_documents
        WHERE status = :status
        ORDER BY isUrgent DESC, deadline ASC, createdAt DESC""")
    fun getDocumentsByStatus(status: String): Flow<List<com.wade.school.data.local.entity.OfficialDocument>>

    @Query("""SELECT * FROM official_documents
        WHERE status NOT IN ('ARCHIVED')
        ORDER BY isUrgent DESC, deadline ASC, createdAt DESC""")
    fun getActiveDocuments(): Flow<List<com.wade.school.data.local.entity.OfficialDocument>>

    @Query("""SELECT * FROM official_documents
        WHERE title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%'
        ORDER BY createdAt DESC""")
    fun searchDocuments(query: String): Flow<List<com.wade.school.data.local.entity.OfficialDocument>>

    @Query("SELECT COUNT(*) FROM official_documents WHERE status = 'PENDING_SIGN'")
    fun getPendingSignCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM official_documents WHERE status = 'PENDING_SIGN' AND isUrgent = 1")
    fun getUrgentPendingCount(): Flow<Int>

    // ── Risk Alerts ──────────────────────────────────────────────────────────
    @Query("SELECT * FROM risk_alerts WHERE isRead = 0 ORDER BY triggeredAt DESC")
    fun getUnreadAlerts(): Flow<List<RiskAlert>>

    @Query("SELECT * FROM risk_alerts WHERE studentId = :studentId ORDER BY triggeredAt DESC")
    fun getAlertsByStudent(studentId: String): Flow<List<RiskAlert>>

    @Query("UPDATE risk_alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAsRead(alertId: Int)

    @Query("UPDATE risk_alerts SET handledAt = :time, handledBy = :by, handledNote = :note WHERE id = :alertId")
    suspend fun markAsHandled(alertId: Int, time: Long, by: String, note: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: RiskAlert)

    @Query("DELETE FROM official_documents WHERE docId = :docId")
    suspend fun deleteDocument(docId: String)
    // ── Assessment Templates ──────────────────────────────────────────────────
    @Query("SELECT * FROM assessment_templates")
    fun getAllAssessmentTemplates(): Flow<List<AssessmentTemplate>>

    @Query("SELECT COUNT(*) FROM assessment_templates")
    suspend fun getAssessmentTemplateCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentTemplate(template: AssessmentTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessmentSession(session: AssessmentSession)
}
