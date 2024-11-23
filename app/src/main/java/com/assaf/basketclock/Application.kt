package com.assaf.basketclock

import android.app.Application
import com.assaf.basketclock.scheduling.scheduleDailyAlarm
import timber.log.Timber

class Application: Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(FileLoggingTree(this))
        Timber.d("Scheduling daily alarm from application...")
        scheduleDailyAlarm(this)
    }
}