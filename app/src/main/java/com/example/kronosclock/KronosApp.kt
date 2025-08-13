package com.example.kronosclock

import android.app.Application
import com.lyft.kronos.AndroidClockFactory
import com.lyft.kronos.KronosClock

class KronosApp : Application() {

    companion object {
        lateinit var kronosClock: KronosClock
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize Kronos with default settings
        kronosClock = AndroidClockFactory.createKronosClock(this)
        // Optionally start sync right away
        kronosClock.sync()
    }
}
