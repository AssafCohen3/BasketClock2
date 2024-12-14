package com.assaf.basketclock.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class SimpleServiceStarter : BroadcastReceiver() {
    companion object{
        const val BROADCAST_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Firing service...")
        fireAlarmService(context, intent.extras)
    }
}


fun isSessionAlarmSet(context: Context): Boolean {
    val intent = Intent(context, SimpleServiceStarter::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        SimpleServiceStarter.BROADCAST_ID,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    return pendingIntent != null
}
