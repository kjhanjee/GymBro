package com.gymlogger.data

import com.gymlogger.getDataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object SettingsRepository {
    private val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    private val TIMER_UNIT = stringPreferencesKey("timer_unit")
    private val THEME_HUE = floatPreferencesKey("theme_hue")
    private val HEIGHT_CM = floatPreferencesKey("height_cm")
    private val WEIGHT_KG = floatPreferencesKey("weight_kg")
    private val GENDER = stringPreferencesKey("gender")
    private val TARGET_WEIGHT_KG = floatPreferencesKey("target_weight_kg")
    private val GOAL = stringPreferencesKey("goal")
    private val TRAINING_CONTEXT_KEY = stringPreferencesKey("weekly_schedule")
    private val CHAT_HISTORY_KEY = stringPreferencesKey("chat_history")

    private val _activeThemeHue = MutableStateFlow(210f)
    val activeThemeHue: StateFlow<Float> = _activeThemeHue.asStateFlow()

    fun updateActiveHue(hue: Float) {
        _activeThemeHue.value = hue
    }

    enum class WeightUnit { KG, LBS }
    enum class TimerUnit { MINUTES, SECONDS }

    suspend fun init() {
        val preferences = getDataStore().data.first()
        _activeThemeHue.value = preferences[THEME_HUE] ?: 210f
    }

    fun getWeightUnit(): Flow<WeightUnit> = getDataStore().data.map { preferences ->
        val unitStr = preferences[WEIGHT_UNIT] ?: WeightUnit.LBS.name
        WeightUnit.valueOf(unitStr)
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        getDataStore().edit { preferences ->
            preferences[WEIGHT_UNIT] = unit.name
        }
    }

    fun getTimerUnit(): Flow<TimerUnit> = getDataStore().data.map { preferences ->
        val unitStr = preferences[TIMER_UNIT] ?: TimerUnit.MINUTES.name
        TimerUnit.valueOf(unitStr)
    }

    suspend fun setTimerUnit(unit: TimerUnit) {
        getDataStore().edit { preferences ->
            preferences[TIMER_UNIT] = unit.name
        }
    }

    fun getThemeHue(): Flow<Float> = getDataStore().data.map { preferences ->
        preferences[THEME_HUE] ?: 210f
    }

    suspend fun setThemeHue(hue: Float) {
        getDataStore().edit { preferences ->
            preferences[THEME_HUE] = hue
        }
        _activeThemeHue.value = hue
    }

    // Physique & Goals
    fun getHeight(): Flow<Float> = getDataStore().data.map { it[HEIGHT_CM] ?: 170f }
    suspend fun setHeight(height: Float) { getDataStore().edit { it[HEIGHT_CM] = height } }

    fun getWeight(): Flow<Float> = getDataStore().data.map { it[WEIGHT_KG] ?: 70f }
    suspend fun setWeight(weight: Float) { getDataStore().edit { it[WEIGHT_KG] = weight } }

    fun getGender(): Flow<String> = getDataStore().data.map { it[GENDER] ?: "Male" }
    suspend fun setGender(gender: String) { getDataStore().edit { it[GENDER] = gender } }

    fun getTargetWeight(): Flow<Float> = getDataStore().data.map { it[TARGET_WEIGHT_KG] ?: 70f }
    suspend fun setTargetWeight(weight: Float) { getDataStore().edit { it[TARGET_WEIGHT_KG] = weight } }

    fun getGoal(): Flow<String> = getDataStore().data.map { it[GOAL] ?: "Body Recomposition" }
    suspend fun setGoal(goal: String) { getDataStore().edit { it[GOAL] = goal } }

    fun getTrainingContext(): Flow<List<String>> = getDataStore().data.map { 
        it[TRAINING_CONTEXT_KEY]?.split(",")?.filter { s -> s.isNotBlank() } ?: emptyList() 
    }
    suspend fun setTrainingContext(schedule: List<String>) {
        getDataStore().edit { it[TRAINING_CONTEXT_KEY] = schedule.joinToString(",") }
    }

    suspend fun getChatHistory(): String? {
        return getDataStore().data.map { it[CHAT_HISTORY_KEY] }.first()
    }

    suspend fun setChatHistory(history: String) {
        getDataStore().edit { preferences ->
            preferences[CHAT_HISTORY_KEY] = history
        }
    }
}
