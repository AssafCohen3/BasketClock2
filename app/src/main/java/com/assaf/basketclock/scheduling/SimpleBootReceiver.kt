package com.assaf.basketclock.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SimpleBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            scheduleDailyAlarm(context)
        }
    }
}
