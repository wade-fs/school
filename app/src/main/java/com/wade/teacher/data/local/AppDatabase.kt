package com.wade.teacher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wade.teacher.data.local.dao.CounselorDao
import com.wade.teacher.data.local.entity.Student
import com.wade.teacher.data.local.entity.CaseLog

@Database(entities = [Student::class, CaseLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun counselorDao(): CounselorDao
}
