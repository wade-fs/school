package com.wade.teacher.util

import com.wade.teacher.data.local.entity.Student
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {
    fun parseStudentCsv(inputStream: InputStream): List<Student> {
        val students = mutableListOf<Student>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        // Skip header
        val header = reader.readLine() ?: return emptyList()
        
        reader.forEachLine { line ->
            val tokens = line.split(",")
            if (tokens.size >= 7) {
                try {
                    val student = Student(
                        studentId = tokens[0].trim(),
                        name = tokens[1].trim(),
                        gender = tokens[2].trim(),
                        entryYear = tokens[3].trim().toInt(),
                        currentGrade = tokens[4].trim().toInt(),
                        currentClass = tokens[5].trim(),
                        seatNo = tokens[6].trim().toInt(),
                        status = tokens.getOrNull(7)?.trim()?.takeIf { it.isNotEmpty() } ?: "Active",
                        statusNote = tokens.getOrNull(8)?.trim(),
                        legalStatus = tokens.getOrNull(9)?.trim(),
                        guardianName = tokens.getOrNull(10)?.trim(),
                        guardianPhone = tokens.getOrNull(11)?.trim()
                    )
                    students.add(student)
                } catch (e: Exception) {
                    // Log error or skip invalid line
                }
            }
        }
        return students
    }
}
