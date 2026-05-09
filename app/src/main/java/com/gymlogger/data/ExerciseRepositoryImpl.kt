package com.gymlogger.data

import com.gymlogger.model.Exercise
import com.gymlogger.model.MuscleGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ExerciseRepositoryImpl(
    private val exerciseDao: ExerciseDao
) : IExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()

    override suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)

    override fun getExercisesByIds(ids: List<Long>): Flow<List<Exercise>> =
        exerciseDao.getExercisesByIds(ids)

    override fun searchExercises(query: String): Flow<List<Exercise>> =
        exerciseDao.getFilteredExercises(query = query)

    override fun filterByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> =
        exerciseDao.getFilteredExercises(muscleGroup = muscleGroup.name)

    override suspend fun insertExercise(exercise: Exercise) = exerciseDao.insertExercise(exercise)

    override suspend fun updateExercise(exercise: Exercise) = exerciseDao.updateExercise(exercise)

    override suspend fun deleteExercise(exercise: Exercise) = exerciseDao.deleteExercise(exercise)
}
