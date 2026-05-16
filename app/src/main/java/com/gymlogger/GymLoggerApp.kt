package com.gymlogger

import android.app.Application
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.gymlogger.ai.MacroCalculator

class GymLoggerApp : Application() {

    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                Log.d("GymLoggerApp", "App backgrounded - starting release timer")
                MacroCalculator.scheduleRelease(applicationContext)
            }
            Lifecycle.Event.ON_START -> {
                Log.d("GymLoggerApp", "App foregrounded - cancelling release timer")
                MacroCalculator.cancelRelease()
            }
            else -> {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("GymLoggerApp", "onCreate")
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
    }
}
