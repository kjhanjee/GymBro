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
    val notes: String? = null,
    val isCompleted: Boolean = false
) {
    @Serializable
    enum class SetType {
        NORMAL,
        WARMUP,
        FAILURE,
        DROP_SET
    }
}
