package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val classId: String,
    val date: Long,
    val periodName: String = "全日", // 例如 "早修", "午休", "第一節"
    val status: String // "出席", "遲到", "曠課", "病假", "事假", "公假"
)
