package com.example.kronosanalogclock

import android.app.Application
import com.lyft.kronos.AndroidClockFactory
import com.lyft.kronos.KronosClock

class KronosApp : Application() {
    lateinit var kronos: KronosClock
        private set

    override fun onCreate() {
        super.onCreate()
        kronos = AndroidClockFactory.createKronosClock(
            this,
            ntpHosts = listOf("time.android.com")
        )
        kronos.syncInBackground()
    }
}
