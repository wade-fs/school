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

// MoeSchool 資料類別 (教育部學校清單)
data class MoeSchool(
    val name: String,
    val city: String,
    val website: String?
)

class CounselorViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).counselorDao()

    val studentsWithProfiles: StateFlow<List<StudentWithProfile>> = dao.getAllStudentsWithProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── 角色篩選後的學生資料 ──────────────────────────────────────────────────
    
    // 輔導老師：僅顯示有輔導紀錄或重點追蹤的學生
    val activeCounselingStudents: StateFlow<List<StudentWithProfile>> = dao.getStudentsWithActiveProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val students: StateFlow<List<Student>> = dao.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classes: StateFlow<List<String>> = dao.getAllStudents()
        .map { students -> students.map { it.currentClass }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    // ── 學校設定 ──────────────────────────────────────────────────────────────
    private val _schoolConfig = MutableStateFlow(SchoolConfig())
    val schoolConfig: StateFlow<SchoolConfig> = _schoolConfig
    
    private fun schoolConfigFlow() = _schoolConfig

    // 導師：僅顯示導師班級的學生（必須在 _schoolConfig 宣告之後初始化）
    val homeroomStudents: StateFlow<List<Student>> = combine(dao.getAllStudents(), _schoolConfig) { list, config ->
        list.filter { it.currentClass == config.homeroomClass }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 教育部學校清單 (未來可接 API，目前為靜態示範資料)
    private val _moeSchools = MutableStateFlow<List<MoeSchool>>(emptyList())
    val moeSchools: StateFlow<List<MoeSchool>> = _moeSchools

    private val _isFetchingSchools = MutableStateFlow(false)
    val isFetchingSchools: StateFlow<Boolean> = _isFetchingSchools

    // 作息時間
    private val _periodTimes = MutableStateFlow<List<PeriodTime>>(defaultPeriodTimes())
    val periodTimes: StateFlow<List<PeriodTime>> = _periodTimes

    // 外部資源
    val externalResources: StateFlow<List<ExternalResource>> = dao.getExternalResources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // 初始化時預填外部資源（若資料庫為空）
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getExternalResources().first()
            if (existing.isEmpty()) {
                listOf(
                    ExternalResource(name = "安心專線", phone = "1925", type = "24小時專線", city = null, isEmergency = true),
                    ExternalResource(name = "生命線", phone = "1995", type = "24小時專線", city = null, isEmergency = true),
                    ExternalResource(name = "兒童保護專線", phone = "113", type = "24小時專線", city = null, isEmergency = true),
                    ExternalResource(name = "少年專線", phone = "0800-001769", type = "全國專線", city = null, isEmergency = false),
                    ExternalResource(name = "張老師專線", phone = "1980", type = "24小時專線", city = null, isEmergency = false)
                ).forEach { dao.insertExternalResource(it) }
            }
        }
        // 示範教育部學校清單（未來替換為真實 API）
        _moeSchools.value = listOf(
            MoeSchool("新北市立清水高級中學", "新北市", "https://www.cssh.ntpc.edu.tw"),
            MoeSchool("臺北市立建國高級中學", "臺北市", "https://www.ck.tp.edu.tw"),
            MoeSchool("臺北市立中山女子高級中學", "臺北市", "https://www.csghs.tp.edu.tw"),
            MoeSchool("新北市立板橋高級中學", "新北市", "https://www.pjhs.ntpc.edu.tw"),
        )
    }

    fun updateSchoolConfig(name: String, type: SchoolType, website: String? = null, homeroom: String? = null) {
        _schoolConfig.value = SchoolConfig(
            schoolName = name, 
            schoolType = type, 
            schoolWebsite = website,
            homeroomClass = homeroom ?: _schoolConfig.value.homeroomClass
        )
    }

    fun updatePeriodTimes(times: List<PeriodTime>) {
        _periodTimes.value = times
    }

    // ── 學生資料 ──────────────────────────────────────────────────────────────
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
                    withContext(Dispatchers.IO) {
                        dao.insertStudents(importedList.map { it.first })
                        importedList.mapNotNull { it.second }.forEach { dao.upsertProfile(it) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CounselorViewModel", "Error importing CSV", e)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun importStudentsForClass(context: Context, uri: Uri, classId: String) {
        viewModelScope.launch {
            _isImporting.value = true
            
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) return@launch
                
                val importedList = withContext(Dispatchers.IO) {
                    inputStream.use { CsvParser.parseStudentCsv(it) }
                }
                
                // Filter only students belonging to this class (case-insensitive and trimmed)
                val targetClass = classId.trim()
                val filteredList = importedList.filter { it.first.currentClass.trim().equals(targetClass, ignoreCase = true) }
                
                if (filteredList.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        dao.insertStudents(filteredList.map { it.first })
                        filteredList.mapNotNull { it.second }.forEach { dao.upsertProfile(it) }
                    }
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "成功匯入 ${filteredList.size} 位學生", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "未找到符合 ${classId} 班的學生資料", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "匯入失敗: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
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
                    } else student
                }
            }
            dao.insertStudents(promotedList)
        }
    }

    fun clearAllStudents() {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteAllStudents() }
    }

    fun clearStudentsForClass(classId: String) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteStudentsByClass(classId) }
    }

    fun toggleKeyTracking(studentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = dao.getProfileForStudent(studentId).first() ?: CounselingProfile(studentId)
            dao.upsertProfile(profile.copy(isKeyTracking = !profile.isKeyTracking))
        }
    }

    fun scheduleAppointment(studentId: String, timestamp: Long, type: String = "晤談") {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertAppointment(Appointment(studentId = studentId, scheduledAt = timestamp, type = type))
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

    // ── 個案記錄 ──────────────────────────────────────────────────────────────
    fun saveCaseLog(studentId: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val (encrypted, iv) = CaseLogCrypto.encrypt(content)
            val student = dao.getStudentById(studentId) ?: return@launch
            dao.insertLog(
                CaseLog(
                    studentId = studentId,
                    academicYear = com.wade.teacher.util.AcademicUtils.getCurrentAcademicYear(),
                    semester = com.wade.teacher.util.AcademicUtils.getCurrentSemester(),
                    classAtTime = student.currentClass,
                    contentEncrypted = encrypted,
                    contentIv = iv
                )
            )
        }
    }

    fun getLogsForStudent(studentId: String): Flow<List<CaseLog>> = dao.getLogsForStudent(studentId)

    fun decryptLogContent(log: CaseLog): String {
        return try { CaseLogCrypto.decrypt(log.contentEncrypted, log.contentIv) }
        catch (e: Exception) { "解密失敗" }
    }

    // ── 心情溫度計 ────────────────────────────────────────────────────────────
    private val _activeSessionId = MutableStateFlow<Int?>(null)
    val activeSessionId: StateFlow<Int?> = _activeSessionId

    fun startMoodCheckSession(classId: String, counselorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = dao.insertMoodCheckSession(
                MoodCheckSession(classId = classId, conductedAt = System.currentTimeMillis(), conductedBy = counselorId)
            )
            _activeSessionId.value = sessionId.toInt()
        }
    }

    fun recordMoodResponse(sessionId: Int, studentId: String, score: Int, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertMoodCheckResponse(MoodCheckResponse(sessionId = sessionId, studentId = studentId, score = score, note = note))
        }
    }

    fun finishMoodCheckSession() { _activeSessionId.value = null }

    fun getRecentMoodSessions(classId: String): Flow<List<MoodCheckSession>> = dao.getLatestSessions(classId, 5)

    val lastSession: Flow<MoodCheckSession?> = dao.getLastSession()

    fun getResponsesForSession(sessionId: Int) = dao.getResponsesForSession(sessionId)

    fun getClassMoodAlerts(classId: String): Flow<List<String>> = dao.getLatestSessions(classId, 2).map { sessions ->
        if (sessions.isEmpty()) return@map emptyList<String>()
        val latestSession = sessions[0]
        val previousSession = if (sessions.size > 1) sessions[1] else null
        val latestResponses = dao.getResponsesForSession(latestSession.id).first()
        val prevResponsesMap = if (previousSession != null)
            dao.getResponsesForSession(previousSession.id).first().associateBy { it.studentId }
        else emptyMap()
        latestResponses.filter { resp ->
            resp.score <= 3 || (prevResponsesMap[resp.studentId]?.score?.let { it - resp.score >= 2 } == true)
        }.map { it.studentId }
    }

    // ── 導師功能 (Homeroom) ──────────────────────────────────────────────────
    fun submitAttendance(records: List<AttendanceRecord>, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = date
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 86400000L
            
            // Delete existing records for that class and day to avoid duplicates when updating
            if (records.isNotEmpty()) {
                dao.deleteAttendanceForDate(records[0].classId, startOfDay, endOfDay)
                dao.insertAttendance(records.map { it.copy(date = startOfDay + 12 * 3600000L) }) // Store at mid-day
            }
        }
    }

    fun getAttendanceForToday(classId: String): Flow<List<AttendanceRecord>> {
        return getAttendanceForDate(classId, System.currentTimeMillis())
    }

    fun getAttendanceForDate(classId: String, date: Long): Flow<List<AttendanceRecord>> {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 86400000L
        return dao.getAttendanceForDate(classId, startOfDay, endOfDay)
    }

    fun getAllAttendanceForClass(classId: String): Flow<List<AttendanceRecord>> = dao.getAllAttendanceForClass(classId)

    fun publishBulletin(classId: String, title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertBulletin(ClassBulletin(classId = classId, title = title, content = content))
        }
    }

    fun getBulletins(classId: String): Flow<List<ClassBulletin>> = dao.getBulletinsForClass(classId)

    // ── 聯絡簿 ──────────────────────────────────────────────────────────────
    fun saveContactBookEntry(entry: ContactBookEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertContactBook(entry)
        }
    }

    fun getTodayContactBook(classId: String): Flow<ContactBookEntry?> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return dao.getContactBookForDate(classId, calendar.timeInMillis)
    }

    companion object {
        fun defaultPeriodTimes(): List<PeriodTime> = listOf(
            PeriodTime(period = 0, startTime = "07:30", endTime = "08:10"),
            PeriodTime(period = 1, startTime = "08:10", endTime = "09:00"),
            PeriodTime(period = 2, startTime = "09:10", endTime = "10:00"),
            PeriodTime(period = 3, startTime = "10:10", endTime = "11:00"),
            PeriodTime(period = 4, startTime = "11:10", endTime = "12:00"),
            PeriodTime(period = 5, startTime = "13:10", endTime = "14:00"),
            PeriodTime(period = 6, startTime = "14:10", endTime = "15:00"),
            PeriodTime(period = 7, startTime = "15:10", endTime = "16:00"),
            PeriodTime(period = 8, startTime = "16:10", endTime = "17:00"),
        )
    }
}
