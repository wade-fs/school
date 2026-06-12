package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classroom_performances")
data class ClassroomPerformance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val classId: String,
    val tagName: String,       // "發言踴躍" / "小組領導" / "未帶課本" / "作業遲交"
    val timestamp: Long = System.currentTimeMillis(),
    val academicYear: Int,
    val semester: Int,
    val note: String? = null
)
