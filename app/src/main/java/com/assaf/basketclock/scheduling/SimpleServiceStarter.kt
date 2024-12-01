package com.assaf.basketclock.scheduling

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
