package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val subjectName: String,
    val title: String,
    val type: String = "作業",   // 作業 / 測驗 / 專題
    val description: String = "",
    val dueDate: Long,
    val createdAt: Long = System.currentTimeMillis()
)
