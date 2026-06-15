package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val scheduledAt: Long,          // 預約時間
    val duration: Int = 50,         // 分鐘
    val type: String,               // "初談" / "後續晤談" / "電訪" / "家訪"
    val location: String? = null,   // "輔導室" / "教室" / "電話"
    val status: String = "scheduled", // scheduled / completed / no_show / cancelled
    val reminderSent: Boolean = false
)
