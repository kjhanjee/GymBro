package com.gymlogger.data

import androidx.room.*
import com.gymlogger.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    fun getExercisesByIds(ids: List<Long>): Flow<List<Exercise>>

    @Query("""
        SELECT * FROM exercises 
        WHERE (:query IS NULL OR name LIKE '%' || :query || '%')
        AND (:muscleGroup IS NULL OR muscleGroups LIKE '%' || :muscleGroup || '%')
        AND (:equipment IS NULL OR equipment = :equipment)
        AND (:category IS NULL OR category = :category)
        ORDER BY name ASC
    """)
    fun getFilteredExercises(
        query: String? = null,
        muscleGroup: String? = null,
        equipment: Exercise.Equipment? = null,
        category: Exercise.ExerciseCategory? = null
    ): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Update
    suspend fun updateExercise(exercise: Exercise): Int

    @Delete
    suspend fun deleteExercise(exercise: Exercise): Int
}
