package com.wade.teacher.data.local.entity

enum class SchoolType {
    JUNIOR_HIGH,    // 國中 (7-9)
    SENIOR_HIGH,    // 高中 (10-12)
    COMPREHENSIVE   // 綜合高中 (7-12)
}

data class SchoolConfig(
    val schoolName: String = "新北市立清水高級中學",
    val schoolType: SchoolType = SchoolType.COMPREHENSIVE,
    val schoolWebsite: String? = null
)
