package com.gymlogger

import android.app.Application
import android.util.Log

class GymLoggerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("GymLoggerApp", "onCreate")
    }
}
