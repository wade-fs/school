package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "period_times")
data class PeriodTime(
    @PrimaryKey val period: Int,    // 1 to 8
    val startTime: String,           // "08:10"
    val endTime: String              // "09:00"
)
