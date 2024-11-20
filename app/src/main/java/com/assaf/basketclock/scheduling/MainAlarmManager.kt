package com.assaf.basketclock.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.assaf.basketclock.AppDatabase
import com.assaf.basketclock.canScheduleExactAlarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.TimeZone

class MyAlarmReceiver : BroadcastReceiver() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (!canScheduleExactAlarms(context)){
            Timber.d("Skipped alarm receiver since alarm scheduling is not enabled.")
            return
        }

        coroutineScope.launch{
            val todayGames = AppDatabase.getDatabase(context).getConditionsRepository().getTodayConditions()
            Timber.d("Broadcast received!!")
            Timber.d("Games count: ${todayGames.size}")
            withContext(Dispatchers.Main){
                Toast.makeText(context, "Today's games count: ${todayGames.size}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


fun scheduleDailyAlarm(context: Context) {
    if (isAlarmSet(context)){
        return
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Create an intent for the receiver
    val intent = Intent(context, MyAlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1000,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )

    // Set the alarm time for 5 PM UTC.
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.HOUR_OF_DAY, 17) // 5 PM in UTC
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)

        // If 5 PM UTC has already passed for today, schedule for tomorrow
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
            // TODO kick in the intent also.
        }
    }

    // Schedule the alarm to repeat daily
    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}

fun isAlarmSet(context: Context): Boolean {
    val intent = Intent(context, MyAlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1000,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    return pendingIntent != null
}

fun fireReceiver(context: Context){
    val intent = Intent(context, MyAlarmReceiver::class.java)
    context.sendBroadcast(intent)
}
