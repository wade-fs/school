package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "external_resources")
data class ExternalResource(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val type: String,           // "24小時專線" / "諮商機構" / "社福單位" / "醫療"
    val city: String?,          // NULL 表示全國性
    val notes: String? = null,
    val isEmergency: Boolean = false
)
