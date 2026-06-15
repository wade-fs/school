package com.wade.school.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "submissions",
    foreignKeys = [ForeignKey(
        entity = Assignment::class,
        parentColumns = ["id"],
        childColumns = ["assignmentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("assignmentId"), Index("studentId")]
)
data class Submission(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val assignmentId: Int,
    val studentId: String,
    val studentName: String,
    val status: String = "待繳",   // 待繳 / 已繳 / 已批改
    val score: Int? = null,
    val feedback: String? = null,
    val submittedAt: Long? = null
)
