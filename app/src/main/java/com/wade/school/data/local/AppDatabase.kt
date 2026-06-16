package com.wade.school.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wade.school.data.local.dao.CounselorDao
import com.wade.school.data.local.entity.*

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
        LessonPlan::class,
        LearningMaterial::class,
        Assignment::class,
        Submission::class,
        ClassroomPerformance::class,
        PeriodTime::class,
        AttendanceRecord::class,
        ClassBulletin::class,
        ContactBookEntry::class,
        SchoolConfig::class,
        HomeroomChecklist::class,
        ParentContactLog::class,
        BehaviorObservations::class,
        ClassCadre::class,
        ClassActivity::class,
        ClassHonor::class,
        MoeSchool::class,
        OfficialDocument::class,          // Phase 1: 公文管理
        RiskAlert::class
    ],
    version = 13,                        // bumped from 12
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun counselorDao(): CounselorDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teacher_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }

        // alias used by SchoolInfoViewModel
        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
