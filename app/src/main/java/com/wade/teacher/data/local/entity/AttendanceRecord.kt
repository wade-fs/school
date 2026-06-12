package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val classId: String,
    val date: Long,
    val status: String // "出席", "遲到", "曠課", "病假", "事假", "公假"
)
