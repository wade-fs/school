package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classroom_performances")
data class ClassroomPerformance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val classId: String,
    val tagName: String,       // 例如 "發言積極" / "需加強"
    val academicYear: Int,
    val semester: Int,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
