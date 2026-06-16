package com.wade.school.ui.screens

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wade.school.data.local.AppDatabase
import com.wade.school.data.local.dao.AssessmentDao
import com.wade.school.data.local.entity.*
import com.wade.school.util.CaseLogCrypto
import com.wade.school.util.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.wade.school.util.AcademicUtils

class CounselorViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).counselorDao()
    private val assessmentDao = AppDatabase.getDatabase(application).assessmentDao()

    // ── 評量系統 ──────────────────────────────────────────────────────────────
    val assessmentTemplates: StateFlow<List<AssessmentTemplate>> = dao.getAllAssessmentTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadAlerts: StateFlow<List<RiskAlert>> = dao.getUnreadAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAlertAsRead(alertId: Int) {
        viewModelScope.launch(Dispatchers.IO) { dao.markAsRead(alertId) }
    }

    fun getQuestions(templateId: String) = assessmentDao.getQuestionsForTemplate(templateId)

    fun initBuiltInTemplates() {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.getAssessmentTemplateCount() == 0) {
                val templates = listOf(
                    AssessmentTemplate("MOOD_WEEKLY", "學生心情溫度計 (週測)", TemplateCategory.MOOD, "每週監測學生情緒狀況，自動偵測高風險警示。", true),
                    AssessmentTemplate("PHQ9_TW", "憂鬱症篩檢 (PHQ-9)", TemplateCategory.MENTAL_HEALTH, "過去兩週憂鬱症狀篩檢，高風險即時警示。", true),
                    AssessmentTemplate("CAREER_INTEREST", "生涯興趣量表 (Holland)", TemplateCategory.CAREER, "探索職業興趣，協助志願選填。", true),
                    AssessmentTemplate("GENDER_EQUALITY", "性別平等調查", TemplateCategory.GENDER_EQUALITY, "校園性平意識與友善環境感受調查。", true),
                    AssessmentTemplate("INTERPERSONAL", "人際與霸凌調查", TemplateCategory.INTERPERSONAL, "班級人際關係與校園安全感調查。", true)
                )
                templates.forEach { dao.insertAssessmentTemplate(it) }
                
                val phq9 = listOf(
                    AssessmentQuestion(templateId = "PHQ9_TW", order = 1, text = "對事情提不起勁或沒有樂趣", type = QuestionType.LIKERT),
                    AssessmentQuestion(templateId = "PHQ9_TW", order = 2, text = "感到情緒低落、沮喪或絕望", type = QuestionType.LIKERT),
                    AssessmentQuestion(templateId = "PHQ9_TW", order = 3, text = "入睡困難、睡眠不穩或睡太多", type = QuestionType.LIKERT),
                    AssessmentQuestion(templateId = "PHQ9_TW", order = 4, text = "感到疲倦或沒有活力", type = QuestionType.LIKERT),
                    AssessmentQuestion(templateId = "PHQ9_TW", order = 5, text = "食慾不振或吃太多", type = QuestionType.LIKERT),
                    AssessmentQuestion(templateId = "PHQ9_TW", order = 6, text = "對自己感到不好，覺得自己是個失敗者或讓家人失望", type = QuestionType.LIKERT),
                    AssessmentQuestion(templateId = "PHQ9_TW", order = 7, text = "難以專注在事情上", type = QuestionType.LIKERT),
                    AssessmentQuestion(templateId = "PHQ9_TW", order = 8, text = "動作或說話速度變慢，或者坐立難安", type = QuestionType.LIKERT),
                    AssessmentQuestion(templateId = "PHQ9_TW", order = 9, text = "有不如死掉或傷害自己的念頭", type = QuestionType.LIKERT, riskTrigger = true, riskThreshold = 1)
                )
                assessmentDao.insertQuestions(phq9)

                val genderEquality = listOf(
                    AssessmentQuestion(templateId = "GENDER_EQUALITY", order = 1, text = "男生理科比女生強是天生的", type = QuestionType.LIKERT),
                    AssessmentQuestion(templateId = "GENDER_EQUALITY", order = 2, text = "家事應該由男女雙方平等分擔", type = QuestionType.LIKERT),
                    AssessmentQuestion(templateId = "GENDER_EQUALITY", order = 7, text = "女生說「不要」有時候其實是「要」", type = QuestionType.LIKERT, riskTrigger = true, riskThreshold = 4)
                )
                assessmentDao.insertQuestions(genderEquality)

                val interpersonal = listOf(
                    AssessmentQuestion(templateId = "INTERPERSONAL", order = 1, text = "我曾在學校被同學持續嘲笑、羞辱或取綽號", type = QuestionType.YES_NO, riskTrigger = true),
                    AssessmentQuestion(templateId = "INTERPERSONAL", order = 2, text = "我曾在學校被同學故意推打、踢踹或拿走我的東西", type = QuestionType.YES_NO, riskTrigger = true),
                    AssessmentQuestion(templateId = "INTERPERSONAL", order = 3, text = "整體的班級人際關係讓我感到滿意", type = QuestionType.LIKERT)
                )
                assessmentDao.insertQuestions(interpersonal)
            }
        }
    }

    fun insertReferral(referral: ReferralRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertReferral(referral)
        }
    }


    fun getStudentsByClass(classId: String) = dao.getStudentsByClass(classId)

    fun startAssessmentSession(session: AssessmentSession) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertAssessmentSession(session)
        }
    }

    fun saveAssessmentResponse(response: AssessmentResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            assessmentDao.insertResponse(response)
            if (response.riskFlagged) {
                dao.insertAlert(
                    RiskAlert(
                        studentId = response.studentId,
                        sourceType = "ASSESSMENT",
                        sourceId = response.sessionId,
                        severity = AlertSeverity.URGENT,
                        reason = "測驗高風險警示 (總分: ${response.totalScore ?: 0})"
                    )
                )
            }
        }
    }


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

    // ── Security Helper ──────────────────────────────────────────────────────
    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    // ── 學校設定 ──────────────────────────────────────────────────────────────
    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded

    val schoolConfig: StateFlow<SchoolConfig> = dao.getSchoolConfig()
        .map { 
            _isDataLoaded.value = true
            it ?: SchoolConfig() 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SchoolConfig())

    fun updateSchoolConfig(
        name: String, 
        type: SchoolType, 
        ownerName: String? = null, 
        website: String? = null, 
        homeroom: String? = null,
        address: String? = null,
        phone: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = schoolConfig.value
            val updated = current.copy(
                schoolName = name,
                schoolType = type,
                ownerName = ownerName ?: current.ownerName,
                schoolWebsite = website,
                homeroomClass = homeroom ?: current.homeroomClass,
                address = address ?: current.address,
                phone = phone ?: current.phone
            )
            dao.upsertSchoolConfig(updated)
        }
    }

    // ── 教育部學校資料 ──────────────────────────────────────────────────────
    private val _isFetchingMoe = MutableStateFlow(false)
    val isFetchingMoe: StateFlow<Boolean> = _isFetchingMoe

    val allCities: StateFlow<List<String>> = dao.getAllCities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moeSchoolCount: StateFlow<Int> = dao.getMoeSchoolCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun searchMoeSchools(query: String): Flow<List<MoeSchool>> {
        if (query.isBlank()) return flowOf(emptyList())
        android.util.Log.d("CounselorViewModel", "Searching for school: $query")
        return dao.searchSchools(query)
    }

    fun fetchMoeSchools(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingMoe.value = true
            var success = false
            try {
                val year = AcademicUtils.getCurrentAcademicYear()
                val url = "https://stats.moe.gov.tw/files/school/$year/high.csv"
                android.util.Log.d("CounselorViewModel", "Fetching MOE schools from: $url")
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.apply {
                    connectTimeout = 20000
                    readTimeout = 20000
                    requestMethod = "GET"
                    // 加入 User-Agent 模擬瀏覽器，避免被某些政府伺服器阻擋
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                }
                
                if (connection.responseCode == 200) {
                    val schools = CsvParser.parseMoeSchoolCsv(connection.inputStream)
                    android.util.Log.d("CounselorViewModel", "Parse attempt finished, got ${schools.size} schools")
                    if (schools.isNotEmpty()) {
                        dao.insertMoeSchools(schools)
                        success = true
                    } else {
                        android.util.Log.e("CounselorViewModel", "Parsed 0 schools from the stream")
                    }
                } else {
                    android.util.Log.e("CounselorViewModel", "Web fetch failed: HTTP ${connection.responseCode}")
                }
            } catch (e: Exception) {
                android.util.Log.e("CounselorViewModel", "Error fetching MOE schools from web", e)
            }

            // Web 失敗則嘗試從 Assets 載入 (預載資料)
            if (!success) {
                try {
                    context.assets.open("high.csv").use { inputStream ->
                        val schools = CsvParser.parseMoeSchoolCsv(inputStream)
                        if (schools.isNotEmpty()) {
                            dao.insertMoeSchools(schools)
                            success = true
                            android.util.Log.d("CounselorViewModel", "Successfully loaded ${schools.size} schools from assets")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CounselorViewModel", "Error loading schools from assets", e)
                }
            }

            withContext(Dispatchers.Main) {
                if (success) {
                    android.widget.Toast.makeText(context, "學校資料庫同步完成", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "同步失敗，請檢查網路連線", android.widget.Toast.LENGTH_LONG).show()
                }
                _isFetchingMoe.value = false
            }
        }
    }

    fun setupSecurity(ownerName: String, pin: String, useBiometric: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = schoolConfig.value
            val updated = current.copy(
                ownerName = ownerName,
                accessPin = hashPin(pin),
                useBiometric = useBiometric
            )
            dao.upsertSchoolConfig(updated)
        }
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = schoolConfig.value.accessPin ?: return true
        return hashPin(pin) == storedHash
    }

    fun isSecurityConfigured(): Boolean {
        return schoolConfig.value.accessPin != null
    }

    // 導師：僅顯示導師班級的學生（必須在 schoolConfig 宣告之後初始化）
    val homeroomStudents: StateFlow<List<Student>> = combine(dao.getAllStudents(), schoolConfig) { list, config ->
        list.filter { it.currentClass == config.homeroomClass }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 教育部學校清單狀態 (已移除寫死資料)
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
        
        // 初始化時自動同步全國學校資料庫（若資料庫為空）
        viewModelScope.launch(Dispatchers.IO) {
            val count = dao.getMoeSchoolCount().first()
            if (count == 0) {
                android.util.Log.d("CounselorViewModel", "MOE database is empty. Auto-syncing on startup...")
                fetchMoeSchools(application.applicationContext)
            }
        }
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
            val config = schoolConfig.value
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
                    academicYear = com.wade.school.util.AcademicUtils.getCurrentAcademicYear(),
                    semester = com.wade.school.util.AcademicUtils.getCurrentSemester(),
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

    fun saveMoodCheckResponse(sessionId: Long, studentId: String, score: Int, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertMoodCheckResponse(MoodCheckResponse(sessionId = sessionId.toInt(), studentId = studentId, score = score, note = note))

            // 高風險檢測：分數 <= 3 觸發 URGENT 警示
            if (score <= 3) {
                dao.insertAlert(
                    RiskAlert(
                        studentId = studentId,
                        sourceType = "ASSESSMENT",
                        sourceId = sessionId.toString(),
                        severity = AlertSeverity.URGENT,
                        reason = "心情溫度計低分警示 (分數: $score)"
                    )
                )
            }
        }
    }

    fun finishMoodCheckSession() { _activeSessionId.value = null }

    fun getRecentMoodSessions(classId: String): Flow<List<MoodCheckSession>> = dao.getLatestSessions(classId, 5)

    val lastSession: Flow<MoodCheckSession?> = dao.getLastSession()

    fun getResponsesForSession(sessionId: Long) = dao.getResponsesForSession(sessionId)

    fun getClassMoodAlerts(classId: String): Flow<List<String>> = dao.getLatestSessions(classId, 2).map { sessions ->
        if (sessions.isEmpty()) return@map emptyList<String>()
        val latestSession = sessions[0]
        val previousSession = if (sessions.size > 1) sessions[1] else null
        val latestResponses = dao.getResponsesForSession(latestSession.id.toLong()).first()
        val prevResponsesMap = if (previousSession != null)
            dao.getResponsesForSession(previousSession.id.toLong()).first().associateBy { it.studentId }
        else emptyMap()
        latestResponses.filter { resp ->
            resp.score <= 3 || (prevResponsesMap[resp.studentId]?.score?.let { it - resp.score >= 2 } == true)
        }.map { it.studentId }
    }

    // ── 導師功能 (Homeroom) ──────────────────────────────────────────────────
    fun submitAttendance(records: List<AttendanceRecord>, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = java.util.Calendar.getInstance()
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 86400000L
            
            // Delete existing records for that class, day, AND period to avoid duplicates when updating
            if (records.isNotEmpty()) {
                val periodName = records[0].periodName
                dao.deleteAttendanceForPeriod(records[0].classId, startOfDay, endOfDay, periodName)
                dao.insertAttendance(records.map { it.copy(date = startOfDay + 12 * 3600000L) }) 
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

    fun getAttendanceForPeriod(classId: String, date: Long, periodName: String): Flow<List<AttendanceRecord>> {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 86400000L
        return dao.getAttendanceForPeriod(classId, startOfDay, endOfDay, periodName)
    }

    fun getAllAttendanceForClass(classId: String): Flow<List<AttendanceRecord>> = dao.getAllAttendanceForClass(classId)

    fun publishBulletin(classId: String, title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertBulletin(ClassBulletin(classId = classId, title = title, content = content))
        }
    }

    fun getBulletins(classId: String): Flow<List<ClassBulletin>> = dao.getBulletinsForClass(classId)

    // ── 導師助理功能 ────────────────────────────────────────────────────────

    // 1. 每日叮嚀 (Checklist)
    fun getChecklist(classId: String) = dao.getChecklistByClass(classId)
    fun addChecklistItem(classId: String, content: String, targetType: String = "Class", assignedNames: String? = null) {
        viewModelScope.launch(Dispatchers.IO) { 
            dao.insertChecklist(HomeroomChecklist(
                classId = classId, 
                content = content,
                targetType = targetType,
                assignedStudentNames = assignedNames
            )) 
        }
    }
    fun toggleChecklistItem(item: HomeroomChecklist) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateChecklist(item.copy(isDone = !item.isDone)) }
    }
    fun deleteChecklistItem(id: Int) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteChecklist(id) }
    }

    // 2. 家長聯絡紀錄 (Contact Log)
    fun getContactLogs(studentId: String) = dao.getContactLogsForStudent(studentId)
    fun getAllContactLogs() = dao.getAllContactLogs()
    fun addContactLog(log: ParentContactLog) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertContactLog(log) }
    }

    // 3. 行為觀察 (Observation)
    fun getObservations(studentId: String) = dao.getObservationsForStudent(studentId)
    fun getAllObservations() = dao.getAllObservations()
    fun addObservation(observation: BehaviorObservations) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertObservation(observation) }
    }

    // 4. 幹部管理 (Cadre)
    fun getCadres(classId: String) = dao.getCadresByClass(classId)
    fun upsertCadre(cadre: ClassCadre) {
        viewModelScope.launch(Dispatchers.IO) { dao.upsertCadre(cadre) }
    }
    fun deleteCadre(id: Int) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteCadre(id) }
    }

    // 5. 班級活動 (Activity)
    fun getActivities(classId: String) = dao.getActivitiesByClass(classId)
    fun addActivity(activity: ClassActivity) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertActivity(activity) }
    }
    fun updateActivity(activity: ClassActivity) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertActivity(activity) } // insertActivity is REPLACE
    }
    fun deleteActivity(id: Int) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteActivity(id) }
    }

    // 6. 優良事蹟 (Honor)
    fun getHonors(classId: String) = dao.getHonorsByClass(classId)
    fun addHonor(honor: ClassHonor) {
        viewModelScope.launch(Dispatchers.IO) { dao.insertHonor(honor) }
    }
    fun deleteHonor(id: Int) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteHonor(id) }
    }

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
