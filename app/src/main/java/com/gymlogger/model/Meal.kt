package com.gymlogger.model

import kotlinx.serialization.Serializable

@Serializable
enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK, PRE_WORKOUT
}

@Serializable
data class MealItem(
    val id: Long,
    val name: String,
    val weight: String
)

@Serializable
data class MealMacros(
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fats: Float = 0f
)

@Serializable
data class Meal(
    val id: Long,
    val date: Long,
    val type: MealType,
    val items: List<MealItem>,
    val macros: MealMacros = MealMacros()
)
