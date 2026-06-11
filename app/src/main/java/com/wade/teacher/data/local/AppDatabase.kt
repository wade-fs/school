package com.wade.teacher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wade.teacher.data.local.dao.CounselorDao
import com.wade.teacher.data.local.entity.*

@Database(
    entities = [
        Student::class,
        CounselingProfile::class,
        CaseLog::class,
        CaseTag::class,
        CaseLogTag::class,
        Appointment::class,
        CrisisEvent::class,
        MoodCheckSession::class,
        MoodCheckResponse::class,
        ExternalResource::class,
        AuditLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun counselorDao(): CounselorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teacher_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
