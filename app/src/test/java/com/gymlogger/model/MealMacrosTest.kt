package com.gymlogger.model

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class MealMacrosTest {

    @Test
    fun testMealMacrosPlusOperator() {
        val macros1 = MealMacros(
            calories = 100f,
            protein = 10f,
            carbs = 20f,
            fats = 5f,
            fibre = 2f,
            refinedSugar = 1f,
            vitaminB = 0.1f,
            vitaminD = 1f,
            omega = 100f,
            vitaminC = 10f,
            iron = 1f,
            potassium = 200f,
            magnesium = 50f,
            sodium = 100f
        )
        val macros2 = MealMacros(
            calories = 50f,
            protein = 5f,
            carbs = 10f,
            fats = 2f,
            fibre = 1f,
            refinedSugar = 0.5f,
            vitaminB = 0.05f,
            vitaminD = 0.5f,
            omega = 50f,
            vitaminC = 5f,
            iron = 0.5f,
            potassium = 100f,
            magnesium = 25f,
            sodium = 50f
        )
        
        val result = macros1 + macros2
        
        assertEquals(150f, result.calories)
        assertEquals(15f, result.protein)
        assertEquals(30f, result.carbs)
        assertEquals(7f, result.fats)
        assertEquals(3f, result.fibre)
        assertEquals(1.5f, result.refinedSugar)
        assertEquals(0.15f, result.vitaminB, 0.001f)
        assertEquals(1.5f, result.vitaminD, 0.001f)
        assertEquals(150f, result.omega)
        assertEquals(15f, result.vitaminC)
        assertEquals(1.5f, result.iron)
        assertEquals(300f, result.potassium)
        assertEquals(75f, result.magnesium)
        assertEquals(150f, result.sodium)
    }

    @Test
    fun testMealMacrosSerialization() {
        val macros = MealMacros(
            vitaminC = 10f,
            iron = 1f,
            potassium = 200f,
            magnesium = 50f,
            sodium = 100f
        )
        
        val json = Json { ignoreUnknownKeys = true }
        val jsonString = json.encodeToString(macros)
        
        val decoded = json.decodeFromString<MealMacros>(jsonString)
        
        assertEquals(10f, decoded.vitaminC)
        assertEquals(1f, decoded.iron)
        assertEquals(200f, decoded.potassium)
        assertEquals(50f, decoded.magnesium)
        assertEquals(100f, decoded.sodium)
    }
}
