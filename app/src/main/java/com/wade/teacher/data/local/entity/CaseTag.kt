package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "case_tags")
data class CaseTag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: String
)

@Entity(
    tableName = "case_log_tags",
    primaryKeys = ["caseLogId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = CaseLog::class,
            parentColumns = ["id"],
            childColumns = ["caseLogId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CaseTag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["caseLogId"]),
        Index(value = ["tagId"])
    ]
)
data class CaseLogTag(
    val caseLogId: Int,
    val tagId: Int
)
