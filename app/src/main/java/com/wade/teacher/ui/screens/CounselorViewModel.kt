package com.wade.teacher.ui.screens

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wade.teacher.data.local.AppDatabase
import com.wade.teacher.data.local.entity.*
import com.wade.teacher.util.CaseLogCrypto
import com.wade.teacher.util.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CounselorViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).counselorDao()

    val studentsWithProfiles: StateFlow<List<StudentWithProfile>> = dao.getAllStudentsWithProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Backward compatibility or simple list
    val students: StateFlow<List<Student>> = dao.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classes: StateFlow<List<String>> = dao.getAllStudents()
        .map { students -> students.map { it.currentClass }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _schoolConfig = MutableStateFlow(SchoolConfig())
    val schoolConfig: StateFlow<SchoolConfig> = _schoolConfig

    fun updateSchoolConfig(name: String, type: SchoolType) {
        _schoolConfig.value = SchoolConfig(name, type)
    }

    fun importCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val importedList = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        CsvParser.parseStudentCsv(inputStream)
                    } ?: emptyList()
                }
                
                if (importedList.isNotEmpty()) {
                    val students = importedList.map { it.first }
                    val profiles = importedList.mapNotNull { it.second }
                    
                    dao.insertStudents(students)
                    profiles.forEach { dao.upsertProfile(it) }
                }
            } catch (e: Exception) {
                android.util.Log.e("CounselorViewModel", "Error importing CSV", e)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun promoteAllStudents() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = students.value
            val config = _schoolConfig.value
            val maxGrade = when (config.schoolType) {
                SchoolType.JUNIOR_HIGH -> 9
                else -> 12
            }

            val promotedList = currentList.map { student ->
                if (student.currentSemester == 1) {
                    student.copy(currentSemester = 2)
                } else {
                    if (student.currentGrade < maxGrade) {
                        student.copy(currentGrade = student.currentGrade + 1, currentSemester = 1)
                    } else {
                        student // Logic for graduation could be applied to CounselingProfile
                    }
                }
            }
            dao.insertStudents(promotedList)
        }
    }

    fun clearAllStudents() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllStudents()
        }
    }

    fun toggleKeyTracking(studentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = dao.getProfileForStudent(studentId).first() ?: CounselingProfile(studentId)
            dao.upsertProfile(profile.copy(isKeyTracking = !profile.isKeyTracking))
        }
    }

    fun scheduleAppointment(studentId: String, timestamp: Long, type: String = "晤談") {
        viewModelScope.launch(Dispatchers.IO) {
            val appointment = Appointment(studentId = studentId, scheduledAt = timestamp, type = type)
            dao.upsertAppointment(appointment)
            
            // Also update legacy field in profile for display
            val profile = dao.getProfileForStudent(studentId).first() ?: CounselingProfile(studentId)
            dao.upsertProfile(profile.copy(nextAppointment = timestamp))
        }
    }

    fun setStudentStatus(studentId: String, status: String, legalStatus: String? = null, priority: String = "Normal") {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = dao.getProfileForStudent(studentId).first() ?: CounselingProfile(studentId)
            dao.upsertProfile(profile.copy(status = status, legalStatus = legalStatus, priority = priority))
        }
    }

    fun getTodayAppointments(startOfDay: Long): Flow<List<Appointment>> {
        val endOfDay = startOfDay + 86400000L
        return dao.getUpcomingAppointments(startOfDay)
            .map { list -> list.filter { it.scheduledAt < endOfDay } }
    }

    // New: Encrypted Case Log
    fun saveCaseLog(studentId: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val (encrypted, iv) = CaseLogCrypto.encrypt(content)
            val student = dao.getStudentById(studentId) ?: return@launch
            
            val log = CaseLog(
                studentId = studentId,
                academicYear = com.wade.teacher.util.AcademicUtils.getCurrentAcademicYear(),
                semester = com.wade.teacher.util.AcademicUtils.getCurrentSemester(),
                classAtTime = student.currentClass,
                contentEncrypted = encrypted,
                contentIv = iv
            )
            dao.insertLog(log)
        }
    }

    fun getLogsForStudent(studentId: String): Flow<List<CaseLog>> = dao.getLogsForStudent(studentId)

    fun decryptLogContent(log: CaseLog): String {
        return try {
            CaseLogCrypto.decrypt(log.contentEncrypted, log.contentIv)
        } catch (e: Exception) {
            "解密失敗"
        }
    }

    // --- Sprint 2: Mood Check ---

    private val _activeSessionId = MutableStateFlow<Int?>(null)
    val activeSessionId: StateFlow<Int?> = _activeSessionId

    fun startMoodCheckSession(classId: String, counselorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = dao.insertMoodCheckSession(
                MoodCheckSession(
                    classId = classId,
                    conductedAt = System.currentTimeMillis(),
                    conductedBy = counselorId
                )
            )
            _activeSessionId.value = sessionId.toInt()
        }
    }

    fun recordMoodResponse(sessionId: Int, studentId: String, score: Int, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertMoodCheckResponse(
                MoodCheckResponse(sessionId = sessionId, studentId = studentId,
                                  score = score, note = note)
            )
        }
    }

    fun finishMoodCheckSession() {
        _activeSessionId.value = null
    }

    fun getRecentMoodSessions(classId: String): Flow<List<MoodCheckSession>> = dao.getLatestSessions(classId, 5)

    val lastSession: Flow<MoodCheckSession?> = dao.getLastSession()

    fun getResponsesForSession(sessionId: Int) = dao.getResponsesForSession(sessionId)

    // --- Sprint 3: Crisis Events ---

    fun reportCrisisEvent(
        studentId: String,
        eventType: String,
        severity: String,
        actionTaken: String,
        occurredAt: Long = System.currentTimeMillis(),
        reportedBy: String,
        notifiedParent: Boolean = false,
        notifiedPrincipal: Boolean = false,
        referralUnit: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertCrisisEvent(
                CrisisEvent(
                    studentId = studentId,
                    eventType = eventType,
                    occurredAt = occurredAt,
                    reportedBy = reportedBy,
                    severity = severity,
                    actionTaken = actionTaken,
                    notifiedParent = notifiedParent,
                    notifiedPrincipal = notifiedPrincipal,
                    externalReferral = referralUnit != null,
                    referralUnit = referralUnit
                )
            )
            // 高嚴重性事件 → 自動將學生風險等級設為 High
            if (severity == "緊急") {
                val profile = dao.getProfileForStudent(studentId).first() ?: CounselingProfile(studentId)
                dao.upsertProfile(profile.copy(priority = "High"))
            }
        }
    }

    fun getCrisisEventsForStudent(studentId: String): Flow<List<CrisisEvent>> =
        dao.getCrisisEventsForStudent(studentId)

    // --- Sprint 4: Counselor-Teacher Notes ---

    fun sendNoteToTeacher(
        studentId: String,
        fromCounselorId: String,
        toTeacherId: String,
        summary: String,
        requestType: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertCounselorNote(
                CounselorTeacherNote(
                    studentId = studentId,
                    fromCounselorId = fromCounselorId,
                    toTeacherId = toTeacherId,
                    summary = summary,
                    requestType = requestType
                )
            )
        }
    }

    fun getNotesForStudent(studentId: String): Flow<List<CounselorTeacherNote>> =
        dao.getNotesForStudent(studentId)

    fun markNoteAsRead(noteId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.markNoteAsRead(noteId)
        }
    }

    fun getClassMoodAlerts(classId: String): Flow<List<String>> = dao.getLatestSessions(classId, 2).map { sessions ->
        if (sessions.isEmpty()) return@map emptyList<String>()
        
        val latestSession = sessions[0]
        val previousSession = if (sessions.size > 1) sessions[1] else null
        
        val latestResponses = dao.getResponsesForSession(latestSession.id).first()
        val prevResponsesMap = if (previousSession != null) {
            dao.getResponsesForSession(previousSession.id).first().associateBy { it.studentId }
        } else {
            emptyMap()
        }
        
        latestResponses.filter { resp ->
            val score = resp.score
            val isLowScore = score <= 3
            val prevScore = prevResponsesMap[resp.studentId]?.score
            val isDropping = prevScore != null && (prevScore - score >= 2)
            
            isLowScore || isDropping
        }.map { it.studentId }
    }
}
