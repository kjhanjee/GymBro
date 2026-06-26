package com.gymlogger.model

import kotlinx.serialization.Serializable

@Serializable
data class MealJson(
    val type: String, // e.g. "BREAKFAST", "LUNCH", "DINNER", "SNACK", "PRE_WORKOUT"
    val items: List<MealItemJson>
)

@Serializable
data class MealItemJson(
    val name: String,
    val calories: Float = 0f,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fats: Float = 0f,
    val fibre: Float = 0f,
    val sugar: Float = 0f,
    val vitaminB: Float = 0f,
    val vitaminD: Float = 0f,
    val omega: Float = 0f,
    val vitaminC: Float = 0f,
    val iron: Float = 0f,
    val potassium: Float = 0f,
    val magnesium: Float = 0f,
    val sodium: Float = 0f
)
