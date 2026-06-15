package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 輔導專屬資料（輔導教師可讀寫）
 */
@Entity(
    tableName = "counseling_profiles",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CounselingProfile(
    @PrimaryKey val studentId: String,
    val priority: String = "Normal",       // High / Medium / Low / Normal
    val isKeyTracking: Boolean = false,
    val status: String = "Active",         // Active / 休學 / 轉學 / 結案 / 外部轉介
    val statusNote: String? = null,
    val legalStatus: String? = null,
    val nextAppointment: Long? = null,
    val assignedCounselorId: String? = null  // 負責的輔導老師
)
