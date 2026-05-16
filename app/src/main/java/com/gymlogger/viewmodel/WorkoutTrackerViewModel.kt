package com.gymlogger.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.data.InProgressExerciseState
import com.gymlogger.data.InProgressSetState
import com.gymlogger.data.InProgressWorkout
import com.gymlogger.data.RoutineRepository
import com.gymlogger.model.WorkoutSet
import com.gymlogger.ui.screens.ExerciseState
import com.gymlogger.ui.screens.SetState
import com.gymlogger.util.UnitConverter
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class WorkoutTrackerViewModel : ViewModel() {

    private val _exercises = mutableStateListOf<ExerciseState>()
    val exercises: SnapshotStateList<ExerciseState> = _exercises

    private val _workoutTitle = MutableStateFlow("Log Workout")
    val workoutTitle: StateFlow<String> = _workoutTitle.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var currentRoutineId: Long? = null

    fun setWorkoutTitle(title: String) {
        _workoutTitle.value = title
    }

    fun init(context: Context, routineId: Long?) {
        if (_isInitialized.value) return

        viewModelScope.launch {
            // Priority 1: Check if there's already an in-progress workout in storage
            val inProgress = RoutineRepository.getInProgressWorkout(context)
            
            if (inProgress != null && (inProgress.startTimeMillis != null || inProgress.exerciseStates.isNotEmpty())) {
                // If a specific routineId was requested, check if it matches the in-progress one
                if (routineId != null && routineId != -1L && inProgress.routineId != routineId) {
                    // Conflict: User wants a different routine than the one in progress.
                    // Start fresh with the new routine.
                    loadFromRoutine(routineId)
                } else {
                    // Either no routineId requested (Start Workout button), or it matches.
                    restoreFromInProgress(inProgress)
                }
            } else if (routineId != null && routineId != -1L) {
                // Priority 2: Load from routine if provided and nothing in progress
                loadFromRoutine(routineId)
            }
            
            _isInitialized.value = true
            
            // Start aggressive persistence after initialization
            startAggressivePersistence(context)
        }
    }

    private suspend fun restoreFromInProgress(inProgress: InProgressWorkout) {
        currentRoutineId = inProgress.routineId
        _workoutTitle.value = inProgress.workoutTitle
        _exercises.clear()
        inProgress.exerciseStates.forEach { inProgressEx ->
            val exercise = ExerciseRepository.getExerciseById(inProgressEx.exerciseId)
            if (exercise != null) {
                val restTime = inProgressEx.restTime.coerceIn(0, 3600)
                val exerciseState = ExerciseState(
                    exercise = exercise,
                    restTime = restTime,
                    sets = mutableStateListOf(),
                    inputType = if (inProgressEx.inputType == "REPS") WorkoutSet.InputType.REPS else WorkoutSet.InputType.TIME,
                    id = inProgressEx.exerciseId
                )
                inProgressEx.sets.forEach { inProgressSet ->
                    exerciseState.sets.add(SetState(
                        baseWeight = inProgressSet.weight.toFloatOrNull(),
                        reps = inProgressSet.reps,
                        rir = inProgressSet.rir,
                        type = WorkoutSet.SetType.valueOf(inProgressSet.type),
                        isCompleted = inProgressSet.isCompleted
                    ))
                }
                _exercises.add(exerciseState)
            }
        }
    }

    private suspend fun loadFromRoutine(routineId: Long) {
        val routine = RoutineRepository.getRoutineById(routineId)
        if (routine != null) {
            currentRoutineId = routineId
            _workoutTitle.value = routine.name
            routine.exercises.forEach { routineEx ->
                val exercise = ExerciseRepository.getExerciseById(routineEx.exerciseId)
                if (exercise != null) {
                    val firstSet = routineEx.sets.firstOrNull()
                    val restTime = (firstSet?.restTime ?: 0).coerceIn(0, 3600)
                    val exerciseState = ExerciseState(
                        exercise = exercise,
                        restTime = restTime,
                        sets = mutableStateListOf(),
                        inputType = routineEx.inputType,
                        id = routineEx.id
                    )
                    routineEx.sets.forEach { setConfig ->
                        exerciseState.sets.add(
                            SetState(
                                baseWeight = setConfig.targetWeight,
                                reps = if (setConfig.targetReps == null || setConfig.targetReps == 0) "" else setConfig.targetReps.toString(),
                                rir = if (setConfig.targetRir == null) "" else setConfig.targetRir.toString(),
                                type = setConfig.type
                            )
                        )
                    }
                    _exercises.add(exerciseState)
                }
            }
        }
    }

    private fun startAggressivePersistence(context: Context) {
        viewModelScope.launch {
            // Monitor changes to exercises and title
            snapshotFlow { 
                Triple(
                    _workoutTitle.value,
                    _exercises.map { ex ->
                        Triple(
                            ex.id,
                            ex.restTime,
                            ex.sets.map { s -> 
                                Quintuple(s.baseWeight?.toString() ?: "", s.reps, s.rir, s.type, s.isCompleted) 
                            }
                        )
                    },
                    _exercises.size
                )
            }
            .debounce(1000) // Save at most once per second
            .distinctUntilChanged()
            .collectLatest { (title, exerciseList, _) ->
                val inProgressWorkout = InProgressWorkout(
                    routineId = currentRoutineId,
                    workoutTitle = title,
                    exerciseStates = _exercises.map { ex ->
                        InProgressExerciseState(
                            exerciseId = ex.exercise.id,
                            exerciseName = ex.exercise.name,
                            restTime = ex.restTime,
                            inputType = ex.inputType.name,
                            sets = ex.sets.map { s ->
                                InProgressSetState(
                                    weight = s.baseWeight?.toString() ?: "",
                                    reps = s.reps,
                                    rir = s.rir,
                                    type = s.type.name,
                                    isCompleted = s.isCompleted
                                )
                            }
                        )
                    }
                )
                
                RoutineRepository.updateInProgressWorkout(context) { current ->
                    inProgressWorkout.copy(
                        startTimeMillis = current.startTimeMillis,
                        restEndTimeMillis = current.restEndTimeMillis,
                        secondsElapsed = current.secondsElapsed
                    )
                }
            }
        }
    }
}

data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
