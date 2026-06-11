package com.wade.teacher.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromByteArray(value: ByteArray): String =
        android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP)

    @TypeConverter
    fun toByteArray(value: String): ByteArray =
        android.util.Base64.decode(value, android.util.Base64.NO_WRAP)
}
