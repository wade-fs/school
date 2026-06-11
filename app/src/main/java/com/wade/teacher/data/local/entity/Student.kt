package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey val studentId: String, // 學號
    val name: String,
    val gender: String,
    val entryYear: Int, // 入學學年
    val currentGrade: Int,
    val currentClass: String,
    val seatNo: Int,
    val status: String = "Active", // 在學, 休學, 轉學, 結案, 外部轉介
    val statusNote: String? = null, // 註記欄 (如: 心理醫師轉介中)
    val legalStatus: String? = null, // 法律狀態 (如: 法院期, 少年監獄)
    val guardianName: String? = null,
    val guardianPhone: String? = null,
    val nextAppointment: Long? = null // 下次面談時間戳記
)
