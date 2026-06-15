package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SchoolType {
    JUNIOR_HIGH,
    SENIOR_HIGH,
    COMPREHENSIVE
}

@Entity(tableName = "school_config")
data class SchoolConfig(
    @PrimaryKey val id: Int = 0,
    val schoolName: String = "市立清水高中",
    val schoolType: SchoolType = SchoolType.COMPREHENSIVE,
    val schoolWebsite: String? = "http://www.cssh.ntpc.edu.tw",
    val address: String? = "新北市土城區明德路一段72號",
    val phone: String? = "(02)22707801",
    val homeroomClass: String = "701",
    val ownerName: String? = null,
    val accessPin: String? = null, // 儲存加密後的 PIN
    val useBiometric: Boolean = false
)
