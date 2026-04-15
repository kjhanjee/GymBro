package com.gymlogger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FoodLabel(
    val itemName: String,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val servingSize: String = "100g"
) {
    val labelInfo: String
        get() = "$servingSize: ${calories}kcal, ${protein}g protein, ${carbs}g carbs, ${fats}g fats"
}

object FoodLabelRepository {
    private val FOOD_LABELS_KEY = stringPreferencesKey("food_labels")
    private val json = Json { ignoreUnknownKeys = true }

    private val _labels = MutableStateFlow<List<FoodLabel>>(emptyList())
    val labels: StateFlow<List<FoodLabel>> = _labels.asStateFlow()

    suspend fun init(context: Context) {
        val preferences = context.dataStore.data.first()
        val labelsJson = preferences[FOOD_LABELS_KEY]
        if (labelsJson != null) {
            try {
                _labels.value = json.decodeFromString<List<FoodLabel>>(labelsJson)
            } catch (e: Exception) {
                _labels.value = emptyList()
            }
        }
    }

    suspend fun saveLabel(
        context: Context,
        itemName: String,
        calories: Float,
        protein: Float,
        carbs: Float,
        fats: Float,
        servingSize: String
    ) {
        val current = _labels.value.toMutableList()
        val index = current.indexOfFirst { it.itemName.equals(itemName, ignoreCase = true) }
        val newLabel = FoodLabel(itemName, calories, protein, carbs, fats, servingSize)
        if (index != -1) {
            current[index] = newLabel
        } else {
            current.add(newLabel)
        }
        _labels.value = current
        
        context.dataStore.edit { preferences ->
            preferences[FOOD_LABELS_KEY] = json.encodeToString(current)
        }
    }

    suspend fun deleteLabel(context: Context, itemName: String) {
        val filtered = _labels.value.filterNot { it.itemName.equals(itemName, ignoreCase = true) }
        _labels.value = filtered
        context.dataStore.edit { preferences ->
            preferences[FOOD_LABELS_KEY] = json.encodeToString(filtered)
        }
    }
}
