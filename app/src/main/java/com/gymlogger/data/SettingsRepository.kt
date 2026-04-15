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
}
