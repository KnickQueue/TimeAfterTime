package com.example.kronosclock

import android.app.Application
import com.lyft.kronos.AndroidClockFactory
import com.lyft.kronos.KronosClock
import com.example.kronosclock.data.WatchDatabase

class KronosApp : Application() {
    lateinit var kronos: KronosClock
        private set
    val database: WatchDatabase by lazy { WatchDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        kronos = AndroidClockFactory.createKronosClock(
            this,
            ntpHosts = listOf("time.android.com")
        )
        kronos.syncInBackground()
    }
}
