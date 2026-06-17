package com.wade.school.data.local

import androidx.room.TypeConverter
import com.wade.school.data.local.entity.*

class Converters {
    @TypeConverter
    fun fromByteArray(value: ByteArray): String =
        android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP)

    @TypeConverter
    fun toByteArray(value: String): ByteArray =
        android.util.Base64.decode(value, android.util.Base64.NO_WRAP)

    @TypeConverter
    fun fromDocStatus(value: DocStatus): String = value.name
    @TypeConverter
    fun toDocStatus(value: String): DocStatus = DocStatus.valueOf(value)

    @TypeConverter
    fun fromDocCategory(value: DocCategory): String = value.name
    @TypeConverter
    fun toDocCategory(value: String): DocCategory = DocCategory.valueOf(value)

    @TypeConverter
    fun fromAlertSeverity(value: AlertSeverity): String = value.name
    @TypeConverter
    fun toAlertSeverity(value: String): AlertSeverity = AlertSeverity.valueOf(value)

    @TypeConverter
    fun fromTemplateCategory(value: TemplateCategory): String = value.name
    @TypeConverter
    fun toTemplateCategory(value: String): TemplateCategory = TemplateCategory.valueOf(value)

    @TypeConverter
    fun fromQuestionType(value: QuestionType): String = value.name
    @TypeConverter
    fun toQuestionType(value: String): QuestionType = QuestionType.valueOf(value)

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name
    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter fun fromExamType(v: ExamType): String = v.name
    @TypeConverter fun toExamType(v: String): ExamType = ExamType.valueOf(v)

    @TypeConverter fun fromMakeupExamStatus(v: MakeupExamStatus): String = v.name
    @TypeConverter fun toMakeupExamStatus(v: String): MakeupExamStatus = MakeupExamStatus.valueOf(v)

    @TypeConverter fun fromInteractionType(v: InteractionType): String = v.name
    @TypeConverter fun toInteractionType(v: String): InteractionType = InteractionType.valueOf(v)

    @TypeConverter fun fromAttendanceStatus(v: AttendanceStatus): String = v.name
    @TypeConverter fun toAttendanceStatus(v: String): AttendanceStatus = AttendanceStatus.valueOf(v)

    @TypeConverter fun fromDisciplineType(v: DisciplineType): String = v.name
    @TypeConverter fun toDisciplineType(v: String): DisciplineType = DisciplineType.valueOf(v)

    @TypeConverter fun fromLeaveType(v: LeaveType): String = v.name
    @TypeConverter fun toLeaveType(v: String): LeaveType = LeaveType.valueOf(v)

    @TypeConverter fun fromLeaveStatus(v: LeaveStatus): String = v.name
    @TypeConverter fun toLeaveStatus(v: String): LeaveStatus = LeaveStatus.valueOf(v)

    @TypeConverter fun fromAlertRuleType(v: AlertRuleType): String = v.name
    @TypeConverter fun toAlertRuleType(v: String): AlertRuleType = AlertRuleType.valueOf(v)

    @TypeConverter fun fromFundTransactionType(v: FundTransactionType): String = v.name
    @TypeConverter fun toFundTransactionType(v: String): FundTransactionType = FundTransactionType.valueOf(v)
}
