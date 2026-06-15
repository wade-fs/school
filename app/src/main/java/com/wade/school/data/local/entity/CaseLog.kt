package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "case_logs")
data class CaseLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val academicYear: Int,
    val semester: Int,
    val classAtTime: String,
    val contentEncrypted: ByteArray,   // AES-256-GCM 加密後的位元組
    val contentIv: ByteArray,          // 每筆記錄獨立的 IV
    val audioPath: String? = null      // 本地音檔路徑
)
