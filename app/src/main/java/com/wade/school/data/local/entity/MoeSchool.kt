package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "moe_schools")
data class MoeSchool(
    @PrimaryKey val code: String,
    val name: String,
    val publicPrivate: String, // 公立, 私立
    val city: String,
    val address: String,
    val phone: String,
    val website: String? = null
)
