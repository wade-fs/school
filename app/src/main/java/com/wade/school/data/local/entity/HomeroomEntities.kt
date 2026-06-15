package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "homeroom_checklists")
data class HomeroomChecklist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val content: String,
    val isDone: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val targetType: String = "Class", // "Class" or "Students"
    val assignedStudentNames: String? = null // Comma-separated names for display
)

@Entity(tableName = "parent_contact_logs")
data class ParentContactLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val parentName: String, // 聯絡對象
    val title: String, // 主題
    val contactDate: Long = System.currentTimeMillis(),
    val channel: String, // 電話, Line, 親晤
    val reason: String,
    val summary: String
)

@Entity(tableName = "behavior_observations")
data class BehaviorObservations(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
    val date: Long = System.currentTimeMillis(),
    val category: String, // 學習, 常規, 人際, 優點
    val content: String,
    val tag: String? = null // 正向, 待改進
)

@Entity(tableName = "class_cadres")
data class ClassCadre(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val position: String, // 班長, 副班長, 學藝...
    val studentId: String,
    val studentName: String
)

@Entity(tableName = "class_activities")
data class ClassActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val title: String,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val location: String? = null,
    val locationUrl: String? = null, // 儲存 Google Maps 連結
    val description: String,
    val participantNames: String? = null
)

@Entity(tableName = "class_honors")
data class ClassHonor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val studentId: String? = null, // null 代表全班榮譽
    val studentName: String? = null,
    val awardTitle: String,
    val awardDate: Long = System.currentTimeMillis(),
    val level: String, // 校內, 全縣, 全國
    val category: String // 體育, 學術, 服務, 其他
)
