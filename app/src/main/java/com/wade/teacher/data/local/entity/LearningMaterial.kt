package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learning_materials",
    foreignKeys = [ForeignKey(
        entity = LessonPlan::class,
        parentColumns = ["id"],
        childColumns = ["lessonPlanId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("lessonPlanId")]
)
data class LearningMaterial(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonPlanId: Int,
    val title: String,
    val type: String = "文件",   // 文件 / 影片 / 連結
    val filePath: String? = null,
    val url: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
