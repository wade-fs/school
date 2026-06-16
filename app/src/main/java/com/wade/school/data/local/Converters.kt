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
}
