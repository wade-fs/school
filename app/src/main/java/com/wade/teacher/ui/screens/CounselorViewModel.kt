package com.wade.teacher.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wade.teacher.data.local.entity.Student
import com.wade.teacher.util.CsvParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CounselorViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _schoolConfig = MutableStateFlow(com.wade.teacher.data.local.entity.SchoolConfig())
    val schoolConfig: StateFlow<com.wade.teacher.data.local.entity.SchoolConfig> = _schoolConfig

    fun updateSchoolConfig(name: String, type: com.wade.teacher.data.local.entity.SchoolType) {
        _schoolConfig.value = com.wade.teacher.data.local.entity.SchoolConfig(name, type)
    }

    fun importCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                // Request persistable URI permission if needed (though OpenDocument handles it for this session)
                val importedList = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        CsvParser.parseStudentCsv(inputStream)
                    } ?: emptyList()
                }
                
                if (importedList.isEmpty()) {
                    android.util.Log.w("CounselorViewModel", "Imported list is empty or file could not be read.")
                } else {
                    android.util.Log.d("CounselorViewModel", "Successfully parsed ${importedList.size} students.")
                    // Upsert logic: merge imported list with current list based on studentId
                    val currentMap = _students.value.associateBy { it.studentId }.toMutableMap()
                    importedList.forEach { student ->
                        currentMap[student.studentId] = student 
                    }
                    _students.value = currentMap.values.toList().sortedBy { it.studentId }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CounselorViewModel", "Error importing CSV", e)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun promoteAllStudents() {
        val config = _schoolConfig.value
        _students.value = _students.value.map { student ->
            if (student.currentSemester == 1) {
                // 上學期 -> 下學期
                student.copy(currentSemester = 2)
            } else {
                // 下學期 -> 明年級上學期
                val maxGrade = when (config.schoolType) {
                    com.wade.teacher.data.local.entity.SchoolType.JUNIOR_HIGH -> 9
                    else -> 12
                }
                
                if (student.currentGrade < maxGrade) {
                    student.copy(currentGrade = student.currentGrade + 1, currentSemester = 1)
                } else {
                    // 到達最高年級下學期結束 -> 結案/畢業
                    student.copy(status = "結案")
                }
            }
        }
    }

    fun updateStudent(updatedStudent: Student) {
        _students.value = _students.value.map { 
            if (it.studentId == updatedStudent.studentId) updatedStudent else it 
        }
    }

    fun clearAllStudents() {
        _students.value = emptyList()
    }

    fun toggleKeyTracking(studentId: String) {
        _students.value = _students.value.map {
            if (it.studentId == studentId) it.copy(isKeyTracking = !it.isKeyTracking) else it
        }
    }

    fun scheduleAppointment(studentId: String, timestamp: Long) {
        _students.value = _students.value.map {
            if (it.studentId == studentId) it.copy(nextAppointment = timestamp) else it
        }
    }

    fun setStudentStatus(studentId: String, status: String, legalStatus: String? = null, priority: String = "Normal") {
        _students.value = _students.value.map {
            if (it.studentId == studentId) {
                it.copy(status = status, legalStatus = legalStatus, priority = priority)
            } else it
        }
    }
}
