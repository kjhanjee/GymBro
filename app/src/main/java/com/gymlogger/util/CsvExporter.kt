package com.gymlogger.util

import com.gymlogger.model.Meal
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun exportMealsToCsv(meals: List<Meal>): String {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val csvBuilder = StringBuilder()
        
        // Header
        csvBuilder.append("Date,Time,Meal Type,Item Name,Weight,Calories,Protein,Carbs,Fats,Fibre,Sugar,VitB,VitD,Omega,VitC,Iron,Potassium,Magnesium,Sodium\n")
        
        // Sort meals by date
        val sortedMeals = meals.sortedBy { it.date }
        
        for (meal in sortedMeals) {
            val date = Date(meal.date)
            val dateStr = dateFormatter.format(date)
            val timeStr = timeFormatter.format(date)
            val mealTypeStr = meal.type.name
            
            val m = meal.macros
            val macroSuffix = ",${m.calories},${m.protein},${m.carbs},${m.fats},${m.fibre},${m.refinedSugar},${m.vitaminB},${m.vitaminD},${m.omega},${m.vitaminC},${m.iron},${m.potassium},${m.magnesium},${m.sodium}"

            if (meal.items.isEmpty()) {
                csvBuilder.append(escapeCsvField(dateStr)).append(",")
                csvBuilder.append(escapeCsvField(timeStr)).append(",")
                csvBuilder.append(escapeCsvField(mealTypeStr)).append(",")
                csvBuilder.append(",") // Empty Item Name
                csvBuilder.append(",") // Empty Weight
                csvBuilder.append(macroSuffix)
                csvBuilder.append("\n")
            } else {
                for ((index, item) in meal.items.withIndex()) {
                    csvBuilder.append(escapeCsvField(dateStr)).append(",")
                    csvBuilder.append(escapeCsvField(timeStr)).append(",")
                    csvBuilder.append(escapeCsvField(mealTypeStr)).append(",")
                    csvBuilder.append(escapeCsvField(item.name)).append(",")
                    csvBuilder.append(escapeCsvField(item.weight)).append(",")
                    
                    // Only add macros for the first item of a meal to avoid double counting totals 
                    // if multiple items are listed separately in the CSV for one meal.
                    // Given the user request, usually one meal = many items, but macros are total for the meal.
                    if (index == 0) {
                        csvBuilder.append(macroSuffix)
                    } else {
                        csvBuilder.append(",,,,,,,,,,,,,,")
                    }
                    csvBuilder.append("\n")
                }
            }
        }
        
        return csvBuilder.toString()
    }

    private fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\""
        }
        return field
    }
}
