package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "referral_records")
data class ReferralRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val resourceId: Int,              // FK → ExternalResource
    val resourceName: String,         // 冗餘存一份，避免原資源刪除後紀錄遺失
    val referredAt: Long = System.currentTimeMillis(),
    val referredBy: String,           // 輔導教師 ID
    val reason: String,               // 轉介原因
    val followUpDate: Long? = null,
    val outcome: String? = null       // 後續結果記錄
)
