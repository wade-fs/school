package com.wade.teacher.util

import android.util.Log
import com.wade.teacher.data.local.entity.CounselingProfile
import com.wade.teacher.data.local.entity.Student
import com.wade.teacher.data.local.entity.TimetableEntry
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {
    private const val TAG = "CsvParser"

    fun parseStudentCsv(inputStream: InputStream): List<Pair<Student, CounselingProfile?>> {
        val result = mutableListOf<Pair<Student, CounselingProfile?>>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        // Skip header
        val header = reader.readLine() ?: run {
            Log.e(TAG, "Empty student CSV file")
            return emptyList()
        }
        Log.d(TAG, "Parsing student CSV with header: $header")
        
        var lineCount = 0
        reader.forEachLine { line ->
            lineCount++
            if (line.isBlank()) return@forEachLine
            
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
                    Log.e(TAG, "Error parsing line $lineCount: $line", e)
                }
            } else {
                Log.w(TAG, "Line $lineCount skipped (insufficient columns ${tokens.size}): $line")
            }
        }
        Log.d(TAG, "Finished parsing student CSV. Successfully parsed ${result.size} students from $lineCount lines.")
        return result
    }

    fun parseTimetableCsv(inputStream: InputStream): List<TimetableEntry> {
        val result = mutableListOf<TimetableEntry>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        // Skip header
        val header = reader.readLine() ?: run {
            Log.e(TAG, "Empty timetable CSV file")
            return emptyList()
        }
        Log.d(TAG, "Parsing timetable CSV with header: $header")
        
        var lineCount = 0
        reader.forEachLine { line ->
            lineCount++
            if (line.isBlank()) return@forEachLine
            
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
                    Log.e(TAG, "Error parsing timetable line $lineCount: $line", e)
                }
            } else {
                Log.w(TAG, "Line $lineCount skipped (insufficient columns): $line")
            }
        }
        Log.d(TAG, "Finished parsing timetable. Successfully parsed ${result.size} entries.")
        return result
    }

    data class GradeImportRow(val studentId: String, val score: Int, val feedback: String?)

    fun parseGradeCsv(inputStream: InputStream): List<GradeImportRow> {
        val result = mutableListOf<GradeImportRow>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        // Skip header: 學號,分數,評語
        val header = reader.readLine() ?: run {
            Log.e(TAG, "Empty grade CSV file")
            return emptyList()
        }
        Log.d(TAG, "Parsing grade CSV with header: $header")
        
        var lineCount = 0
        reader.forEachLine { line ->
            lineCount++
            if (line.isBlank()) return@forEachLine
            
            val tokens = line.split(",")
            if (tokens.size >= 2) {
                try {
                    result.add(GradeImportRow(
                        studentId = tokens[0].trim(),
                        score = tokens[1].trim().toInt(),
                        feedback = tokens.getOrNull(2)?.trim()
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing grade line $lineCount: $line", e)
                }
            } else {
                Log.w(TAG, "Line $lineCount skipped: $line")
            }
        }
        Log.d(TAG, "Finished parsing grades. Successfully parsed ${result.size} rows.")
        return result
    }
}
