package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_bulletins")
data class ClassBulletin(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val readCount: Int = 0
)
