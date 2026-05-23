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
        csvBuilder.append("Date,Time,Meal Type,Item Name,Weight\n")
        
        // Sort meals by date
        val sortedMeals = meals.sortedBy { it.date }
        
        for (meal in sortedMeals) {
            val date = Date(meal.date)
            val dateStr = dateFormatter.format(date)
            val timeStr = timeFormatter.format(date)
            val mealTypeStr = meal.type.name
            
            if (meal.items.isEmpty()) {
                csvBuilder.append(escapeCsvField(dateStr)).append(",")
                csvBuilder.append(escapeCsvField(timeStr)).append(",")
                csvBuilder.append(escapeCsvField(mealTypeStr)).append(",")
                csvBuilder.append(",") // Empty Item Name
                csvBuilder.append("\n") // Empty Weight
            } else {
                for (item in meal.items) {
                    csvBuilder.append(escapeCsvField(dateStr)).append(",")
                    csvBuilder.append(escapeCsvField(timeStr)).append(",")
                    csvBuilder.append(escapeCsvField(mealTypeStr)).append(",")
                    csvBuilder.append(escapeCsvField(item.name)).append(",")
                    csvBuilder.append(escapeCsvField(item.weight)).append("\n")
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
