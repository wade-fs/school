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
    val schoolName: String = "新北市立清水高級中學",
    val schoolType: SchoolType = SchoolType.COMPREHENSIVE,
    val schoolWebsite: String? = null,
    val homeroomClass: String = "101",
    val ownerName: String? = null,
    val accessPin: String? = null, // 儲存加密後的 PIN
    val useBiometric: Boolean = false
)
