package com.gymlogger.util

import java.util.Locale
import com.gymlogger.data.SettingsRepository

object UnitConverter {
    fun formatWeight(kgValue: Float?, unit: SettingsRepository.WeightUnit): String {
        if (kgValue == null || kgValue == 0f) return ""
        val value = if (unit == SettingsRepository.WeightUnit.LBS) {
            kgValue * 2.20462f
        } else {
            kgValue
        }
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
    }

    fun formatTimer(seconds: Int, unit: SettingsRepository.TimerUnit): String {
        return if (unit == SettingsRepository.TimerUnit.SECONDS) {
            seconds.toString()
        } else {
            val minutes = seconds / 60.0
            if (minutes % 1.0 == 0.0) {
                minutes.toInt().toString()
            } else {
                String.format(Locale.US, "%.1f", minutes)
            }
        }
    }

    fun weightToBase(weightText: String, unit: SettingsRepository.WeightUnit): Float? {
        val value = weightText.toFloatOrNull() ?: return null
        return if (unit == SettingsRepository.WeightUnit.LBS) {
            value / 2.20462f
        } else {
            value
        }
    }

    fun timerToBase(timerText: String, unit: SettingsRepository.TimerUnit): Int {
        return timerToSeconds(timerText, unit)
    }

    fun timerToSeconds(timerText: String, unit: SettingsRepository.TimerUnit): Int {
        val value = timerText.toFloatOrNull() ?: 0f
        return if (unit == SettingsRepository.TimerUnit.SECONDS) {
            value.toInt()
        } else {
            (value * 60).toInt()
        }
    }
}
