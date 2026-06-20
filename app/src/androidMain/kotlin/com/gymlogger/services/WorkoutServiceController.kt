package com.gymlogger.services

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.gymlogger.AndroidPlatform
import com.gymlogger.service.WorkoutService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

actual object WorkoutServiceController {
    private val _secondsElapsed = MutableStateFlow(0L)
    actual val secondsElapsed: StateFlow<Long> = _secondsElapsed.asStateFlow()

    private val _restSecondsRemaining = MutableStateFlow(0)
    actual val restSecondsRemaining: StateFlow<Int> = _restSecondsRemaining.asStateFlow()

    private val _isRestTimerActive = MutableStateFlow(false)
    actual val isRestTimerActive: StateFlow<Boolean> = _isRestTimerActive.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    actual val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _workoutTitle = MutableStateFlow("Tracked Workout")
    actual val workoutTitle: StateFlow<String> = _workoutTitle.asStateFlow()

    private val _totalSets = MutableStateFlow(0)
    actual val totalSets: StateFlow<Int> = _totalSets.asStateFlow()

    private val _completedSets = MutableStateFlow(0)
    actual val completedSets: StateFlow<Int> = _completedSets.asStateFlow()

    private var workoutService: WorkoutService? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WorkoutService.WorkoutBinder
            workoutService = binder.getService()
            job?.cancel()
            job = scope.launch {
                launch {
                    workoutService?.secondsElapsed?.collect { _secondsElapsed.value = it }
                }
                launch {
                    workoutService?.restSecondsRemaining?.collect { _restSecondsRemaining.value = it }
                }
                launch {
                    workoutService?.isRestTimerActive?.collect { _isRestTimerActive.value = it }
                }
                launch {
                    workoutService?.isActive?.collect { _isActive.value = it }
                }
                launch {
                    workoutService?.workoutTitle?.collect { _workoutTitle.value = it }
                }
                launch {
                    workoutService?.totalSets?.collect { _totalSets.value = it }
                }
                launch {
                    workoutService?.completedSets?.collect { _completedSets.value = it }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            workoutService = null
            job?.cancel()
        }
    }

    actual fun startWorkoutService() {
        val context = AndroidPlatform.context
        val intent = Intent(context, WorkoutService::class.java).apply {
            action = WorkoutService.ACTION_START_WORKOUT
        }
        context.startService(intent)
    }

    actual fun stopWorkoutService() {
        val context = AndroidPlatform.context
        val stopIntent = Intent(context, WorkoutService::class.java).apply {
            action = WorkoutService.ACTION_STOP_WORKOUT
        }
        context.startService(stopIntent)
    }

    actual fun restoreWorkoutService() {
        val context = AndroidPlatform.context
        val intent = Intent(context, WorkoutService::class.java).apply {
            action = WorkoutService.ACTION_RESTORE_WORKOUT
        }
        context.startService(intent)
    }

    actual fun bindService() {
        val context = AndroidPlatform.context
        val intent = Intent(context, WorkoutService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    actual fun unbindService() {
        val context = AndroidPlatform.context
        try {
            context.unbindService(connection)
        } catch (e: Exception) {
            // Already unbound
        }
        job?.cancel()
        workoutService = null
    }

    actual fun updateWorkoutStats(title: String, totalSets: Int, completedSets: Int) {
        workoutService?.updateWorkoutStats(title, totalSets, completedSets)
    }

    actual fun startRestTimer(seconds: Int) {
        workoutService?.startRestTimer(seconds)
    }

    actual fun stopRestTimer() {
        workoutService?.stopRestTimer()
    }
}
