package com.example.tracker

import android.app.Application

class TrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TrackerApplication
            private set
    }
}
