package com.gymlogger.util

import java.util.Locale
import com.gymlogger.data.SettingsRepository

object UnitConverter {
    fun formatWeight(weight: Float?, unit: SettingsRepository.WeightUnit): String {
        if (weight == null || weight == 0f) return ""
        val value = if (unit == SettingsRepository.WeightUnit.KG) {
            weight * 0.453592f
        } else {
            weight
        }
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    fun formatTimer(minutes: Int, unit: SettingsRepository.TimerUnit): String {
        return if (unit == SettingsRepository.TimerUnit.SECONDS) {
            (minutes * 60).toString()
        } else {
            minutes.toString()
        }
    }

    fun weightToBase(weightText: String, unit: SettingsRepository.WeightUnit): Float? {
        val value = weightText.toFloatOrNull() ?: return null
        return if (unit == SettingsRepository.WeightUnit.KG) {
            value / 0.453592f
        } else {
            value
        }
    }

    fun timerToBase(timerText: String, unit: SettingsRepository.TimerUnit): Int {
        val value = timerText.toIntOrNull() ?: 0
        return if (unit == SettingsRepository.TimerUnit.SECONDS) {
            // If unit is seconds, convert to minutes for base storage (Float for precision if needed, but here it's Int)
            // But if we want to support seconds in base, we might need to change the model.
            // For now, let's assume base is seconds if we want to be precise, or just round.
            // Looking at Routine.kt, restTime is Int.
            value / 60
        } else {
            value
        }
    }

    fun timerToSeconds(timerText: String, unit: SettingsRepository.TimerUnit): Int {
        val value = timerText.toIntOrNull() ?: 0
        return if (unit == SettingsRepository.TimerUnit.SECONDS) {
            value
        } else {
            value * 60
        }
    }
}
