package com.wade.school.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class StudentWithProfile(
    @Embedded val student: Student,
    @Relation(
        parentColumn = "studentId",
        entityColumn = "studentId"
    )
    val profile: CounselingProfile?
)
