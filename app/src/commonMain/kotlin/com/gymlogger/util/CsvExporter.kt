package com.gymlogger.util

import com.gymlogger.model.Meal


object CsvExporter {

    fun exportMealsToCsv(meals: List<Meal>): String {
        val dateFormatPattern = "yyyy-MM-dd"
        val timeFormatPattern = "HH:mm"
        val csvBuilder = StringBuilder()
        
        // Header
        csvBuilder.append("Date,Time,Meal Type,Item Name,Weight\n")
        
        // Sort meals by date
        val sortedMeals = meals.sortedBy { it.date }
        
        for (meal in sortedMeals) {
            val dateStr = com.gymlogger.util.formatTimestamp(meal.date, dateFormatPattern)
            val timeStr = com.gymlogger.util.formatTimestamp(meal.date, timeFormatPattern)
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
