package com.gymlogger.util

import com.gymlogger.model.Routine

expect object ShareManager {
    fun shareWorkout(htmlContent: String)
    fun shareRoutine(routine: Routine)
}
