package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,        // "VIEW" / "CREATE" / "UPDATE" / "DELETE"
    val targetType: String,    // "CaseLog" / "CrisisEvent" / "CounselingProfile"
    val targetId: String,
    val performedBy: String,   // 輔導老師帳號
    val performedAt: Long = System.currentTimeMillis(),
    val ipAddress: String? = null
)
