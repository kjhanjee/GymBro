package com.gymlogger.services

import kotlinx.coroutines.flow.StateFlow

expect object WorkoutServiceController {
    val secondsElapsed: StateFlow<Long>
    val restSecondsRemaining: StateFlow<Int>
    val isRestTimerActive: StateFlow<Boolean>
    val isActive: StateFlow<Boolean>
    val workoutTitle: StateFlow<String>
    val totalSets: StateFlow<Int>
    val completedSets: StateFlow<Int>

    fun startWorkoutService()
    fun stopWorkoutService()
    fun restoreWorkoutService()
    fun bindService()
    fun unbindService()
    
    fun updateWorkoutStats(title: String, totalSets: Int, completedSets: Int)
    fun startRestTimer(seconds: Int)
    fun stopRestTimer()
}
