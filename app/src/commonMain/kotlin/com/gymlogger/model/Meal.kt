package com.gymlogger.model

import kotlinx.serialization.SerialName
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
    val fats: Float = 0f,
    val fibre: Float = 0f,
    @SerialName("sugar")
    val refinedSugar: Float = 0f,
    val vitaminB: Float = 0f,
    val vitaminD: Float = 0f,
    val omega: Float = 0f,
    val vitaminC: Float = 0f,
    val iron: Float = 0f,
    val potassium: Float = 0f,
    val magnesium: Float = 0f,
    val sodium: Float = 0f
) {
    operator fun plus(other: MealMacros): MealMacros {
        return MealMacros(
            calories = this.calories + other.calories,
            protein = this.protein + other.protein,
            carbs = this.carbs + other.carbs,
            fats = this.fats + other.fats,
            fibre = this.fibre + other.fibre,
            refinedSugar = this.refinedSugar + other.refinedSugar,
            vitaminB = this.vitaminB + other.vitaminB,
            vitaminD = this.vitaminD + other.vitaminD,
            omega = this.omega + other.omega,
            vitaminC = this.vitaminC + other.vitaminC,
            iron = this.iron + other.iron,
            potassium = this.potassium + other.potassium,
            magnesium = this.magnesium + other.magnesium,
            sodium = this.sodium + other.sodium
        )
    }
}

@Serializable
data class Meal(
    val id: Long,
    val date: Long,
    val type: MealType,
    val items: List<MealItem>,
    val macros: MealMacros = MealMacros()
)
