package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val subjectName: String,
    val title: String,
    val type: String = "作業", // "作業" / "小考" / "期中考" / "期末考" / "其他"
    val description: String,
    val dueDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val totalPoints: Int = 100
)

@Entity(tableName = "submissions")
data class Submission(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val assignmentId: Int,
    val studentId: String,
    val studentName: String,
    val status: String,        // "待繳" / "已繳" / "已批改"
    val score: Int? = null,
    val feedback: String? = null,
    val submittedAt: Long? = null
)
