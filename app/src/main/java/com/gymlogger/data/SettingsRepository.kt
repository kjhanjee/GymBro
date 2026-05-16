package com.gymlogger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

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

    suspend fun init(context: Context) {
        val preferences = context.dataStore.data.first()
        _activeThemeHue.value = preferences[THEME_HUE] ?: 210f
    }

    fun getWeightUnit(context: Context): Flow<WeightUnit> = context.dataStore.data.map { preferences ->
        val unitStr = preferences[WEIGHT_UNIT] ?: WeightUnit.LBS.name
        WeightUnit.valueOf(unitStr)
    }

    suspend fun setWeightUnit(context: Context, unit: WeightUnit) {
        context.dataStore.edit { preferences ->
            preferences[WEIGHT_UNIT] = unit.name
        }
    }

    fun getTimerUnit(context: Context): Flow<TimerUnit> = context.dataStore.data.map { preferences ->
        val unitStr = preferences[TIMER_UNIT] ?: TimerUnit.MINUTES.name
        TimerUnit.valueOf(unitStr)
    }

    suspend fun setTimerUnit(context: Context, unit: TimerUnit) {
        context.dataStore.edit { preferences ->
            preferences[TIMER_UNIT] = unit.name
        }
    }

    fun getThemeHue(context: Context): Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[THEME_HUE] ?: 210f
    }

    suspend fun setThemeHue(context: Context, hue: Float) {
        context.dataStore.edit { preferences ->
            preferences[THEME_HUE] = hue
        }
        _activeThemeHue.value = hue
    }

    // Physique & Goals
    fun getHeight(context: Context): Flow<Float> = context.dataStore.data.map { it[HEIGHT_CM] ?: 170f }
    suspend fun setHeight(context: Context, height: Float) { context.dataStore.edit { it[HEIGHT_CM] = height } }

    fun getWeight(context: Context): Flow<Float> = context.dataStore.data.map { it[WEIGHT_KG] ?: 70f }
    suspend fun setWeight(context: Context, weight: Float) { context.dataStore.edit { it[WEIGHT_KG] = weight } }

    fun getGender(context: Context): Flow<String> = context.dataStore.data.map { it[GENDER] ?: "Male" }
    suspend fun setGender(context: Context, gender: String) { context.dataStore.edit { it[GENDER] = gender } }

    fun getTargetWeight(context: Context): Flow<Float> = context.dataStore.data.map { it[TARGET_WEIGHT_KG] ?: 70f }
    suspend fun setTargetWeight(context: Context, weight: Float) { context.dataStore.edit { it[TARGET_WEIGHT_KG] = weight } }

    fun getGoal(context: Context): Flow<String> = context.dataStore.data.map { it[GOAL] ?: "Body Recomposition" }
    suspend fun setGoal(context: Context, goal: String) { context.dataStore.edit { it[GOAL] = goal } }

    fun getTrainingContext(context: Context): Flow<List<String>> = context.dataStore.data.map { 
        it[TRAINING_CONTEXT_KEY]?.split(",")?.filter { s -> s.isNotBlank() } ?: emptyList() 
    }
    suspend fun setTrainingContext(context: Context, schedule: List<String>) {
        context.dataStore.edit { it[TRAINING_CONTEXT_KEY] = schedule.joinToString(",") }
    }

    suspend fun getChatHistory(context: Context): String? {
        return context.dataStore.data.map { it[CHAT_HISTORY_KEY] }.first()
    }

    suspend fun setChatHistory(context: Context, history: String) {
        context.dataStore.edit { preferences ->
            preferences[CHAT_HISTORY_KEY] = history
        }
    }
}
