package com.wade.teacher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        AuditLog::class,
        CounselorTeacherNote::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
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
