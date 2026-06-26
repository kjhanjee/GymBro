package com.gymlogger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymlogger.model.Meal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MealRepository {
    private val MEALS_KEY = stringPreferencesKey("meals_list")
    
    // Reuse Json instance for efficiency
    private val json = Json { ignoreUnknownKeys = true }

    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    suspend fun init(context: Context) {
        val preferences = context.dataStore.data.first()
        val mealsJson = preferences[MEALS_KEY]
        if (mealsJson != null) {
            try {
                val decoded = json.decodeFromString<List<Meal>>(mealsJson)
                _meals.value = decoded
            } catch (e: Exception) {
                _meals.value = emptyList()
            }
        }
    }

    private suspend fun saveMeals(context: Context) {
        val jsonString = json.encodeToString(_meals.value)
        context.dataStore.edit { preferences ->
            preferences[MEALS_KEY] = jsonString
        }
    }

    suspend fun addMeal(context: Context, meal: Meal) {
        val id = (_meals.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val newMeal = meal.copy(id = id)
        _meals.value = _meals.value + newMeal
        saveMeals(context)
    }

    suspend fun updateMeal(context: Context, meal: Meal) {
        _meals.value = _meals.value.map { if (it.id == meal.id) meal else it }
        saveMeals(context)
    }

    suspend fun deleteMeal(context: Context, id: Long) {
        _meals.value = _meals.value.filter { it.id != id }
        saveMeals(context)
    }
}
