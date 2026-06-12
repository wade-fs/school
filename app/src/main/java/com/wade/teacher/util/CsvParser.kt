package com.wade.teacher.util

import com.wade.teacher.data.local.entity.CounselingProfile
import com.wade.teacher.data.local.entity.Student
import com.wade.teacher.data.local.entity.TimetableEntry
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {
    fun parseStudentCsv(inputStream: InputStream): List<Pair<Student, CounselingProfile?>> {
        val result = mutableListOf<Pair<Student, CounselingProfile?>>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        // Skip header
        val header = reader.readLine() ?: return emptyList()
        
        reader.forEachLine { line ->
            val tokens = line.split(",")
            if (tokens.size >= 7) {
                try {
                    val studentId = tokens[0].trim()
                    val student = Student(
                        studentId = studentId,
                        name = tokens[1].trim(),
                        gender = tokens[2].trim(),
                        entryYear = tokens[3].trim().toInt(),
                        currentGrade = tokens[4].trim().toInt(),
                        currentSemester = tokens[5].trim().toInt(),
                        currentClass = tokens[6].trim(),
                        seatNo = tokens[7].trim().toInt(),
                        phone = tokens.getOrNull(8)?.trim(),
                        email = tokens.getOrNull(9)?.trim(),
                        guardianName = tokens.getOrNull(13)?.trim(),
                        guardianPhone = tokens.getOrNull(14)?.trim()
                    )
                    
                    val status = tokens.getOrNull(10)?.trim()
                    val profile = if (!status.isNullOrEmpty()) {
                        CounselingProfile(
                            studentId = studentId,
                            status = status,
                            statusNote = tokens.getOrNull(11)?.trim(),
                            legalStatus = tokens.getOrNull(12)?.trim()
                        )
                    } else null
                    
                    result.add(Pair(student, profile))
                } catch (e: Exception) {
                    // Log error or skip invalid line
                }
            }
        }
        return result
    }

    fun parseTimetableCsv(inputStream: InputStream): List<TimetableEntry> {
        val result = mutableListOf<TimetableEntry>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        // Skip header
        val header = reader.readLine() ?: return emptyList()
        
        reader.forEachLine { line ->
            val tokens = line.split(",")
            if (tokens.size >= 5) {
                try {
                    result.add(TimetableEntry(
                        dayOfWeek = tokens[0].trim().toInt(),
                        period = tokens[1].trim().toInt(),
                        classId = tokens[2].trim(),
                        subjectName = tokens[3].trim(),
                        roomNumber = tokens[4].trim()
                    ))
                } catch (e: Exception) {
                    // Skip invalid line
                }
            }
        }
        return result
    }
}
