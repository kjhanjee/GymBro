package com.gymlogger.data

import android.content.Context
import com.gymlogger.model.Routine
import com.gymlogger.model.WorkoutSet
import com.gymlogger.model.Exercise
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

object RoutineRepository {
    private val ROUTINES_KEY = stringPreferencesKey("routines_list")
    private val WORKOUTS_KEY = stringPreferencesKey("completed_workouts")

    private val defaultRoutines = listOf<Routine>(
        Routine(
            id = 1L,
            name = "Chest & Triceps",
            exercises = listOf(
                Routine.RoutineExercise(1, 1, "Bench Press (Barbell)", 0, listOf(
                    Routine.RoutineExercise.SetConfig(1, WorkoutSet.SetType.NORMAL, 10, 135f, 2, 2)
                )),
                Routine.RoutineExercise(2, 4, "Tricep Pushdown (Cable)", 1, listOf(
                    Routine.RoutineExercise.SetConfig(2, WorkoutSet.SetType.NORMAL, 12, 40f, 2, 1)
                ))
            )
        ),
        Routine(
            id = 2L,
            name = "Back & Biceps",
            exercises = listOf(
                Routine.RoutineExercise(3, 2, "Deadlift (Barbell)", 0, listOf(
                    Routine.RoutineExercise.SetConfig(3, WorkoutSet.SetType.NORMAL, 5, 225f, 2, 3)
                )),
                Routine.RoutineExercise(4, 3, "Pull Up", 1, listOf(
                    Routine.RoutineExercise.SetConfig(4, WorkoutSet.SetType.NORMAL, 10, 0f, 2, 2)
                ))
            )
        )
    )

    private val routines = mutableListOf<Routine>()

    private val _routinesFlow = MutableStateFlow<List<Routine>>(emptyList())
    val routinesFlow: StateFlow<List<Routine>> = _routinesFlow.asStateFlow()

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val completedWorkouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    suspend fun init(context: Context) {
        val preferences = context.dataStore.data.first()
        val routinesJson = preferences[ROUTINES_KEY]
        if (routinesJson != null) {
            try {
                val decoded = Json.decodeFromString<List<Routine>>(routinesJson)
                routines.clear()
                routines.addAll(decoded)
            } catch (e: Exception) {
                routines.clear()
                routines.addAll(defaultRoutines)
            }
        } else {
            routines.clear()
            routines.addAll(defaultRoutines)
        }
        _routinesFlow.value = routines.toList()

        val workoutsJson = preferences[WORKOUTS_KEY]
        if (workoutsJson != null) {
            try {
                val decoded = Json.decodeFromString<List<Workout>>(workoutsJson)
                _workouts.value = decoded
            } catch (e: Exception) {
                _workouts.value = emptyList()
            }
        }
    }

    private suspend fun saveRoutines(context: Context) {
        val json = Json.encodeToString(routines.toList())
        context.dataStore.edit { preferences ->
            preferences[ROUTINES_KEY] = json
        }
        _routinesFlow.value = routines.toList()
    }

    private suspend fun saveWorkouts(context: Context) {
        val json = Json.encodeToString(_workouts.value)
        context.dataStore.edit { preferences ->
            preferences[WORKOUTS_KEY] = json
        }
    }

    fun getRoutines(): Flow<List<Routine>> {
        return routinesFlow
    }

    suspend fun createRoutine(context: Context, routine: Routine): Long {
        val id = (routines.maxOfOrNull { it.id } ?: 0L) + 1L
        routines.add(Routine(id, routine.name, routine.exercises, routine.description))
        saveRoutines(context)
        return id
    }

    suspend fun deleteRoutine(context: Context, id: Long) {
        routines.removeAll { it.id == id }
        saveRoutines(context)
    }

    suspend fun getRoutineById(id: Long): Routine? {
        return routines.find { it.id == id }
    }

    suspend fun updateRoutine(context: Context, routine: Routine) {
        val index = routines.indexOfFirst { it.id == routine.id }
        if (index != -1) {
            routines[index] = routine
            saveRoutines(context)
        }
    }

    suspend fun startWorkout(routineId: Long): Workout {
        val routine = routines.find { it.id == routineId } ?: Routine(0, "Unknown", emptyList())
        val id = (_workouts.value.maxOfOrNull { it.id } ?: 0L) + 1L
        return Workout(
            id = id,
            routine = routine,
            date = System.currentTimeMillis(),
            sets = routine.exercises.flatMap { exercise ->
                exercise.sets.map { setConfig ->
                    WorkoutSet(
                        id = setConfig.id,
                        exerciseId = exercise.exerciseId,
                        exerciseName = exercise.exerciseName,
                        type = setConfig.type,
                        reps = setConfig.targetReps,
                        weight = setConfig.targetWeight,
                        rir = setConfig.targetRir,
                        notes = null,
                        isCompleted = false
                    )
                }
            },
            notes = null
        )
    }

    suspend fun completeWorkout(context: Context, workout: Workout) {
        _workouts.update { it + workout }
        saveWorkouts(context)
    }

    fun getCompletedWorkouts(): Flow<List<Workout>> {
        return completedWorkouts
    }

    suspend fun saveWorkout(context: Context, workout: Workout) {
        completeWorkout(context, workout)
    }

    fun getWorkoutsByDateRange(startDate: Long, endDate: Long): Flow<List<Workout>> {
        return flow {
            val start = startDate
            val end = endDate
            emit(_workouts.value.filter { it.date in start..end }.sortedByDescending { it.date })
        }
    }
}

@Serializable
data class Workout(
    val id: Long,
    val routine: Routine,
    val date: Long,
    val sets: List<WorkoutSet>,
    val notes: String? = null
)
