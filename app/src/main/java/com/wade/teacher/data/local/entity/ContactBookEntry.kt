package com.wade.teacher.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = Gson().toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = 
        Gson().fromJson(value, object : TypeToken<List<String>>() {}.type)
}

@Entity(tableName = "contact_book")
@TypeConverters(Converters::class)
data class ContactBookEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classId: String,
    val date: Long, // 當日日期
    val content: String, // 聯絡事項/作業叮嚀
    val signedByParents: List<String> = emptyList() // 記錄已簽閱的學生ID
)
