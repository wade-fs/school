package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertSeverity { WATCH, WARNING, URGENT }

@Entity(tableName = "risk_alerts")
data class RiskAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val sourceType: String,            // "ASSESSMENT" / "MANUAL" / "CRISIS"
    val sourceId: String,              // sessionId 或 crisisEventId
    val triggeredAt: Long = System.currentTimeMillis(),
    val severity: AlertSeverity,
    val reason: String,                // 觸發原因描述
    val isRead: Boolean = false,
    val handledAt: Long? = null,
    val handledBy: String? = null,
    val handledNote: String? = null
)
