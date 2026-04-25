package com.gymlogger.data

import androidx.room.TypeConverter
import com.gymlogger.model.Exercise
import com.gymlogger.model.MuscleGroup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromMuscleGroupList(value: List<MuscleGroup>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toMuscleGroupList(value: String): List<MuscleGroup> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromEquipment(value: Exercise.Equipment): String {
        return value.name
    }

    @TypeConverter
    fun toEquipment(value: String): Exercise.Equipment {
        return Exercise.Equipment.valueOf(value)
    }

    @TypeConverter
    fun fromCategory(value: Exercise.ExerciseCategory): String {
        return value.name
    }

    @TypeConverter
    fun toCategory(value: String): Exercise.ExerciseCategory {
        return Exercise.ExerciseCategory.valueOf(value)
    }
}
