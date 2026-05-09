package com.gymlogger.data

import com.gymlogger.model.Exercise
import com.gymlogger.model.MuscleGroup
import kotlinx.coroutines.flow.Flow

interface IExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    suspend fun getExerciseById(id: Long): Exercise?
    fun getExercisesByIds(ids: List<Long>): Flow<List<Exercise>>
    fun searchExercises(query: String): Flow<List<Exercise>>
    fun filterByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>>
    suspend fun insertExercise(exercise: Exercise)
    suspend fun updateExercise(exercise: Exercise)
    suspend fun deleteExercise(exercise: Exercise)
}
