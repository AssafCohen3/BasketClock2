package com.assaf.basketclock

import android.app.AlarmManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date


//convert a data class to a map
fun <T> T.serializeToMap(): Map<String, Any> {
    return convert()
}

//convert a map to a data class
inline fun <reified T> Map<String, Any>.toDataClass(): T {
    return convert()
}

//convert an object of type I to type O
inline fun <I, reified O> I.convert(): O {
    val json = Gson().toJson(this)
    return Gson().fromJson(json, object : TypeToken<O>() {}.type)
}

fun canScheduleExactAlarms(context: Context): Boolean{
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }
    return true
}

fun canPostNotifications(context: Context): Boolean{
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

fun getESTDate(date: ZonedDateTime): Date{
    return Date.from(date.withZoneSameInstant(java.time.ZoneId.of("EST")).toInstant())
}

fun systemSecondsToLocalDateTime(milliSeconds: Long): LocalDateTime{
    val localDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(milliSeconds / 1000),
        ZoneId.systemDefault()
    )
    return localDateTime
}
