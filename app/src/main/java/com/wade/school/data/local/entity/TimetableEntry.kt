package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable_entries")
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,       // 例如 "701"
    val subjectName: String,   // 例如 "數學"
    val roomNumber: String,    // 例如 "A101"
    val dayOfWeek: Int,        // 1=週一 ... 5=週五
    val period: Int            // 第幾節
)
