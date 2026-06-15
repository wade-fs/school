package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "homeroom_checklists")
data class HomeroomChecklist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val content: String,
    val isDone: Boolean = false,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "parent_contact_logs")
data class ParentContactLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val studentName: String,
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
