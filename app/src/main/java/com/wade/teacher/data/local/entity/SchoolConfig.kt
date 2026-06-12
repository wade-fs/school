package com.wade.teacher.data.local.entity

enum class SchoolType {
    JUNIOR_HIGH,
    SENIOR_HIGH,
    COMPREHENSIVE
}

data class SchoolConfig(
    val schoolName: String = "新北市立清水高級中學",
    val schoolType: SchoolType = SchoolType.COMPREHENSIVE,
    val schoolWebsite: String? = null
)
