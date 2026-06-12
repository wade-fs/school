package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable_entries")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: Int,        // 1 (Mon) to 5 (Fri)
    val period: Int,           // 1 to 8
    val classId: String,
    val subjectName: String,
    val roomNumber: String
)
