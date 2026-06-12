package com.wade.teacher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wade.teacher.data.local.dao.CounselorDao
import com.wade.teacher.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        CounselorTeacherNote::class,
        TimetableEntry::class,
        PeriodTime::class,
        LessonPlan::class,
        LearningMaterial::class,
        ClassroomPerformance::class,
        Assignment::class,
        Submission::class
    ],
    version = 8,
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
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).counselorDao()
                                dao.insertExternalResource(ExternalResource(name="安心專線", phone="1925", type="24小時專線", city=null, isEmergency=true))
                                dao.insertExternalResource(ExternalResource(name="生命線", phone="1995", type="24小時專線", city=null, isEmergency=true))
                                dao.insertExternalResource(ExternalResource(name="兒童保護專線", phone="113", type="24小時專線", city=null, isEmergency=true))
                                dao.insertExternalResource(ExternalResource(name="少年專線", phone="0800-001769", type="全國專線", city=null, isEmergency=false))
                                dao.insertExternalResource(ExternalResource(name="張老師專線", phone="1980", type="24小時專線", city=null, isEmergency=false))
                                
                                // Pre-fill Period Times
                                dao.upsertPeriodTimes(listOf(
                                    PeriodTime(1, "08:10", "09:00"),
                                    PeriodTime(2, "09:10", "10:00"),
                                    PeriodTime(3, "10:10", "11:00"),
                                    PeriodTime(4, "11:10", "12:00"),
                                    PeriodTime(5, "13:10", "14:00"),
                                    PeriodTime(6, "14:10", "15:00"),
                                    PeriodTime(7, "15:10", "16:00"),
                                    PeriodTime(8, "16:10", "17:00")
                                ))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
