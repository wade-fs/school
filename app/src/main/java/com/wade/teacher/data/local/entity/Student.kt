package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 學籍資料（唯讀，從教務匯入）
 */
@Entity(tableName = "students")
data class Student(
    @PrimaryKey val studentId: String, // 學號
    val name: String,
    val gender: String,
    val entryYear: Int, // 入學學年
    val currentGrade: Int, // 7-12 年級
    val currentSemester: Int, // 1 (上學期) 或 2 (下學期)
    val currentClass: String,
    val seatNo: Int,
    val phone: String? = null, // 學生個人電話
    val email: String? = null, // 學生個人 Email
    val guardianName: String? = null,
    val guardianPhone: String? = null
)
