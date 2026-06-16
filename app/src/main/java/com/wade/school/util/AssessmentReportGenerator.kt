package com.wade.school.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.wade.school.data.local.entity.AssessmentResponse
import com.wade.school.data.local.entity.AssessmentQuestion
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object AssessmentReportGenerator {

    fun generatePhq9ExcelReport(
        context: Context,
        response: AssessmentResponse,
        questions: List<AssessmentQuestion>,
        studentName: String
    ): Uri? {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("PHQ-9 測驗報告")

        val headerStyle = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        headerStyle.setFont(font)

        // Header Row
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("項目")
        headerRow.createCell(1).setCellValue("內容")
        headerRow.getCell(0).cellStyle = headerStyle
        headerRow.getCell(1).cellStyle = headerStyle

        // Report Details
        var rowNum = 1
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("學生姓名")
            createCell(1).setCellValue(studentName)
        }
        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("測驗類型")
            createCell(1).setCellValue("PHQ-9 憂鬱症篩檢")
        }
        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("測驗日期")
            createCell(1).setCellValue(dateFormat.format(Date(response.completedAt)))
        }
        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("總分")
            createCell(1).setCellValue(response.totalScore?.toDouble() ?: 0.0)
        }
        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("風險警示")
            createCell(1).setCellValue(if (response.riskFlagged) "是" else "否")
        }
        sheet.createRow(rowNum++).apply {
            createCell(0).setCellValue("風險程度")
            createCell(1).setCellValue(getPhq9RiskLevel(response.totalScore ?: 0))
        }

        rowNum++ // Blank row
        
        // Questions and Answers Header
        val qaHeaderRow = sheet.createRow(rowNum++)
        qaHeaderRow.createCell(0).setCellValue("題號")
        qaHeaderRow.createCell(1).setCellValue("題目")
        qaHeaderRow.createCell(2).setCellValue("作答")
        qaHeaderRow.getCell(0).cellStyle = headerStyle
        qaHeaderRow.getCell(1).cellStyle = headerStyle
        qaHeaderRow.getCell(2).cellStyle = headerStyle

        // Questions and Answers
        val answersJson = JSONObject(response.answersJson)
        questions.sortedBy { it.order }.forEach { q ->
            val answerScore = answersJson.optInt(q.id.toString(), 0)
            sheet.createRow(rowNum++).apply {
                createCell(0).setCellValue(q.order.toDouble())
                createCell(1).setCellValue(q.text)
                createCell(2).setCellValue(convertPhq9ScoreToText(answerScore))
            }
        }

        // Auto-size columns
        for (i in 0 until 3) {
            sheet.autoSizeColumn(i)
        }

        // Save the file
        val fileName = "PHQ9_${studentName}_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.xlsx"
        val file = File(context.cacheDir, fileName) // Use cacheDir for temporary file
        
        return try {
            FileOutputStream(file).use { fos ->
                workbook.write(fos)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            workbook.close()
        }
    }

    private fun getPhq9RiskLevel(score: Int): String {
        return when {
            score >= 20 -> "重度憂鬱"
            score >= 15 -> "中重度憂鬱"
            score >= 10 -> "中度憂鬱"
            score >= 5 -> "輕度憂鬱"
            else -> "無憂鬱傾向"
        }
    }

    private fun convertPhq9ScoreToText(score: Int): String {
        return when (score) {
            0 -> "從未"
            1 -> "幾天"
            2 -> "超過一半"
            3 -> "幾乎每天"
            else -> "N/A"
        }
    }
}
