package com.gymlogger.model

import kotlinx.serialization.Serializable

@Serializable
data class Routine(
    val id: Long,
    val name: String,
    val exercises: List<RoutineExercise>,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    @Serializable
    data class RoutineExercise(
        val id: Long,
        val exerciseId: Long,
        val exerciseName: String,
        val order: Int,
        val sets: List<SetConfig>,
        val inputType: WorkoutSet.InputType = WorkoutSet.InputType.REPS
    ) {
        @Serializable
        data class SetConfig(
            val id: Long,
            val type: WorkoutSet.SetType,
            val targetReps: Int?,
            val targetWeight: Float?,
            val targetRir: Int? = null,
            val restTime: Int, // seconds
            val completedSets: List<WorkoutSet> = emptyList()
        )
    }
}
