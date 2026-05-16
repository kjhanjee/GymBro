package com.gymlogger.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSet(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val type: SetType,
    val reps: Int?,
    val weight: Float?,
    val rir: Int? = null, // Reps In Reserve
    val inputType: InputType = InputType.REPS,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val restTime: Int? = null // New field
) {
    @Serializable
    enum class SetType(val label: String, val shortLabel: String) {
        NORMAL("Normal Set", "N"),
        WARMUP("Warmup Set", "W"),
        FAILURE("Failure Set", "F"),
        DROP_SET("Drop Set", "D")
    }

    @Serializable
    enum class InputType {
        REPS,
        TIME
    }
}
