package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counselor_teacher_notes")
data class CounselorTeacherNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val fromCounselorId: String,
    val toTeacherId: String,       // 導師
    val summary: String,           // 去識別化摘要
    val requestType: String,       // "請多關心" / "注意課堂行為" / "避免點名" / "其他"
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
