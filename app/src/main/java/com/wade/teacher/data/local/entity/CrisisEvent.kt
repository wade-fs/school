package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crisis_events")
data class CrisisEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val eventType: String,      // "自傷" / "自殺意念" / "霸凌" / "家暴通報" / "其他"
    val occurredAt: Long,
    val reportedAt: Long = System.currentTimeMillis(),
    val reportedBy: String,     // 誰通報
    val severity: String,       // "緊急" / "嚴重" / "一般"
    val actionTaken: String,    // 採取的處理行動
    val followUpDate: Long? = null,
    val notifiedParent: Boolean = false,
    val notifiedPrincipal: Boolean = false,
    val externalReferral: Boolean = false,
    val referralUnit: String? = null
)
