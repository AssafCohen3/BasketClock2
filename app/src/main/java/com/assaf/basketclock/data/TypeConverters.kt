package com.assaf.basketclock.data

import androidx.room.TypeConverter
import com.assaf.basketclock.conditions.ConditionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ZonedDateTypeConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): ZonedDateTime? {
        return value?.let { Instant.ofEpochSecond(value).atZone(ZoneId.of("UTC")) }
    }

    @TypeConverter
    fun dateToTimestamp(date: ZonedDateTime?): Long? {
        return date?.toEpochSecond()
    }
}

class ConditionTypeConverter {

    @TypeConverter
    fun fromConditionType(value: ConditionType?): String? {
        return value?.name // Convert enum to String
    }

    @TypeConverter
    fun toConditionType(value: String?): ConditionType? {
        return value?.let { ConditionType.valueOf(it) } // Convert String back to enum
    }
}

class SessionGameStatusTypeConverter {

    @TypeConverter
    fun fromSessionGameStatus(value: SessionGameStatus?): String? {
        return value?.name // Convert enum to String
    }

    @TypeConverter
    fun toSessionGameStatus(value: String?): SessionGameStatus? {
        return value?.let { SessionGameStatus.valueOf(it) } // Convert String back to enum
    }
}

class SessionStatusTypeConverter {

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus?): String? {
        return value?.name // Convert enum to String
    }

    @TypeConverter
    fun toSessionStatus(value: String?): SessionStatus? {
        return value?.let { SessionStatus.valueOf(it) } // Convert String back to enum
    }
}

class MapTypeConverter {

    @TypeConverter
    fun fromMap(value: Map<String, Any>?): String? {
        return value?.let { Gson().toJson(value) }
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, Any>? {
        return value?.let {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            Gson().fromJson<Map<String, Any>>(value, type)
        }
    }
}
