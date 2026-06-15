package com.wade.school.util

import com.wade.school.data.local.entity.CounselingProfile
import com.wade.school.data.local.entity.Student
import com.wade.school.data.local.entity.TimetableEntry
import com.wade.school.data.local.entity.MoeSchool
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class GradeRow(val studentId: String, val score: Int, val feedback: String?)

object CsvParser {

    fun parseStudentCsv(inputStream: InputStream): List<Pair<Student, CounselingProfile?>> {
        val result = mutableListOf<Pair<Student, CounselingProfile?>>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.readLine() ?: return emptyList() // skip header
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
                        seatNo = tokens.getOrNull(7)?.trim()?.toIntOrNull() ?: 0,
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
                    android.util.Log.w("CsvParser", "Skipping invalid row: $line", e)
                }
            }
        }
        return result
    }

    // CSV 格式: 星期(0), 節次(1), 班級(2), 科目(3), 教室(4)
    fun parseTimetableCsv(inputStream: InputStream): List<TimetableEntry> {
        val result = mutableListOf<TimetableEntry>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.readLine() ?: return emptyList() // skip header
        reader.forEachLine { line ->
            val tokens = line.split(",")
            if (tokens.size >= 5) {
                try {
                    result.add(
                        TimetableEntry(
                            dayOfWeek = tokens[0].trim().toInt(),
                            period = tokens[1].trim().toInt(),
                            classId = tokens[2].trim(),
                            subjectName = tokens[3].trim(),
                            roomNumber = tokens[4].trim()
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.w("CsvParser", "Skipping invalid timetable row: $line", e)
                }
            }
        }
        return result
    }

    // CSV 格式: 學年度(0), 代碼(1), 學校名稱(2), 公/私立(3), 縣市名稱(4), 地址(5), 電話(6), 網址(7)
    fun parseMoeSchoolCsv(inputStream: InputStream): List<MoeSchool> {
        val result = mutableListOf<MoeSchool>()
        try {
            val bytes = inputStream.readBytes()
            // 嘗試先用 UTF-8 解析
            var content = String(bytes, java.nio.charset.Charset.forName("UTF-8"))
            
            // 如果解析出奇怪的替換符號 (代表不是合法的 UTF-8，這通常是 Big5 編碼)
            if (content.contains("\ufffd")) {
                content = String(bytes, java.nio.charset.Charset.forName("Big5"))
            }

            val lines = content.split("\n", "\r\n").filter { it.isNotBlank() }
            if (lines.isEmpty()) return result

            lines.drop(1).forEach { line ->
                // 使用簡易的狀態機來處理包含引號的 CSV
                val tokens = line.split(",").map { it.trim().removeSurrounding("\"") }
                if (tokens.size >= 7) {
                    try {
                        val city = tokens[4].replace(Regex("\\[.*?\\]"), "")
                        val address = tokens[5].replace(Regex("\\[.*?\\]"), "")

                        result.add(
                            MoeSchool(
                                code = tokens[1],
                                name = tokens[2],
                                publicPrivate = tokens[3],
                                city = city,
                                address = address,
                                phone = tokens[6],
                                website = tokens.getOrNull(7)
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("CsvParser", "跳過無效學校列: $line", e)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CsvParser", "解析教育部 CSV 出錯", e)
        }
        return result
    }

    // CSV 格式: studentId, score, feedback(optional)
    fun parseGradeCsv(inputStream: InputStream): List<GradeRow> {
        val result = mutableListOf<GradeRow>()
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.readLine() ?: return emptyList() // skip header
        reader.forEachLine { line ->
            val tokens = line.split(",")
            if (tokens.size >= 2) {
                try {
                    result.add(
                        GradeRow(
                            studentId = tokens[0].trim(),
                            score = tokens[1].trim().toInt(),
                            feedback = tokens.getOrNull(2)?.trim()?.ifEmpty { null }
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.w("CsvParser", "Skipping invalid grade row: $line", e)
                }
            }
        }
        return result
    }
}
