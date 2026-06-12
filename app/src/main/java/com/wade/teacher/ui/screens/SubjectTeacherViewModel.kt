package com.wade.teacher.ui.screens

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wade.teacher.data.local.AppDatabase
import com.wade.teacher.data.local.entity.ClassroomPerformance
import com.wade.teacher.data.local.entity.LearningMaterial
import com.wade.teacher.data.local.entity.LessonPlan
import com.wade.teacher.data.local.entity.Assignment
import com.wade.teacher.data.local.entity.Submission
import com.wade.teacher.data.local.entity.TimetableEntry
import com.wade.teacher.util.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SubjectClassInfo(
    val classId: String,
    val subjectName: String,
    val roomNumber: String,
    val studentCount: Int,
    val nextLessonTime: String? = null
)

class SubjectTeacherViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).counselorDao()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _selectedClassId = MutableStateFlow<String?>(null)
    val selectedClassId: StateFlow<String?> = _selectedClassId.asStateFlow()

    // Derived from TimetableEntries in DB
    val assignedClasses: StateFlow<List<SubjectClassInfo>> = dao.getFullTimetable()
        .map { entries ->
            entries.groupBy { it.classId + it.subjectName }
                .map { (_, group) ->
                    val first = group.first()
                    SubjectClassInfo(
                        classId = first.classId,
                        subjectName = first.subjectName,
                        roomNumber = first.roomNumber,
                        studentCount = 0, // Need to join with students count per class
                        nextLessonTime = "星期${first.dayOfWeek} 第${first.period}節"
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial selection if empty
        viewModelScope.launch {
            assignedClasses.collect {
                if (_selectedClassId.value == null && it.isNotEmpty()) {
                    _selectedClassId.value = it.first().classId
                }
            }
        }
    }

    fun selectClass(classId: String) {
        _selectedClassId.value = classId
    }

    fun importStudents(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val importedList = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        CsvParser.parseStudentCsv(inputStream)
                    } ?: emptyList()
                }
                if (importedList.isNotEmpty()) {
                    dao.insertStudents(importedList.map { it.first })
                    // Subject teacher doesn't necessarily handle profiles, but we can save them if present
                    importedList.mapNotNull { it.second }.forEach { dao.upsertProfile(it) }
                }
            } catch (e: Exception) {
                android.util.Log.e("SubjectTeacherViewModel", "Error importing students", e)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun importSchedule(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val entries = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        CsvParser.parseTimetableCsv(inputStream)
                    } ?: emptyList()
                }
                if (entries.isNotEmpty()) {
                    dao.deleteFullTimetable()
                    dao.insertTimetableEntries(entries)
                }
            } catch (e: Exception) {
                android.util.Log.e("SubjectTeacherViewModel", "Error importing schedule", e)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllStudents()
            dao.deleteFullTimetable()
        }
    }

    // --- Sprint 2: Lesson Plans ---

    val allLessonPlans: Flow<List<LessonPlan>> = dao.getAllLessonPlans()

    fun saveLessonPlan(plan: LessonPlan, materials: List<LearningMaterial>) {
        viewModelScope.launch(Dispatchers.IO) {
            val planId = dao.insertLessonPlan(plan).toInt()
            materials.forEach {
                dao.insertLearningMaterial(it.copy(lessonPlanId = planId))
            }
        }
    }

    fun getMaterialsForPlan(planId: Int) = dao.getMaterialsForPlan(planId)

    fun addMaterialToPlan(material: LearningMaterial) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertLearningMaterial(material)
        }
    }

    fun updateLessonPlanNotes(planId: Int, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Need a more specific update in DAO or use a copy pattern
            // For now, let's keep it simple and assume a full plan update if needed
            // Actually, let's just use the existing insert (upsert) behavior
        }
    }

    // --- Sprint 1-C: Classroom Performance ---

    fun markStudentPerformance(studentId: String, classId: String, tagName: String, note: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertClassroomPerformance(
                ClassroomPerformance(
                    studentId = studentId,
                    classId = classId,
                    tagName = tagName,
                    academicYear = com.wade.teacher.util.AcademicUtils.getCurrentAcademicYear(),
                    semester = com.wade.teacher.util.AcademicUtils.getCurrentSemester(),
                    note = note
                )
            )
        }
    }

    fun getStudentsInClass(classId: String) = dao.getAllStudents()
        .map { list -> list.filter { it.currentClass == classId } }

    // --- Sprint 3: Assignments ---

    fun createAssignment(classId: String, subjectName: String, title: String, description: String, dueDate: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val assignment = Assignment(
                classId = classId,
                subjectName = subjectName,
                title = title,
                description = description,
                dueDate = dueDate
            )
            val assignmentId = dao.insertAssignment(assignment).toInt()
            
            // Auto-create pending submissions for all students in the class
            val students = dao.getStudentsByClass(classId).first()
            val submissions = students.map { student ->
                Submission(
                    assignmentId = assignmentId,
                    studentId = student.studentId,
                    studentName = student.name,
                    status = "待繳"
                )
            }
            dao.insertSubmissions(submissions)
        }
    }

    fun getAssignmentsForClass(classId: String) = dao.getAssignmentsForClass(classId)

    fun getSubmissionsForAssignment(assignmentId: Int) = dao.getSubmissionsForAssignment(assignmentId)

    fun gradeSubmission(submission: Submission, score: Int, feedback: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateSubmission(submission.copy(score = score, feedback = feedback, status = "已批改"))
        }
    }
}
