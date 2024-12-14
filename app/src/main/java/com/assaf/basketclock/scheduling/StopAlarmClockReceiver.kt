package com.assaf.basketclock.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopAlarmClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Stop the alarm service
        val serviceIntent = Intent(context, AlarmClockService::class.java)
        context.stopService(serviceIntent)
    }
}
