package com.gymlogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlogger.data.ExerciseRepository
import com.gymlogger.data.IExerciseRepository
import com.gymlogger.model.Exercise
import com.gymlogger.model.MuscleGroup
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExerciseViewModel(
    private val repository: IExerciseRepository = ExerciseRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(MuscleGroup.CHEST)
    val selectedMuscleGroup: StateFlow<MuscleGroup?> = _selectedMuscleGroup.asStateFlow()

    val filteredExercises: StateFlow<List<Exercise>> = combine(
        _searchQuery.debounce(300),
        _selectedMuscleGroup
    ) { query, muscleGroup ->
        Pair(query, muscleGroup)
    }.flatMapLatest { (query, muscleGroup) ->
        if (query.isBlank()) {
            if (muscleGroup != null) {
                repository.filterByMuscleGroup(muscleGroup)
            } else {
                repository.getAllExercises()
            }
        } else {
            repository.searchExercises(query).map { exercises ->
                if (muscleGroup != null) {
                    exercises.filter { it.muscleGroups.contains(muscleGroup) }
                } else {
                    exercises
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onMuscleGroupSelected(muscleGroup: MuscleGroup?) {
        _selectedMuscleGroup.value = muscleGroup
    }
}
