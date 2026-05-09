package com.gymlogger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymlogger.model.Meal
import com.gymlogger.model.MealMacros
import com.gymlogger.ai.MacroCalculator
import com.gymlogger.data.FoodLabelRepository
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
        val calculatedMacros = calculateMacrosForMeal(meal)
        val id = (_meals.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val newMeal = meal.copy(id = id, macros = calculatedMacros)
        _meals.value = _meals.value + newMeal
        saveMeals(context)
    }

    suspend fun updateMeal(context: Context, meal: Meal) {
        val calculatedMacros = calculateMacrosForMeal(meal)
        val updatedMeal = meal.copy(macros = calculatedMacros)
        _meals.value = _meals.value.map { if (it.id == meal.id) updatedMeal else it }
        saveMeals(context)
    }

    private suspend fun calculateMacrosForMeal(meal: Meal): MealMacros {
        // Fetch all saved labels
        val savedLabels = FoodLabelRepository.labels.value
        
        // Create a separate block for labels information
        val labelsInfo = if (savedLabels.isNotEmpty()) {
            savedLabels.joinToString("\n") { "- ${it.itemName}: ${it.labelInfo}" }
        } else {
            "No specific label information provided."
        }

        // Create a string representation of all items for the AI
        val mealDescription = meal.items.joinToString(", ") { "${it.weight} ${it.name}" }
        
        android.util.Log.d("MealRepository", "Requesting macros for: $mealDescription with labels: $labelsInfo")
        val jsonResponse = MacroCalculator.calculateMacros(mealDescription, labelsInfo)
        android.util.Log.d("MealRepository", "AI Response: $jsonResponse")
        
        return try {
            if (jsonResponse != null) {
                // Strip potential markdown format and common noise
                val cleanJson = jsonResponse.trim()
                    .removeSurrounding("```json", "```")
                    .removeSurrounding("```")
                    .trim()
                
                android.util.Log.d("MealRepository", "Cleaned JSON: $cleanJson")
                
                // Fallback: If Gemma adds extra conversational text, try to extract the JSON object
                val jsonObject = if (cleanJson.contains("{") && cleanJson.contains("}")) {
                    cleanJson.substring(cleanJson.indexOf("{"), cleanJson.lastIndexOf("}") + 1)
                } else {
                    cleanJson
                }
                
                android.util.Log.d("MealRepository", "Final JSON for parsing: $jsonObject")
                json.decodeFromString<MealMacros>(jsonObject)
            } else {
                android.util.Log.w("MealRepository", "AI returned null response")
                MealMacros(0f, 0f, 0f, 0f)
            }
        } catch (e: Exception) {
            android.util.Log.e("MealRepository", "Failed to parse macros JSON", e)
            MealMacros(0f, 0f, 0f, 0f)
        }
    }

    suspend fun deleteMeal(context: Context, id: Long) {
        _meals.value = _meals.value.filter { it.id != id }
        saveMeals(context)
    }
}
