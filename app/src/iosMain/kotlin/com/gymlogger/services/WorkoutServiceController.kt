package com.gymlogger.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    actual fun startWorkoutService() {
        println("WorkoutService stub started on iOS")
    }

    actual fun stopWorkoutService() {
        println("WorkoutService stub stopped on iOS")
    }

    actual fun restoreWorkoutService() {
        println("WorkoutService stub restored on iOS")
    }

    actual fun bindService() {
    }

    actual fun unbindService() {
    }

    actual fun updateWorkoutStats(title: String, totalSets: Int, completedSets: Int) {
    }

    actual fun startRestTimer(seconds: Int) {
    }

    actual fun stopRestTimer() {
    }
}
